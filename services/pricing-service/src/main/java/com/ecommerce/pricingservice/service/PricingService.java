package com.ecommerce.pricingservice.service;

import com.ecommerce.pricingservice.dto.PriceResponse;
import com.ecommerce.pricingservice.dto.PricingRuleRequest;
import com.ecommerce.pricingservice.dto.PricingRuleResponse;
import com.ecommerce.pricingservice.entity.PricingRule;
import com.ecommerce.pricingservice.exception.PricingRuleNotFoundException;
import com.ecommerce.pricingservice.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PricingService {

    private final PricingRuleRepository pricingRuleRepository;

    @Transactional
    public PricingRuleResponse createPricingRule(PricingRuleRequest request) {
        log.info("Creating pricing rule for item: {}", request.getItemId());

        PricingRule rule = PricingRule.builder()
                .itemId(request.getItemId())
                .basePrice(request.getBasePrice())
                .discountPercent(request.getDiscountPercent())
                .currency(request.getCurrency())
                .ruleType(request.getRuleType())
                .minQuantity(request.getMinQuantity())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        PricingRule savedRule = pricingRuleRepository.save(rule);
        log.info("Pricing rule created with ID: {}", savedRule.getId());

        return mapToResponse(savedRule);
    }

    @Transactional
    @CacheEvict(value = "prices", allEntries = true)
    public PricingRuleResponse updatePricingRule(Long id, PricingRuleRequest request) {
        log.info("Updating pricing rule with ID: {}", id);

        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new PricingRuleNotFoundException("Pricing rule not found with ID: " + id));

        rule.setBasePrice(request.getBasePrice());
        rule.setDiscountPercent(request.getDiscountPercent());
        rule.setCurrency(request.getCurrency());
        rule.setRuleType(request.getRuleType());
        rule.setMinQuantity(request.getMinQuantity());
        rule.setValidFrom(request.getValidFrom());
        rule.setValidUntil(request.getValidUntil());
        if (request.getStatus() != null) {
            rule.setStatus(request.getStatus());
        }

        // Manually recalculate final price to ensure it's updated
        if (rule.getDiscountPercent() != null && rule.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = rule.getBasePrice().multiply(rule.getDiscountPercent()).divide(new BigDecimal("100"));
            rule.setFinalPrice(rule.getBasePrice().subtract(discount));
        } else {
            rule.setFinalPrice(rule.getBasePrice());
        }

        PricingRule updatedRule = pricingRuleRepository.save(rule);
        log.info("Pricing rule updated: {}", updatedRule.getId());

        return mapToResponse(updatedRule);
    }

    @Transactional(readOnly = true)
    public PricingRuleResponse getPricingRuleById(Long id) {
        log.debug("Fetching pricing rule by ID: {}", id);
        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new PricingRuleNotFoundException("Pricing rule not found with ID: " + id));
        return mapToResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<PricingRuleResponse> getAllPricingRules() {
        log.debug("Fetching all pricing rules");
        return pricingRuleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PricingRuleResponse> getPricingRulesByItemId(String itemId) {
        log.debug("Fetching pricing rules for item: {}", itemId);
        return pricingRuleRepository.findByItemId(itemId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "prices", allEntries = true)
    public void deletePricingRule(Long id) {
        log.info("Deleting pricing rule with ID: {}", id);
        if (!pricingRuleRepository.existsById(id)) {
            throw new PricingRuleNotFoundException("Pricing rule not found with ID: " + id);
        }
        pricingRuleRepository.deleteById(id);
        log.info("Pricing rule deleted: {}", id);
    }

    @Cacheable(value = "prices", key = "#itemId", unless = "#result == null")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "pricingService", fallbackMethod = "getPriceFallback")
    @Transactional(readOnly = true, timeout = 3) // 3 seconds timeout at transaction level
    public PriceResponse getPriceForItem(String itemId) {
        return getPriceForItemWithQuantity(itemId, 1);
    }

    @Cacheable(value = "prices", key = "#itemId + '_' + #quantity", unless = "#result == null")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "pricingService", fallbackMethod = "getPriceWithQuantityFallback")
    @Transactional(readOnly = true, timeout = 3) // 3 seconds timeout at transaction level
    public PriceResponse getPriceForItemWithQuantity(String itemId, Integer quantity) {
        log.info("Fetching price for item: {} with quantity: {}", itemId, quantity);

        List<PricingRule> rules = pricingRuleRepository.findBestPricingRuleForItem(itemId, quantity);

        if (rules.isEmpty()) {
            log.warn("No pricing rule found for item: {}", itemId);
            throw new PricingRuleNotFoundException("No active pricing rule found for item: " + itemId);
        }

        // Get the best rule (first one due to ORDER BY discountPercent DESC)
        PricingRule bestRule = rules.get(0);

        PriceResponse response = PriceResponse.builder()
                .itemId(itemId)
                .price(bestRule.getFinalPrice())
                .currency(bestRule.getCurrency())
                .source(null) // Will be set by cache interceptor
                .discountApplied(bestRule.getDiscountPercent() != null &&
                                bestRule.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0)
                .originalPrice(bestRule.getBasePrice())
                .build();

        log.info("Price fetched for item {}: {} {}", itemId, response.getPrice(), response.getCurrency());
        return response;
    }

    @CacheEvict(value = "prices", allEntries = true)
    public void invalidateAllPriceCache() {
        log.info("Invalidating all price cache");
    }

    @CacheEvict(value = "prices", key = "#itemId")
    public void invalidatePriceCacheForItem(String itemId) {
        log.info("Invalidating price cache for item: {}", itemId);
    }

    // Fallback method for resilience
    public PriceResponse getPriceFallback(String itemId, Throwable throwable) {
        log.error("Fallback triggered for item: {}. Error: {}", itemId, throwable.getMessage());

        // Try to get cached value first
        return PriceResponse.builder()
                .itemId(itemId)
                .price(BigDecimal.ZERO)
                .currency("USD")
                .source("FALLBACK")
                .discountApplied(false)
                .build();
    }

    // Fallback method for getPriceForItemWithQuantity
    public PriceResponse getPriceWithQuantityFallback(String itemId, Integer quantity, Throwable throwable) {
        log.error("Fallback triggered for item: {} with quantity: {}. Error: {}", itemId, quantity, throwable.getMessage());

        // Try to return cached or default value
        return PriceResponse.builder()
                .itemId(itemId)
                .price(BigDecimal.ZERO)
                .currency("USD")
                .source("FALLBACK")
                .discountApplied(false)
                .build();
    }

    private PricingRuleResponse mapToResponse(PricingRule rule) {
        return PricingRuleResponse.builder()
                .id(rule.getId())
                .itemId(rule.getItemId())
                .basePrice(rule.getBasePrice())
                .discountPercent(rule.getDiscountPercent())
                .finalPrice(rule.getFinalPrice())
                .currency(rule.getCurrency())
                .ruleType(rule.getRuleType())
                .minQuantity(rule.getMinQuantity())
                .validFrom(rule.getValidFrom())
                .validUntil(rule.getValidUntil())
                .status(rule.getStatus())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}

