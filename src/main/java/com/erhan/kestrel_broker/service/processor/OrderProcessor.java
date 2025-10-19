package com.erhan.kestrel_broker.service.processor;

import com.erhan.kestrel_broker.domain.entity.Order;
import com.erhan.kestrel_broker.dto.request.CreateOrderRequest;

public interface OrderProcessor {

    void reserve(CreateOrderRequest req);
    void match(Order order);

}
