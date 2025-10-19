package com.erhan.kestrel_broker.controller;

import com.erhan.kestrel_broker.domain.enums.OrderStatus;
import com.erhan.kestrel_broker.dto.request.CreateOrderRequest;
import com.erhan.kestrel_broker.dto.request.MatchOrdersRequest;
import com.erhan.kestrel_broker.dto.response.MatchOrdersResponse;
import com.erhan.kestrel_broker.dto.response.OrderResponse;
import com.erhan.kestrel_broker.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "basicAuth")
public class OrderController {

    private final OrderService orderService;

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @PostMapping
    public OrderResponse createOrder(@RequestBody @Valid CreateOrderRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            req = new CreateOrderRequest(username, req.assetName(), req.side(), req.size(), req.price());
        }

        return orderService.create(req);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping
    public List<OrderResponse> listOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            customerId = username;
        }

        return orderService.listOrders(customerId, status, from, to);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable Long orderId, Authentication auth) {
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return orderService.cancel(orderId, username, isAdmin);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/match")
    public ResponseEntity<OrderResponse> matchOrder(@PathVariable Long id) {
        var order = orderService.match(id);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/match")
    public MatchOrdersResponse matchOrders(@RequestBody MatchOrdersRequest request) {
        return orderService.matchOrders(request.buyOrderId(), request.sellOrderId());
    }

}
