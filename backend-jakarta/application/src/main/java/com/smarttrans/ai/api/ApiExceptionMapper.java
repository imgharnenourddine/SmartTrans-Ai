package com.smarttrans.ai.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.smarttrans.ai.api.ApiExceptions.Business;
import com.smarttrans.ai.api.ApiExceptions.Conflict;
import com.smarttrans.ai.api.ApiExceptions.Forbidden;
import com.smarttrans.ai.api.ApiExceptions.InvalidBody;
import com.smarttrans.ai.api.ApiExceptions.NotFound;
import com.smarttrans.ai.api.ApiExceptions.Unauthorized;
import com.smarttrans.ai.api.ApiExceptions.ValidationFailed;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Provider
@Priority(Priorities.USER)
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    private HttpServletRequest request;

    @Override
    public Response toResponse(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ValidationFailed validation) {
                return error(400, "Bad Request", validation.getMessage(), validation.fieldErrors());
            }
            if (current instanceof ConstraintViolationException violations) {
                return error(400, "Bad Request", "Validation failed", fieldErrors(violations));
            }
            if (current instanceof ValidationException validation) {
                ConstraintViolationException violations = violationsOf(validation);
                if (violations != null) {
                    return error(400, "Bad Request", "Validation failed", fieldErrors(violations));
                }
            }
            if (current instanceof InvalidBody invalidBody) {
                return error(400, "Bad Request", invalidBody.getMessage(), null);
            }
            if (current instanceof JsonProcessingException json) {
                return error(400, "Bad Request", jsonMessage(json), null);
            }
            if (current instanceof Unauthorized unauthorized) {
                return error(401, "Unauthorized", unauthorized.getMessage(), null);
            }
            if (current instanceof Forbidden forbidden) {
                return error(403, "Forbidden", forbidden.getMessage(), null);
            }
            if (current instanceof NotFound notFound) {
                return error(404, "Not Found", notFound.getMessage(), null);
            }
            if (current instanceof Conflict conflict) {
                return error(409, "Conflict", conflict.getMessage(), null);
            }
            if (current instanceof Business business) {
                return error(422, "Unprocessable Entity", business.getMessage(), null);
            }
            current = current.getCause();
        }
        if (exception instanceof WebApplicationException web) {
            return web.getResponse();
        }
        return error(500, "Internal Server Error", "Unexpected server error", null);
    }

    private ConstraintViolationException violationsOf(ValidationException exception) {
        if (exception instanceof ConstraintViolationException violations) {
            return violations;
        }
        try {
            Object value = exception.getClass().getMethod("getConstraintViolations").invoke(exception);
            if (value instanceof Set<?> violations && !violations.isEmpty()) {
                @SuppressWarnings("unchecked")
                Set<ConstraintViolation<?>> typed = (Set<ConstraintViolation<?>>) value;
                return new ConstraintViolationException(typed);
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    private Map<String, String> fieldErrors(ConstraintViolationException violations) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : violations.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            int dot = path.lastIndexOf('.');
            String field = dot >= 0 ? path.substring(dot + 1) : path;
            fieldErrors.put(field, violation.getMessage());
        }
        return fieldErrors;
    }

    private String jsonMessage(JsonProcessingException exception) {
        String detail = exception.getOriginalMessage();
        if (detail != null && detail.toLowerCase().contains("uuid")) {
            return "Invalid UUID format";
        }
        return "Invalid request body";
    }

    private Response error(int status, String error, String message, Map<String, String> fieldErrors) {
        String path = request == null ? "" : request.getRequestURI();
        ApiError body = new ApiError(Instant.now(), status, error, message, path, fieldErrors);
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    public record ApiError(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path,
            Map<String, String> fieldErrors) {
    }
}
