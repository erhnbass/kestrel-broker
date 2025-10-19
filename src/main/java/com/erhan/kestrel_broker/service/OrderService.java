package com.erhan.kestrel_broker.service;

import com.erhan.kestrel_broker.domain.enums.OrderSide;
import com.erhan.kestrel_broker.domain.enums.OrderStatus;
import com.erhan.kestrel_broker.dto.request.CreateOrderRequest;
import com.erhan.kestrel_broker.dto.response.MatchOrdersResponse;
import com.erhan.kestrel_broker.dto.response.OrderResponse;
import com.erhan.kestrel_broker.exception.BusinessException;
import com.erhan.kestrel_broker.exception.NotFoundException;
import com.erhan.kestrel_broker.service.processor.OrderProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.erhan.kestrel_broker.domain.entity.Asset;
import com.erhan.kestrel_broker.domain.entity.Order;
import com.erhan.kestrel_broker.repository.AssetRepository;
import com.erhan.kestrel_broker.repository.OrderRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final AssetRepository assetRepository;
    private final OrderRepository orderRepository;
    private final Map<String, OrderProcessor> processors;

    @Transactional
    public OrderResponse create(CreateOrderRequest req) {
        processors.get(req.side().name()).reserve(req);
        Order order = new Order();
        order.setCustomerId(req.customerId());
        order.setAssetName(req.assetName());
        order.setSide(req.side());
        order.setSize(req.size());
        order.setPrice(req.price());
        order.setStatus(OrderStatus.PENDING);
        order.setCreateDate(Instant.now());
        orderRepository.save(order);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(Long orderId, String requester, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!isAdmin && !order.getCustomerId().equals(requester)) {
            throw new BusinessException("You cannot cancel another user's order");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Only PENDING orders can be canceled");
        }

        if (order.getSide() == OrderSide.BUY) {
            long refund = Math.multiplyExact(order.getSize(), order.getPrice());
            Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(order.getCustomerId(), "TRY")
                    .orElseThrow(() -> new BusinessException("TRY asset not found"));

            tryAsset.setUsableSize(tryAsset.getUsableSize() + refund);
            assetRepository.save(tryAsset);
        } else {
            Asset asset = assetRepository.findByCustomerIdAndAssetName(order.getCustomerId(), order.getAssetName())
                    .orElseThrow(() -> new BusinessException("Asset not found"));

            asset.setUsableSize(asset.getUsableSize() + order.getSize());
            assetRepository.save(asset);
        }

        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);

        return toResponse(order);
    }

    @Transactional
    public Order match(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Only PENDING orders can be matched");
        }

        OrderProcessor processor = processors.get(order.getSide().name());
        if (processor == null) {
            throw new BusinessException("No processor found for side: " + order.getSide());
        }

        processor.match(order);

        order.setStatus(OrderStatus.MATCHED);
        orderRepository.save(order);

        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(String customerId, OrderStatus status, Instant from, Instant to) {
        Specification<Order> spec = Specification.allOf();

        if (customerId != null && !customerId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("customerId"), customerId));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThan(root.get("createDate"), to));
        }

        return orderRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createDate"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MatchOrdersResponse matchOrders(Long buyOrderId, Long sellOrderId) {
        Order buyOrder = orderRepository.findById(buyOrderId)
                .orElseThrow(() -> new NotFoundException("Buy order not found"));
        Order sellOrder = orderRepository.findById(sellOrderId)
                .orElseThrow(() -> new NotFoundException("Sell order not found"));

        if (buyOrder.getStatus() != OrderStatus.PENDING || sellOrder.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Only PENDING orders can be matched");
        }

        if (buyOrder.getSide() != OrderSide.BUY || sellOrder.getSide() != OrderSide.SELL) {
            throw new BusinessException("Invalid sides for matching");
        }

        if (buyOrder.getPrice() != sellOrder.getPrice()) {
            throw new BusinessException("Price mismatch between buy and sell orders");
        }

        if (buyOrder.getSize() != sellOrder.getSize()) {
            throw new BusinessException("Order sizes must be equal for match");
        }

        if (!buyOrder.getAssetName().equals(sellOrder.getAssetName())) {
            throw new BusinessException("Asset names must match for BUY and SELL orders");
        }

        Asset buyerAsset = assetRepository.findByCustomerIdAndAssetName(buyOrder.getCustomerId(), sellOrder.getAssetName())
                .orElseThrow(() -> new BusinessException("Buyer asset not found"));
        buyerAsset.setUsableSize(buyerAsset.getUsableSize() + buyOrder.getSize());
        assetRepository.save(buyerAsset);

        Asset sellerAsset = assetRepository.findByCustomerIdAndAssetName(sellOrder.getCustomerId(), "TRY")
                .orElseThrow(() -> new BusinessException("Seller TRY asset not found"));
        long payment = sellOrder.getPrice() * sellOrder.getSize();
        sellerAsset.setUsableSize(sellerAsset.getUsableSize() + payment);
        assetRepository.save(sellerAsset);

        buyOrder.setStatus(OrderStatus.MATCHED);
        sellOrder.setStatus(OrderStatus.MATCHED);
        orderRepository.save(buyOrder);
        orderRepository.save(sellOrder);

        return new MatchOrdersResponse(
                toResponse(buyOrder),
                toResponse(sellOrder),
                "Orders matched successfully"
        );
    }


    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getAssetName(),
                order.getSide(),
                order.getSize(),
                order.getPrice(),
                order.getStatus(),
                order.getCreateDate()
        );
    }
}
