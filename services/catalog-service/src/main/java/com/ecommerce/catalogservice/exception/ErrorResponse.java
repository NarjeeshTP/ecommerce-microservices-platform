package com.ecommerce.catalogservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized error response structure for all API errors.
 * Provides consistent error format across the application.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> fieldErrors;
    private String correlationId;

    // Builder pattern for flexible object creation
    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }

    // Constructors
    public ErrorResponse() {
    }

    public ErrorResponse(Instant timestamp, int status, String error, String message,
                        String path, Map<String, String> fieldErrors, String correlationId) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
        this.correlationId = correlationId;
    }

    // Getters and Setters
    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    // Builder class
    public static class ErrorResponseBuilder {
        private Instant timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> fieldErrors;
        private String correlationId;

        ErrorResponseBuilder() {
        }

        public ErrorResponseBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        public ErrorResponseBuilder error(String error) {
            this.error = error;
            return this;
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ErrorResponseBuilder fieldErrors(Map<String, String> fieldErrors) {
            this.fieldErrors = fieldErrors;
            return this;
        }

        public ErrorResponseBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(timestamp, status, error, message, path, fieldErrors, correlationId);
        }

        @Override
        public String toString() {
            return "ErrorResponse.ErrorResponseBuilder(timestamp=" + this.timestamp + ", status=" + this.status
                    + ", error=" + this.error + ", message=" + this.message + ", path=" + this.path
                    + ", fieldErrors=" + this.fieldErrors + ", correlationId=" + this.correlationId + ")";
        }
    }
}

