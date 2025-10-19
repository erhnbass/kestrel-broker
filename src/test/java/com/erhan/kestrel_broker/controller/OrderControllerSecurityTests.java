package com.erhan.kestrel_broker.controller;

import com.erhan.kestrel_broker.domain.entity.Asset;
import com.erhan.kestrel_broker.domain.entity.Order;
import com.erhan.kestrel_broker.domain.enums.OrderSide;
import com.erhan.kestrel_broker.domain.enums.OrderStatus;
import com.erhan.kestrel_broker.repository.AssetRepository;
import com.erhan.kestrel_broker.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AssetRepository assetRepository;

    @BeforeEach
    void setup() {
        // Flyway asset verisini getiriyor, biz sadece order’ları sıfırlıyoruz
        orderRepository.deleteAll();

        // C1 için zaten TRY ve AAPL var → sadece order ekle
        Order order1 = new Order();
        order1.setCustomerId("C1");
        order1.setAssetName("AAPL");
        order1.setSide(OrderSide.BUY);
        order1.setSize(10);
        order1.setPrice(1000);
        order1.setStatus(OrderStatus.PENDING);
        order1.setCreateDate(Instant.now());
        orderRepository.save(order1);

        // C2 için TRY ve GOOG ekle (TRY asset varsa kullan)
        Asset tryAsset2 = assetRepository.findByCustomerIdAndAssetName("C2", "TRY")
                .orElseGet(() -> {
                    Asset a = new Asset();
                    a.setCustomerId("C2");
                    a.setAssetName("TRY");
                    a.setSize(500_000);
                    a.setUsableSize(500_000);
                    a.setVersion(0L);
                    return assetRepository.save(a);
                });

        Order order2 = new Order();
        order2.setCustomerId("C2");
        order2.setAssetName("GOOG");
        order2.setSide(OrderSide.SELL);
        order2.setSize(5);
        order2.setPrice(2000);
        order2.setStatus(OrderStatus.PENDING);
        order2.setCreateDate(Instant.now());
        orderRepository.save(order2);
    }

    @Test
    @WithMockUser(username = "C1", roles = {"CUSTOMER"})
    void customerShouldSeeOnlyOwnOrders() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].customerId", everyItem(equalTo("C1"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminShouldSeeAllOrders() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].customerId", hasItems("C1", "C2")));
    }
}
