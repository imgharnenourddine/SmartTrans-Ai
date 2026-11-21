package com.smarttrans.ai.api;

import java.util.Map;

public final class ApiExceptions {

    private ApiExceptions() {
    }

    public static final class NotFound extends RuntimeException {
        public NotFound(String message) {
            super(message);
        }
    }

    public static final class Conflict extends RuntimeException {
        public Conflict(String message) {
            super(message);
        }
    }

    public static final class Business extends RuntimeException {
        public Business(String message) {
            super(message);
        }
    }

    public static final class Unauthorized extends RuntimeException {
        public Unauthorized(String message) {
            super(message);
        }
    }

    public static final class Forbidden extends RuntimeException {
        public Forbidden() {
            super("Access denied");
        }
    }

    public static final class InvalidBody extends RuntimeException {
        public InvalidBody(String message) {
            super(message);
        }
    }

    public static final class ValidationFailed extends RuntimeException {
        private final Map<String, String> fieldErrors;

        public ValidationFailed(Map<String, String> fieldErrors) {
            super("Validation failed");
            this.fieldErrors = fieldErrors;
        }

        public Map<String, String> fieldErrors() {
            return fieldErrors;
        }
    }
}
