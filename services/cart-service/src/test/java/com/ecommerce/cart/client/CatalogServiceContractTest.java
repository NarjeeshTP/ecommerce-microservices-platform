package com.ecommerce.cart.client;

import com.ecommerce.cart.dto.external.ProductDTO;
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
 * Contract tests for Catalog Service API
 * Verifies that the catalog service returns expected structure
 */
class CatalogServiceContractTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private CatalogServiceClient catalogClient;

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder()
                .baseUrl(wireMock.baseUrl())
                .build();
        catalogClient = new CatalogServiceClient(webClient);
    }

    @Test
    void catalogApi_shouldReturnValidProduct_whenProductExists() {
        // Given: Catalog service returns a product
        String productId = "PROD-001";
        wireMock.stubFor(get(urlEqualTo("/api/products/" + productId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "PROD-001",
                                    "name": "Laptop",
                                    "description": "High-performance laptop",
                                    "category": "Electronics",
                                    "basePrice": 999.99,
                                    "available": true
                                }
                                """)));

        // When: We fetch the product
        ProductDTO product = catalogClient.getProductBlocking(productId);

        // Then: Product structure matches expected contract
        assertThat(product).isNotNull();
        assertThat(product.getId()).isEqualTo("PROD-001");
        assertThat(product.getName()).isEqualTo("Laptop");
        assertThat(product.getDescription()).isEqualTo("High-performance laptop");
        assertThat(product.getBasePrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(product.getAvailable()).isTrue();

        // Verify the request was made
        wireMock.verify(getRequestedFor(urlEqualTo("/api/products/" + productId)));
    }

    @Test
    void catalogApi_shouldReturnNull_whenProductNotFound() {
        // Given: Catalog service returns 404
        String productId = "INVALID-ID";
        wireMock.stubFor(get(urlEqualTo("/api/products/" + productId))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "error": "Product not found"
                                }
                                """)));

        // When: We fetch non-existent product
        ProductDTO product = catalogClient.getProductBlocking(productId);

        // Then: Should return null (handled gracefully)
        assertThat(product).isNull();
    }

    @Test
    void catalogApi_shouldHandleServiceUnavailable() {
        // Given: Catalog service is down
        String productId = "PROD-002";
        wireMock.stubFor(get(urlEqualTo("/api/products/" + productId))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withBody("Service Unavailable")));

        // When: We fetch the product
        ProductDTO product = catalogClient.getProductBlocking(productId);

        // Then: Should handle error gracefully
        assertThat(product).isNull();
    }

    @Test
    void catalogApi_shouldHandleTimeout() {
        // Given: Catalog service times out
        String productId = "PROD-003";
        wireMock.stubFor(get(urlEqualTo("/api/products/" + productId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(6000)  // Longer than timeout
                        .withBody("{}")));

        // When: We fetch the product
        ProductDTO product = catalogClient.getProductBlocking(productId);

        // Then: Should handle timeout gracefully
        assertThat(product).isNull();
    }

    @Test
    void productExists_shouldReturnTrue_whenProductExists() {
        // Given
        String productId = "PROD-004";
        wireMock.stubFor(get(urlEqualTo("/api/products/" + productId))
                .willReturn(aResponse().withStatus(200)));

        // When
        Boolean exists = catalogClient.productExists(productId).block();

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void productExists_shouldReturnFalse_whenProductNotFound() {
        // Given
        String productId = "INVALID";
        wireMock.stubFor(get(urlEqualTo("/api/products/" + productId))
                .willReturn(aResponse().withStatus(404)));

        // When
        Boolean exists = catalogClient.productExists(productId).block();

        // Then
        assertThat(exists).isFalse();
    }
}


