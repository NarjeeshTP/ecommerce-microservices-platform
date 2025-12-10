package com.ecommerce.cart.client;

import com.ecommerce.cart.dto.external.PriceDTO;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for Pricing Service API
 * Verifies that the pricing service returns expected structure
 */
class PricingServiceContractTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private PricingServiceClient pricingClient;

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder()
                .baseUrl(wireMock.baseUrl())
                .build();
        pricingClient = new PricingServiceClient(webClient);
    }

    @Test
    void pricingApi_shouldReturnValidPrice_whenProductExists() {
        // Given: Pricing service returns a price
        String productId = "PROD-001";
        wireMock.stubFor(get(urlEqualTo("/pricing/price/" + productId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "productId": "PROD-001",
                                    "finalPrice": 899.99,
                                    "basePrice": 999.99,
                                    "discount": 100.00,
                                    "currency": "USD",
                                    "effectiveDate": "2025-12-07T10:00:00"
                                }
                                """)));

        // When: We fetch the price
        PriceDTO price = pricingClient.getPriceBlocking(productId);

        // Then: Price structure matches expected contract
        assertThat(price).isNotNull();
        assertThat(price.getProductId()).isEqualTo("PROD-001");
        assertThat(price.getFinalPrice()).isEqualByComparingTo(new BigDecimal("899.99"));
        assertThat(price.getBasePrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(price.getDiscount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(price.getCurrency()).isEqualTo("USD");
        assertThat(price.getEffectiveDate()).isNotNull();

        // Verify the request was made
        wireMock.verify(getRequestedFor(urlEqualTo("/pricing/price/" + productId)));
    }

    @Test
    void pricingApi_shouldReturnNull_whenPriceNotFound() {
        // Given: Pricing service returns 404
        String productId = "INVALID-ID";
        wireMock.stubFor(get(urlEqualTo("/pricing/price/" + productId))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "error": "Price not found"
                                }
                                """)));

        // When: We fetch non-existent price
        PriceDTO price = pricingClient.getPriceBlocking(productId);

        // Then: Should return null (handled gracefully)
        assertThat(price).isNull();
    }

    @Test
    void pricingApi_shouldHandleServiceUnavailable() {
        // Given: Pricing service is down
        String productId = "PROD-002";
        wireMock.stubFor(get(urlEqualTo("/pricing/price/" + productId))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withBody("Service Unavailable")));

        // When: We fetch the price
        PriceDTO price = pricingClient.getPriceBlocking(productId);

        // Then: Should handle error gracefully
        assertThat(price).isNull();
    }

    @Test
    void getFinalPrice_shouldReturnBigDecimal() {
        // Given
        String productId = "PROD-003";
        wireMock.stubFor(get(urlEqualTo("/pricing/price/" + productId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "productId": "PROD-003",
                                    "finalPrice": 49.99,
                                    "basePrice": 59.99,
                                    "discount": 10.00,
                                    "currency": "USD"
                                }
                                """)));

        // When
        BigDecimal finalPrice = pricingClient.getFinalPriceBlocking(productId);

        // Then
        assertThat(finalPrice).isEqualByComparingTo(new BigDecimal("49.99"));
    }

    @Test
    void pricingApi_shouldValidatePriceIsPositive() {
        // Given: Pricing service returns a price
        String productId = "PROD-004";
        wireMock.stubFor(get(urlEqualTo("/pricing/price/" + productId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "productId": "PROD-004",
                                    "finalPrice": 0.01,
                                    "basePrice": 0.01,
                                    "discount": 0.00,
                                    "currency": "USD"
                                }
                                """)));

        // When
        PriceDTO price = pricingClient.getPriceBlocking(productId);

        // Then: Price should be positive
        assertThat(price).isNotNull();
        assertThat(price.getFinalPrice()).isGreaterThan(BigDecimal.ZERO);
    }
}

