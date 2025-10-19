package com.erhan.kestrel_broker.dto.response;

public record MatchOrdersResponse(
        OrderResponse buyOrder,
        OrderResponse sellOrder,
        String message
) {}
