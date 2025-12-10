package com.ecommerce.cart.client;

import com.ecommerce.cart.dto.external.PriceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

@Component
@Slf4j
public class PricingServiceClient {

    private final WebClient pricingWebClient;

    public PricingServiceClient(@Qualifier("pricingWebClient") WebClient pricingWebClient) {
        this.pricingWebClient = pricingWebClient;
    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2),
            retryFor = {WebClientResponseException.ServiceUnavailable.class}
    )
    public Mono<PriceDTO> getPrice(String productId) {
        log.debug("Fetching price for product {} from pricing service", productId);

        return pricingWebClient
                .get()
                .uri("/pricing/price/{productId}", productId)
                .retrieve()
                .bodyToMono(PriceDTO.class)
                .timeout(Duration.ofSeconds(3))
                .doOnSuccess(price -> log.debug("Successfully fetched price for product {}: {}",
                        productId, price != null ? price.getFinalPrice() : null))
                .doOnError(error -> log.error("Error fetching price for {}: {}", productId, error.getMessage()))
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn("Price not found for product {}", productId);
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Failed to fetch price for {} from pricing service", productId, e);
                    return Mono.empty();
                });
    }

    public Mono<BigDecimal> getFinalPrice(String productId) {
        return getPrice(productId)
                .map(PriceDTO::getFinalPrice)
                .doOnSuccess(price -> log.debug("Final price for {}: {}", productId, price));
    }

    public PriceDTO getPriceBlocking(String productId) {
        return getPrice(productId).block();
    }

    public BigDecimal getFinalPriceBlocking(String productId) {
        return getFinalPrice(productId).block();
    }
}

