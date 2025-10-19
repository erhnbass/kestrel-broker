package com.erhan.kestrel_broker.service.processor;

import com.erhan.kestrel_broker.domain.entity.Asset;
import com.erhan.kestrel_broker.domain.entity.Order;
import com.erhan.kestrel_broker.dto.request.CreateOrderRequest;
import com.erhan.kestrel_broker.exception.BusinessException;
import com.erhan.kestrel_broker.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("SELL")
@RequiredArgsConstructor
public class SellOrderProcessor implements OrderProcessor {

    private final AssetRepository assetRepository;

    @Override
    public void reserve(CreateOrderRequest req) {
        Asset asset = assetRepository.findByCustomerIdAndAssetName(req.customerId(), req.assetName())
                .orElseThrow(() -> new BusinessException("Asset not found"));
        if (asset.getUsableSize() < req.size())
            throw new BusinessException("Insufficient asset usable balance");
        asset.setUsableSize(asset.getUsableSize() - req.size());
        assetRepository.save(asset);
    }

    @Override
    public void match(Order order) {
        Asset asset = assetRepository.findByCustomerIdAndAssetName(order.getCustomerId(), order.getAssetName())
                .orElseThrow(() -> new BusinessException("Asset not found"));
        asset.setSize(asset.getSize() - order.getSize());

        long proceeds = Math.multiplyExact(order.getSize(), order.getPrice());
        Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(order.getCustomerId(), "TRY")
                .orElseThrow(() -> new BusinessException("TRY asset not found"));
        tryAsset.setSize(tryAsset.getSize() + proceeds);
        tryAsset.setUsableSize(tryAsset.getUsableSize() + proceeds);
        assetRepository.save(asset);
        assetRepository.save(tryAsset);
    }
}
