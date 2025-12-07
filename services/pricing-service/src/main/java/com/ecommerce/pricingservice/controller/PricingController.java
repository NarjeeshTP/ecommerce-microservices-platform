package com.ecommerce.pricingservice.controller;

import com.ecommerce.pricingservice.dto.PriceResponse;
import com.ecommerce.pricingservice.dto.PricingRuleRequest;
import com.ecommerce.pricingservice.dto.PricingRuleResponse;
import com.ecommerce.pricingservice.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pricing", description = "Pricing management APIs")
public class PricingController {

    private final PricingService pricingService;
    private final CacheManager cacheManager;

    @Operation(summary = "Create a new pricing rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pricing rule created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/rules")
    public ResponseEntity<PricingRuleResponse> createPricingRule(
            @Valid @RequestBody PricingRuleRequest request) {
        log.info("POST /api/v1/pricing/rules - Creating pricing rule for item: {}", request.getItemId());
        PricingRuleResponse response = pricingService.createPricingRule(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing pricing rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pricing rule updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Pricing rule not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/rules/{id}")
    public ResponseEntity<PricingRuleResponse> updatePricingRule(
            @Parameter(description = "Pricing rule ID") @PathVariable Long id,
            @Valid @RequestBody PricingRuleRequest request) {
        log.info("PUT /api/v1/pricing/rules/{} - Updating pricing rule", id);
        PricingRuleResponse response = pricingService.updatePricingRule(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get a pricing rule by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pricing rule found"),
            @ApiResponse(responseCode = "404", description = "Pricing rule not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/rules/{id}")
    public ResponseEntity<PricingRuleResponse> getPricingRuleById(
            @Parameter(description = "Pricing rule ID") @PathVariable Long id) {
        log.info("GET /api/v1/pricing/rules/{} - Fetching pricing rule", id);
        PricingRuleResponse response = pricingService.getPricingRuleById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all pricing rules")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all pricing rules")
    @GetMapping("/rules")
    public ResponseEntity<List<PricingRuleResponse>> getAllPricingRules() {
        log.info("GET /api/v1/pricing/rules - Fetching all pricing rules");
        List<PricingRuleResponse> responses = pricingService.getAllPricingRules();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get pricing rules for a specific item")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved pricing rules for item")
    @GetMapping("/rules/item/{itemId}")
    public ResponseEntity<List<PricingRuleResponse>> getPricingRulesByItemId(
            @Parameter(description = "Item ID") @PathVariable String itemId) {
        log.info("GET /api/v1/pricing/rules/item/{} - Fetching pricing rules for item", itemId);
        List<PricingRuleResponse> responses = pricingService.getPricingRulesByItemId(itemId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Delete a pricing rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pricing rule deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Pricing rule not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deletePricingRule(
            @Parameter(description = "Pricing rule ID") @PathVariable Long id) {
        log.info("DELETE /api/v1/pricing/rules/{} - Deleting pricing rule", id);
        pricingService.deletePricingRule(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get price for an item (cached)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Price retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No pricing rule found for item"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/price/{itemId}")
    public ResponseEntity<PriceResponse> getPriceForItem(
            @Parameter(description = "Item ID") @PathVariable String itemId) {
        log.info("GET /api/v1/pricing/price/{} - Fetching price for item", itemId);

        // Check if value is in cache before calling service
        boolean isFromCache = false;
        Cache cache = cacheManager.getCache("prices");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(itemId);
            isFromCache = (wrapper != null);
        }

        PriceResponse response = pricingService.getPriceForItem(itemId);

        // Set source based on cache status (only if not already set by fallback)
        if (response.getSource() == null) {
            response.setSource(isFromCache ? "CACHE" : "DATABASE");
        }
        log.info("Price source for '{}': {}", itemId, response.getSource());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get price for an item with quantity (cached)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Price retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No pricing rule found for item"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/price/{itemId}/quantity/{quantity}")
    public ResponseEntity<PriceResponse> getPriceForItemWithQuantity(
            @Parameter(description = "Item ID") @PathVariable String itemId,
            @Parameter(description = "Quantity") @PathVariable Integer quantity) {
        log.info("GET /api/v1/pricing/price/{}/quantity/{} - Fetching price for item with quantity", itemId, quantity);

        // Check if value is in cache before calling service
        String cacheKey = itemId + "_" + quantity;
        boolean isFromCache = false;
        Cache cache = cacheManager.getCache("prices");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            isFromCache = (wrapper != null);
        }

        PriceResponse response = pricingService.getPriceForItemWithQuantity(itemId, quantity);

        // Set source based on cache status (only if not already set by fallback)
        if (response.getSource() == null) {
            response.setSource(isFromCache ? "CACHE" : "DATABASE");
        }
        log.info("Price source for '{}': {}", cacheKey, response.getSource());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Invalidate all price cache")
    @ApiResponse(responseCode = "200", description = "Cache invalidated successfully")
    @PostMapping("/cache/invalidate-all")
    public ResponseEntity<String> invalidateAllCache() {
        log.info("POST /api/v1/pricing/cache/invalidate-all - Invalidating all cache");
        pricingService.invalidateAllPriceCache();
        return ResponseEntity.ok("All price cache invalidated successfully");
    }

    @Operation(summary = "Invalidate price cache for a specific item")
    @ApiResponse(responseCode = "200", description = "Cache invalidated successfully")
    @PostMapping("/cache/invalidate/{itemId}")
    public ResponseEntity<String> invalidateCacheForItem(
            @Parameter(description = "Item ID") @PathVariable String itemId) {
        log.info("POST /api/v1/pricing/cache/invalidate/{} - Invalidating cache for item", itemId);
        pricingService.invalidatePriceCacheForItem(itemId);
        return ResponseEntity.ok("Price cache invalidated for item: " + itemId);
    }
}

