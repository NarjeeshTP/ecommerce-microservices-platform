package com.ecommerce.pricingservice.controller;

import com.ecommerce.pricingservice.dto.PriceResponse;
import com.ecommerce.pricingservice.dto.PricingRuleRequest;
import com.ecommerce.pricingservice.dto.PricingRuleResponse;
import com.ecommerce.pricingservice.exception.PricingRuleNotFoundException;
import com.ecommerce.pricingservice.service.PricingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PricingController.class)
class PricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PricingService pricingService;

    private PricingRuleRequest testRequest;
    private PricingRuleResponse testResponse;
    private PriceResponse priceResponse;

    @BeforeEach
    void setUp() {
        testRequest = PricingRuleRequest.builder()
                .itemId("ITEM-001")
                .basePrice(new BigDecimal("100.00"))
                .discountPercent(new BigDecimal("10.00"))
                .currency("USD")
                .ruleType("STANDARD")
                .status("ACTIVE")
                .build();

        testResponse = PricingRuleResponse.builder()
                .id(1L)
                .itemId("ITEM-001")
                .basePrice(new BigDecimal("100.00"))
                .discountPercent(new BigDecimal("10.00"))
                .finalPrice(new BigDecimal("90.00"))
                .currency("USD")
                .ruleType("STANDARD")
                .status("ACTIVE")
                .build();

        priceResponse = PriceResponse.builder()
                .itemId("ITEM-001")
                .price(new BigDecimal("90.00"))
                .currency("USD")
                .source("DATABASE")
                .discountApplied(true)
                .originalPrice(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void createPricingRule_ShouldReturnCreated() throws Exception {
        when(pricingService.createPricingRule(any(PricingRuleRequest.class)))
                .thenReturn(testResponse);

        mockMvc.perform(post("/api/v1/pricing/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.itemId").value("ITEM-001"))
                .andExpect(jsonPath("$.basePrice").value(100.00))
                .andExpect(jsonPath("$.finalPrice").value(90.00));
    }

    @Test
    void createPricingRule_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        PricingRuleRequest invalidRequest = PricingRuleRequest.builder()
                .itemId("")  // Invalid: blank
                .basePrice(new BigDecimal("-10.00"))  // Invalid: negative
                .build();

        mockMvc.perform(post("/api/v1/pricing/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePricingRule_ShouldReturnUpdated() throws Exception {
        when(pricingService.updatePricingRule(eq(1L), any(PricingRuleRequest.class)))
                .thenReturn(testResponse);

        mockMvc.perform(put("/api/v1/pricing/rules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.itemId").value("ITEM-001"));
    }

    @Test
    void getPricingRuleById_WhenExists_ShouldReturnRule() throws Exception {
        when(pricingService.getPricingRuleById(1L)).thenReturn(testResponse);

        mockMvc.perform(get("/api/v1/pricing/rules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.itemId").value("ITEM-001"));
    }

    @Test
    void getPricingRuleById_WhenNotFound_ShouldReturnNotFound() throws Exception {
        when(pricingService.getPricingRuleById(999L))
                .thenThrow(new PricingRuleNotFoundException("Pricing rule not found with ID: 999"));

        mockMvc.perform(get("/api/v1/pricing/rules/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pricing rule not found with ID: 999"));
    }

    @Test
    void getAllPricingRules_ShouldReturnList() throws Exception {
        when(pricingService.getAllPricingRules()).thenReturn(Arrays.asList(testResponse));

        mockMvc.perform(get("/api/v1/pricing/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void deletePricingRule_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/pricing/rules/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getPriceForItem_ShouldReturnPrice() throws Exception {
        when(pricingService.getPriceForItem("ITEM-001")).thenReturn(priceResponse);

        mockMvc.perform(get("/api/v1/pricing/price/ITEM-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value("ITEM-001"))
                .andExpect(jsonPath("$.price").value(90.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.discountApplied").value(true));
    }

    @Test
    void getPriceForItemWithQuantity_ShouldReturnPrice() throws Exception {
        when(pricingService.getPriceForItemWithQuantity("ITEM-001", 10))
                .thenReturn(priceResponse);

        mockMvc.perform(get("/api/v1/pricing/price/ITEM-001/quantity/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value("ITEM-001"))
                .andExpect(jsonPath("$.price").value(90.00));
    }

    @Test
    void invalidateAllCache_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/pricing/cache/invalidate-all"))
                .andExpect(status().isOk())
                .andExpect(content().string("All price cache invalidated successfully"));
    }

    @Test
    void invalidateCacheForItem_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/pricing/cache/invalidate/ITEM-001"))
                .andExpect(status().isOk())
                .andExpect(content().string("Price cache invalidated for item: ITEM-001"));
    }
}

