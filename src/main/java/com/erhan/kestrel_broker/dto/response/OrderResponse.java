package com.erhan.kestrel_broker.dto.response;

import com.erhan.kestrel_broker.domain.entity.Order;
import com.erhan.kestrel_broker.domain.enums.OrderSide;
import com.erhan.kestrel_broker.domain.enums.OrderStatus;

import java.time.Instant;

public record OrderResponse(
        Long id,
        String customerId,
        String assetName,
        OrderSide side,
        long size,
        long price,
        OrderStatus status,
        Instant createDate
) {
    public static OrderResponse from(Order order) {
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