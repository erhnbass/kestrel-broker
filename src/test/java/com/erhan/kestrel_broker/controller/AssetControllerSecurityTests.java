package com.erhan.kestrel_broker.controller;

import com.erhan.kestrel_broker.domain.entity.Asset;
import com.erhan.kestrel_broker.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AssetControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssetRepository assetRepository;

    @BeforeEach
    void setup() {
        // Flyway zaten C1-TRY ve C1-AAPL ekliyor
        // Ek olarak C2 verisini manuel ekleyelim
        var existsC2 = assetRepository.findByCustomerIdAndAssetName("C2", "USD");
        if (existsC2.isEmpty()) {
            var usdAsset = new com.erhan.kestrel_broker.domain.entity.Asset();
            usdAsset.setCustomerId("C2");
            usdAsset.setAssetName("USD");
            usdAsset.setSize(2_000);
            usdAsset.setUsableSize(2_000);
            usdAsset.setVersion(0L);
            assetRepository.save(usdAsset);
        }
    }

    @Test
    @WithMockUser(username = "C1", roles = {"CUSTOMER"})
    void customerShouldSeeOnlyOwnAssets() throws Exception {
        mockMvc.perform(get("/api/assets")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].customerId", everyItem(equalTo("C1"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminShouldSeeAllAssets() throws Exception {
        mockMvc.perform(get("/api/assets")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].customerId", hasItems("C1", "C2")));
    }
}