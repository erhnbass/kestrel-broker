package com.erhan.kestrel_broker.service.processor;

import com.erhan.kestrel_broker.domain.entity.Asset;
import com.erhan.kestrel_broker.domain.entity.Order;
import com.erhan.kestrel_broker.dto.request.CreateOrderRequest;
import com.erhan.kestrel_broker.exception.BusinessException;
import com.erhan.kestrel_broker.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("BUY")
@RequiredArgsConstructor
public class BuyOrderProcessor implements OrderProcessor {

    private final AssetRepository assetRepository;

    @Override
    public void reserve(CreateOrderRequest req) {
        long cost = Math.multiplyExact(req.size(), req.price());
        Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(req.customerId(), "TRY")
                .orElseThrow(() -> new BusinessException("TRY asset not found"));
        if (tryAsset.getUsableSize() < cost)
            throw new BusinessException("Insufficient TRY usable balance");
        tryAsset.setUsableSize(tryAsset.getUsableSize() - cost);
        assetRepository.save(tryAsset);
    }

    @Override
    public void match(Order order) {
        long cost = Math.multiplyExact(order.getSize(), order.getPrice());
        Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(order.getCustomerId(), "TRY")
                .orElseThrow(() -> new BusinessException("TRY asset not found"));
        tryAsset.setSize(tryAsset.getSize() - cost);

        Asset asset = assetRepository.findByCustomerIdAndAssetName(order.getCustomerId(), order.getAssetName())
                .orElseThrow(() -> new BusinessException("Asset not found"));
        asset.setSize(asset.getSize() + order.getSize());
        asset.setUsableSize(asset.getUsableSize() + order.getSize());
        assetRepository.save(asset);
        assetRepository.save(tryAsset);
    }
}
