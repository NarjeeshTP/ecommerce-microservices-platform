package com.ecommerce.catalogservice.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to handle correlation IDs for distributed tracing across microservices.
 *
 * This filter:
 * - Extracts correlation ID from X-Correlation-ID header if present
 * - Generates a new UUID if not provided
 * - Stores it in MDC for automatic inclusion in all logs
 * - Adds it to response headers so clients can reference it
 * - Ensures MDC is cleaned up to prevent memory leaks
 *
 * @see <a href="docs/CORRELATION_ID_GUIDE.md">Correlation ID Guide</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Get or generate correlation ID
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            // Set in MDC for logging (will be included in all log statements)
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // Add to response headers so client can reference it (e.g., for support tickets)
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue with the request
            chain.doFilter(request, response);
        } finally {
            // Critical: Clean up MDC to prevent memory leaks in thread pools
            MDC.clear();
        }
    }
}

