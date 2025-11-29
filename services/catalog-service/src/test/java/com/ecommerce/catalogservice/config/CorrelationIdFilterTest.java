package com.ecommerce.catalogservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldGenerateCorrelationIdWhenNotProvided() throws Exception {
        // When: No correlation ID header is provided
        filter.doFilter(request, response, filterChain);

        // Then: A correlation ID should be generated and added to response
        String correlationId = response.getHeader("X-Correlation-ID");
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$"); // UUID format
    }

    @Test
    void shouldUseProvidedCorrelationId() throws Exception {
        // Given: A correlation ID is provided in the request
        String providedId = "test-correlation-id-123";
        request.addHeader("X-Correlation-ID", providedId);

        // When: Filter processes the request
        filter.doFilter(request, response, filterChain);

        // Then: The provided correlation ID should be used
        String correlationId = response.getHeader("X-Correlation-ID");
        assertThat(correlationId).isEqualTo(providedId);
    }

    @Test
    void shouldSetMDCForLogging() throws Exception {
        // Given: A correlation ID is provided
        String testId = "mdc-test-id";
        request.addHeader("X-Correlation-ID", testId);

        // When: Filter processes the request
        filterChain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                // Then: MDC should be set during request processing
                assertThat(MDC.get("correlationId")).isEqualTo(testId);
            }
        };

        filter.doFilter(request, response, filterChain);
    }

    @Test
    void shouldClearMDCAfterRequest() throws Exception {
        // Given: A correlation ID is set
        request.addHeader("X-Correlation-ID", "clear-test-id");

        // When: Filter processes the request
        filter.doFilter(request, response, filterChain);

        // Then: MDC should be cleared after request to prevent memory leaks
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void shouldHandleBlankCorrelationId() throws Exception {
        // Given: A blank correlation ID is provided
        request.addHeader("X-Correlation-ID", "   ");

        // When: Filter processes the request
        filter.doFilter(request, response, filterChain);

        // Then: A new correlation ID should be generated
        String correlationId = response.getHeader("X-Correlation-ID");
        assertThat(correlationId).isNotBlank();
        assertThat(correlationId).matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");
    }

    @Test
    void shouldHandleEmptyCorrelationId() throws Exception {
        // Given: An empty correlation ID is provided
        request.addHeader("X-Correlation-ID", "");

        // When: Filter processes the request
        filter.doFilter(request, response, filterChain);

        // Then: A new correlation ID should be generated
        String correlationId = response.getHeader("X-Correlation-ID");
        assertThat(correlationId).isNotBlank();
        assertThat(correlationId).matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");
    }

    @Test
    void shouldPreserveMDCThroughoutRequestChain() throws Exception {
        // Given: A correlation ID is provided
        String testId = "chain-test-id";
        request.addHeader("X-Correlation-ID", testId);

        // Track if MDC was available during chain execution
        final boolean[] mdcWasSet = {false};

        filterChain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                // Verify MDC is available to the entire request chain
                if (testId.equals(MDC.get("correlationId"))) {
                    mdcWasSet[0] = true;
                }
            }
        };

        // When: Filter processes the request
        filter.doFilter(request, response, filterChain);

        // Then: MDC should have been set during the chain execution
        assertThat(mdcWasSet[0]).isTrue();
    }

    @Test
    void shouldGenerateUniqueIdsForDifferentRequests() throws Exception {
        // First request
        filter.doFilter(request, response, filterChain);
        String firstId = response.getHeader("X-Correlation-ID");

        // Second request with new objects
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        MockFilterChain filterChain2 = new MockFilterChain();

        filter.doFilter(request2, response2, filterChain2);
        String secondId = response2.getHeader("X-Correlation-ID");

        // Then: Each request should have a unique correlation ID
        assertThat(firstId).isNotEqualTo(secondId);
    }
}

