package com.ecommerce.pricingservice.integration;

import com.ecommerce.pricingservice.dto.PriceResponse;
import com.ecommerce.pricingservice.dto.PricingRuleRequest;
import com.ecommerce.pricingservice.dto.PricingRuleResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PricingServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("pricing_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        registry.add("spring.cache.type", () -> "none"); // Disable Redis for integration tests
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testFullPricingWorkflow() throws Exception {
        // Step 1: Create a pricing rule
        PricingRuleRequest createRequest = PricingRuleRequest.builder()
                .itemId("INTEGRATION-ITEM-001")
                .basePrice(new BigDecimal("200.00"))
                .discountPercent(new BigDecimal("15.00"))
                .currency("USD")
                .ruleType("PROMOTIONAL")
                .status("ACTIVE")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/pricing/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemId").value("INTEGRATION-ITEM-001"))
                .andExpect(jsonPath("$.basePrice").value(200.00))
                .andExpect(jsonPath("$.finalPrice").value(170.00))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        PricingRuleResponse createdRule = objectMapper.readValue(responseBody, PricingRuleResponse.class);
        Long ruleId = createdRule.getId();

        // Step 2: Get pricing rule by ID
        mockMvc.perform(get("/api/v1/pricing/rules/" + ruleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ruleId))
                .andExpect(jsonPath("$.itemId").value("INTEGRATION-ITEM-001"));

        // Step 3: Get price for item
        MvcResult priceResult = mockMvc.perform(get("/api/v1/pricing/price/INTEGRATION-ITEM-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value("INTEGRATION-ITEM-001"))
                .andExpect(jsonPath("$.price").value(170.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andReturn();

        String priceBody = priceResult.getResponse().getContentAsString();
        PriceResponse priceResponse = objectMapper.readValue(priceBody, PriceResponse.class);
        assertThat(priceResponse.getDiscountApplied()).isTrue();
        assertThat(priceResponse.getOriginalPrice()).isEqualByComparingTo(new BigDecimal("200.00"));

        // Step 4: Update pricing rule
        PricingRuleRequest updateRequest = PricingRuleRequest.builder()
                .itemId("INTEGRATION-ITEM-001")
                .basePrice(new BigDecimal("250.00"))
                .discountPercent(new BigDecimal("20.00"))
                .currency("USD")
                .ruleType("SEASONAL")
                .status("ACTIVE")
                .build();

        mockMvc.perform(put("/api/v1/pricing/rules/" + ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePrice").value(250.00))
                .andExpect(jsonPath("$.finalPrice").value(200.00));

        // Step 5: Get all pricing rules
        mockMvc.perform(get("/api/v1/pricing/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Step 6: Delete pricing rule
        mockMvc.perform(delete("/api/v1/pricing/rules/" + ruleId))
                .andExpect(status().isNoContent());

        // Step 7: Verify deletion
        mockMvc.perform(get("/api/v1/pricing/rules/" + ruleId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testBulkPricing() throws Exception {
        // Create bulk pricing rule
        PricingRuleRequest bulkRequest = PricingRuleRequest.builder()
                .itemId("BULK-ITEM-001")
                .basePrice(new BigDecimal("100.00"))
                .discountPercent(new BigDecimal("25.00"))
                .currency("USD")
                .ruleType("BULK")
                .minQuantity(10)
                .status("ACTIVE")
                .build();

        mockMvc.perform(post("/api/v1/pricing/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andExpect(status().isCreated());

        // Get price with quantity
        mockMvc.perform(get("/api/v1/pricing/price/BULK-ITEM-001/quantity/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(75.00))
                .andExpect(jsonPath("$.discountApplied").value(true));
    }

    @Test
    void testPriceNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/pricing/price/NON-EXISTENT-ITEM"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No active pricing rule found for item: NON-EXISTENT-ITEM"));
    }

    @Test
    void testInvalidPricingRuleCreation() throws Exception {
        PricingRuleRequest invalidRequest = PricingRuleRequest.builder()
                .itemId("")  // Invalid: blank
                .basePrice(new BigDecimal("-50.00"))  // Invalid: negative
                .build();

        mockMvc.perform(post("/api/v1/pricing/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}

