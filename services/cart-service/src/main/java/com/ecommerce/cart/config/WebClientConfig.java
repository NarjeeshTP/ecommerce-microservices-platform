package com.ecommerce.cart.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${services.catalog.url}")
    private String catalogServiceUrl;

    @Value("${services.pricing.url}")
    private String pricingServiceUrl;

    @Value("${services.catalog.timeout:5000}")
    private int catalogTimeout;

    @Value("${services.pricing.timeout:3000}")
    private int pricingTimeout;

    @Value("${webclient.max-connections:100}")
    private int maxConnections;

    @Value("${webclient.pending-acquire-timeout:45000}")
    private int pendingAcquireTimeout;

    @Value("${webclient.max-idle-time:20000}")
    private int maxIdleTime;

    private ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("custom")
                .maxConnections(maxConnections)
                .pendingAcquireTimeout(Duration.ofMillis(pendingAcquireTimeout))
                .maxIdleTime(Duration.ofMillis(maxIdleTime))
                .build();
    }

    private HttpClient createHttpClient(int timeoutMs) {
        return HttpClient.create(connectionProvider())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
                );
    }

    @Bean
    public WebClient catalogWebClient(WebClient.Builder builder) {
        HttpClient httpClient = createHttpClient(catalogTimeout);
        return builder
                .baseUrl(catalogServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public WebClient pricingWebClient(WebClient.Builder builder) {
        HttpClient httpClient = createHttpClient(pricingTimeout);
        return builder
                .baseUrl(pricingServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

