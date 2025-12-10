package com.ecommerce.cart.client;

import com.ecommerce.cart.dto.external.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
public class CatalogServiceClient {

    private final WebClient catalogWebClient;

    public CatalogServiceClient(@Qualifier("catalogWebClient") WebClient catalogWebClient) {
        this.catalogWebClient = catalogWebClient;
    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryFor = {WebClientResponseException.ServiceUnavailable.class}
    )
    public Mono<ProductDTO> getProduct(String productId) {
        log.debug("Fetching product {} from catalog service", productId);

        return catalogWebClient
                .get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .bodyToMono(ProductDTO.class)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(product -> log.debug("Successfully fetched product: {}", productId))
                .doOnError(error -> log.error("Error fetching product {}: {}", productId, error.getMessage()))
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn("Product {} not found in catalog", productId);
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Failed to fetch product {} from catalog service", productId, e);
                    return Mono.empty();
                });
    }

    public Mono<Boolean> productExists(String productId) {
        log.debug("Checking if product {} exists", productId);

        return catalogWebClient
                .get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .timeout(Duration.ofSeconds(3))
                .doOnSuccess(exists -> log.debug("Product {} exists: {}", productId, exists))
                .onErrorReturn(false);
    }

    public ProductDTO getProductBlocking(String productId) {
        return getProduct(productId).block();
    }
}

