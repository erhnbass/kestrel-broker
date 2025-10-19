package com.erhan.kestrel_broker.dto.response;

import com.erhan.kestrel_broker.domain.entity.Asset;

public record AssetResponse(String assetName, long size, long usableSize) {
    public static AssetResponse from(Asset asset) {
        return new AssetResponse(asset.getAssetName(), asset.getSize(), asset.getUsableSize());
    }
}
