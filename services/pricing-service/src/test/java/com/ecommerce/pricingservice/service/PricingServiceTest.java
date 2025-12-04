package com.ecommerce.pricingservice.service;

import com.ecommerce.pricingservice.dto.PriceResponse;
import com.ecommerce.pricingservice.dto.PricingRuleRequest;
import com.ecommerce.pricingservice.dto.PricingRuleResponse;
import com.ecommerce.pricingservice.entity.PricingRule;
import com.ecommerce.pricingservice.exception.PricingRuleNotFoundException;
import com.ecommerce.pricingservice.repository.PricingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @InjectMocks
    private PricingService pricingService;

    private PricingRule testRule;
    private PricingRuleRequest testRequest;

    @BeforeEach
    void setUp() {
        testRule = PricingRule.builder()
                .id(1L)
                .itemId("ITEM-001")
                .basePrice(new BigDecimal("100.00"))
                .discountPercent(new BigDecimal("10.00"))
                .finalPrice(new BigDecimal("90.00"))
                .currency("USD")
                .ruleType("STANDARD")
                .status("ACTIVE")
                .build();

        testRequest = PricingRuleRequest.builder()
                .itemId("ITEM-001")
                .basePrice(new BigDecimal("100.00"))
                .discountPercent(new BigDecimal("10.00"))
                .currency("USD")
                .ruleType("STANDARD")
                .status("ACTIVE")
                .build();
    }

    @Test
    void createPricingRule_ShouldReturnCreatedRule() {
        when(pricingRuleRepository.save(any(PricingRule.class))).thenReturn(testRule);

        PricingRuleResponse response = pricingService.createPricingRule(testRequest);

        assertThat(response).isNotNull();
        assertThat(response.getItemId()).isEqualTo("ITEM-001");
        assertThat(response.getBasePrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        verify(pricingRuleRepository, times(1)).save(any(PricingRule.class));
    }

    @Test
    void updatePricingRule_WhenRuleExists_ShouldReturnUpdatedRule() {
        when(pricingRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));
        when(pricingRuleRepository.save(any(PricingRule.class))).thenReturn(testRule);

        PricingRuleResponse response = pricingService.updatePricingRule(1L, testRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        verify(pricingRuleRepository, times(1)).findById(1L);
        verify(pricingRuleRepository, times(1)).save(any(PricingRule.class));
    }

    @Test
    void updatePricingRule_WhenRuleNotFound_ShouldThrowException() {
        when(pricingRuleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pricingService.updatePricingRule(1L, testRequest))
                .isInstanceOf(PricingRuleNotFoundException.class)
                .hasMessageContaining("Pricing rule not found with ID: 1");
    }

    @Test
    void getPricingRuleById_WhenRuleExists_ShouldReturnRule() {
        when(pricingRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));

        PricingRuleResponse response = pricingService.getPricingRuleById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getItemId()).isEqualTo("ITEM-001");
    }

    @Test
    void getPricingRuleById_WhenRuleNotFound_ShouldThrowException() {
        when(pricingRuleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pricingService.getPricingRuleById(1L))
                .isInstanceOf(PricingRuleNotFoundException.class);
    }

    @Test
    void getAllPricingRules_ShouldReturnAllRules() {
        when(pricingRuleRepository.findAll()).thenReturn(Arrays.asList(testRule));

        List<PricingRuleResponse> responses = pricingService.getAllPricingRules();

        assertThat(responses).isNotEmpty();
        assertThat(responses).hasSize(1);
    }

    @Test
    void getPriceForItem_WhenRuleExists_ShouldReturnPrice() {
        when(pricingRuleRepository.findBestPricingRuleForItem("ITEM-001", 1))
                .thenReturn(Arrays.asList(testRule));

        PriceResponse response = pricingService.getPriceForItem("ITEM-001");

        assertThat(response).isNotNull();
        assertThat(response.getItemId()).isEqualTo("ITEM-001");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(response.getDiscountApplied()).isTrue();
    }

    @Test
    void getPriceForItem_WhenNoRuleExists_ShouldThrowException() {
        when(pricingRuleRepository.findBestPricingRuleForItem("ITEM-999", 1))
                .thenReturn(Arrays.asList());

        assertThatThrownBy(() -> pricingService.getPriceForItem("ITEM-999"))
                .isInstanceOf(PricingRuleNotFoundException.class)
                .hasMessageContaining("No active pricing rule found for item: ITEM-999");
    }

    @Test
    void deletePricingRule_WhenRuleExists_ShouldDeleteRule() {
        when(pricingRuleRepository.existsById(1L)).thenReturn(true);
        doNothing().when(pricingRuleRepository).deleteById(1L);

        pricingService.deletePricingRule(1L);

        verify(pricingRuleRepository, times(1)).deleteById(1L);
    }

    @Test
    void deletePricingRule_WhenRuleNotFound_ShouldThrowException() {
        when(pricingRuleRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> pricingService.deletePricingRule(1L))
                .isInstanceOf(PricingRuleNotFoundException.class);
    }

    @Test
    void getPriceForItemWithQuantity_ShouldApplyBulkPricing() {
        PricingRule bulkRule = PricingRule.builder()
                .id(2L)
                .itemId("ITEM-001")
                .basePrice(new BigDecimal("100.00"))
                .discountPercent(new BigDecimal("20.00"))
                .finalPrice(new BigDecimal("80.00"))
                .currency("USD")
                .ruleType("BULK")
                .minQuantity(10)
                .status("ACTIVE")
                .build();

        when(pricingRuleRepository.findBestPricingRuleForItem("ITEM-001", 10))
                .thenReturn(Arrays.asList(bulkRule));

        PriceResponse response = pricingService.getPriceForItemWithQuantity("ITEM-001", 10);

        assertThat(response).isNotNull();
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(response.getDiscountApplied()).isTrue();
        assertThat(response.getOriginalPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}

