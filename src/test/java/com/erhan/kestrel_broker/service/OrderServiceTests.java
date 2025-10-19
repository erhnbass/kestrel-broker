package com.erhan.kestrel_broker.service;

import com.erhan.kestrel_broker.domain.entity.Asset;
import com.erhan.kestrel_broker.domain.entity.Order;
import com.erhan.kestrel_broker.domain.enums.OrderSide;
import com.erhan.kestrel_broker.domain.enums.OrderStatus;
import com.erhan.kestrel_broker.dto.request.CreateOrderRequest;
import com.erhan.kestrel_broker.dto.response.OrderResponse;
import com.erhan.kestrel_broker.exception.BusinessException;
import com.erhan.kestrel_broker.exception.NotFoundException;
import com.erhan.kestrel_broker.repository.AssetRepository;
import com.erhan.kestrel_broker.repository.OrderRepository;
import com.erhan.kestrel_broker.service.processor.OrderProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTests {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private Map<String, OrderProcessor> processors;

    @Mock
    private OrderProcessor buyProcessor;

    @Mock
    private OrderProcessor sellProcessor;

    @InjectMocks
    private OrderService orderService;

    private Asset tryAsset;
    private Asset aaplAsset;

    @BeforeEach
    void setup() {
        tryAsset = new Asset();
        tryAsset.setCustomerId("C1");
        tryAsset.setAssetName("TRY");
        tryAsset.setSize(1_000_000);
        tryAsset.setUsableSize(1_000_000);

        aaplAsset = new Asset();
        aaplAsset.setCustomerId("C1");
        aaplAsset.setAssetName("AAPL");
        aaplAsset.setSize(100);
        aaplAsset.setUsableSize(100);
    }

    // --- CREATE TESTS ---

    @Test
    void shouldReserveTRYWhenCreatingBuyOrder() {
        CreateOrderRequest req = new CreateOrderRequest("C1", "AAPL", OrderSide.BUY, 10, 100);
        when(assetRepository.findByCustomerIdAndAssetName("C1", "TRY")).thenReturn(Optional.of(tryAsset));
        when(processors.get("BUY")).thenReturn(buyProcessor);

        OrderResponse resp = orderService.create(req);

        verify(buyProcessor).reserve(req);
        assertEquals(OrderStatus.PENDING, resp.status());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldReserveAssetWhenCreatingSellOrder() {
        CreateOrderRequest req = new CreateOrderRequest("C1", "AAPL", OrderSide.SELL, 10, 100);
        when(assetRepository.findByCustomerIdAndAssetName("C1", "AAPL")).thenReturn(Optional.of(aaplAsset));
        when(processors.get("SELL")).thenReturn(sellProcessor);

        orderService.create(req);

        verify(sellProcessor).reserve(req);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldThrowWhenTryBalanceInsufficient() {
        CreateOrderRequest req = new CreateOrderRequest("C1", "AAPL", OrderSide.BUY, 9999, 1_000_000);
        when(assetRepository.findByCustomerIdAndAssetName("C1", "TRY")).thenReturn(Optional.of(tryAsset));
        when(processors.get("BUY")).thenReturn(buyProcessor);
        doThrow(new BusinessException("Insufficient balance")).when(buyProcessor).reserve(req);

        assertThrows(BusinessException.class, () -> orderService.create(req));
    }

    @Test
    void shouldThrowWhenAssetNotFoundOnCreate() {
        // given
        CreateOrderRequest req = new CreateOrderRequest("C1", "AAPL", OrderSide.BUY, 10, 100);
        when(processors.get("BUY")).thenReturn(buyProcessor);

        doThrow(new BusinessException("TRY asset not found"))
                .when(buyProcessor)
                .reserve(req);

        assertThrows(BusinessException.class, () -> orderService.create(req));

        verify(buyProcessor).reserve(req);
        verify(orderRepository, never()).save(any());
    }

    // --- CANCEL TESTS ---

    @Test
    void shouldCancelPendingBuyOrderAndRefundTRY() {
        Order order = new Order();
        order.setCustomerId("C1");
        order.setAssetName("AAPL");
        order.setSide(OrderSide.BUY);
        order.setSize(10);
        order.setPrice(100);
        order.setStatus(OrderStatus.PENDING);
        order.setCreateDate(Instant.now());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(assetRepository.findByCustomerIdAndAssetName("C1", "TRY")).thenReturn(Optional.of(tryAsset));

        orderService.cancel(1L, "C1", false);

        assertEquals(OrderStatus.CANCELED, order.getStatus());
        verify(assetRepository).save(any(Asset.class));
        verify(orderRepository).save(order);
    }

    @Test
    void shouldCancelPendingSellOrderAndRestoreAsset() {
        Order order = new Order();
        order.setCustomerId("C1");
        order.setAssetName("AAPL");
        order.setSide(OrderSide.SELL);
        order.setSize(10);
        order.setPrice(100);
        order.setStatus(OrderStatus.PENDING);
        order.setCreateDate(Instant.now());

        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(assetRepository.findByCustomerIdAndAssetName("C1", "AAPL")).thenReturn(Optional.of(aaplAsset));

        orderService.cancel(2L, "C1", false);

        assertEquals(OrderStatus.CANCELED, order.getStatus());
        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void shouldNotCancelMatchedOrder() {
        Order order = new Order();
        order.setCustomerId("C1");
        order.setAssetName("AAPL");
        order.setSide(OrderSide.BUY);
        order.setSize(10);
        order.setPrice(100);
        order.setStatus(OrderStatus.MATCHED);
        order.setCreateDate(Instant.now());

        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> orderService.cancel(3L, "C1", false));
        verify(assetRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCancelOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.cancel(99L, "C1", false));

    }

    // --- MATCH TESTS ---

    @Test
    void shouldMatchBuyOrderSuccessfully() {
        Order order = new Order();
        order.setCustomerId("C1");
        order.setAssetName("AAPL");
        order.setSide(OrderSide.BUY);
        order.setSize(10);
        order.setPrice(100);
        order.setStatus(OrderStatus.PENDING);
        order.setCreateDate(Instant.now());

        when(orderRepository.findById(4L)).thenReturn(Optional.of(order));
        when(processors.get("BUY")).thenReturn(buyProcessor);

        orderService.match(4L);

        verify(buyProcessor).match(order);
        assertEquals(OrderStatus.MATCHED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldThrowWhenMatchingInvalidSide() {
        Order order = new Order();
        order.setCustomerId("C1");
        order.setAssetName("AAPL");
        order.setSide(OrderSide.BUY);
        order.setSize(10);
        order.setPrice(100);
        order.setStatus(OrderStatus.PENDING);
        order.setCreateDate(Instant.now());

        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(processors.get("BUY")).thenReturn(null);

        assertThrows(BusinessException.class, () -> orderService.match(5L));
    }

    @Test
    void shouldThrowWhenMatchingNonPendingOrder() {
        Order order = new Order();
        order.setCustomerId("C1");
        order.setAssetName("AAPL");
        order.setSide(OrderSide.BUY);
        order.setSize(10);
        order.setPrice(100);
        order.setStatus(OrderStatus.MATCHED);
        order.setCreateDate(Instant.now());

        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> orderService.match(6L));
        verify(processors, never()).get(anyString());
    }

    @Test
    void shouldThrowWhenMatchOrderNotFound() {
        when(orderRepository.findById(77L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.match(77L));
    }

    // --- LISTING TESTS ---

    @Test
    void shouldListOrdersFilteredByCustomerIdAndStatus() {
        Order o1 = new Order();
        o1.setCustomerId("C1");
        o1.setAssetName("AAPL");
        o1.setSide(OrderSide.BUY);
        o1.setSize(10);
        o1.setPrice(100);
        o1.setStatus(OrderStatus.PENDING);
        o1.setCreateDate(Instant.now());

        Order o2 = new Order();
        o2.setCustomerId("C2");
        o2.setAssetName("AAPL");
        o2.setSide(OrderSide.SELL);
        o2.setSize(10);
        o2.setPrice(100);
        o2.setStatus(OrderStatus.CANCELED);
        o2.setCreateDate(Instant.now());

        when(orderRepository.findAll(Mockito.<Specification<Order>>any(), any(Sort.class)))
                .thenReturn(List.of(o1));

        var result = orderService.listOrders("C1", OrderStatus.PENDING, null, null);

        assertEquals(1, result.size());
        assertEquals("C1", result.get(0).customerId());
        verify(orderRepository).findAll(Mockito.<Specification<Order>>any(), any(Sort.class));
    }
}