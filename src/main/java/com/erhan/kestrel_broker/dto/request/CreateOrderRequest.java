package com.erhan.kestrel_broker.dto.request;

import com.erhan.kestrel_broker.domain.enums.OrderSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(

        @NotBlank
        String customerId,

        @Pattern(regexp = "^[A-Z0-9_.-]+$")
        String assetName,

        @NotNull
        OrderSide side,

        @Positive
        long size,

        @Positive
        long price
) {}
