package com.tulka.samvaer.shared.error;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.tulka.samvaer.shared.logging.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every failure into RFC 9457 problem details with a stable {@code code}, as the API
 * contract requires: {@code { type, title, status, detail, code, fieldErrors, correlationId }}.
 * Raw exception text never reaches the client. The code is also written to the log via MDC.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String ERROR_CODE_MDC_KEY = "error_code";

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException e) {
        return problem(e.status(), e.code(), e.title(), e.detail(), e.fieldErrors(), e);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        ApiException stale = ApiException.staleVersion();
        return problem(stale.status(), stale.code(), stale.title(), stale.detail(), List.of(), e);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        boolean pathVariable = e.getParameter().hasParameterAnnotation(
                org.springframework.web.bind.annotation.PathVariable.class);
        if (pathVariable) {
            // A malformed id cannot name an existing item.
            return problem(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Not found", null, List.of(), e);
        }
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION, "Invalid input", "Some fields need attention.",
                List.of(new FieldError(e.getName(), "Invalid value.")), e);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Bad request", null, List.of(), e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception e) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL, "Internal server error", null,
                List.of(), e);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION, "Invalid input", "Some fields need attention.",
                fieldErrors, ex);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.getCause() instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            String field = mapping.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : String.valueOf(ref.getIndex()))
                    .collect(Collectors.joining("."));
            return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION, "Invalid input",
                    "Some fields need attention.", List.of(new FieldError(field, "Invalid value.")), ex);
        }
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_JSON, "Invalid JSON", null, List.of(), ex);
    }

    /** Spring MVC's own exceptions (405, 415, 404 for unknown paths, ...) get a code too. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String code = switch (status) {
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> ErrorCode.METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case BAD_REQUEST -> ErrorCode.BAD_REQUEST;
            default -> status.is5xxServerError() ? ErrorCode.INTERNAL : "common." + status.name().toLowerCase().replace('_', '-');
        };
        return problem(status, code, status.getReasonPhrase(), null, List.of(), ex, headers);
    }

    private ResponseEntity<Object> problem(HttpStatus status, String code, String title, String detail,
            List<FieldError> fieldErrors, Exception exception) {
        return problem(status, code, title, detail, fieldErrors, exception, new HttpHeaders());
    }

    private ResponseEntity<Object> problem(HttpStatus status, String code, String title, String detail,
            List<FieldError> fieldErrors, Exception exception, HttpHeaders headers) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        String previousErrorCode = MDC.get(ERROR_CODE_MDC_KEY);
        MDC.put(ERROR_CODE_MDC_KEY, code);
        try {
            if (status.is5xxServerError()) {
                LOGGER.error("{}: {}", code, exception.getMessage(), exception);
            } else {
                LOGGER.warn("{}: {}", code, exception.getMessage());
            }

            ProblemDetail body = ProblemDetail.forStatus(status);
            body.setType(URI.create("about:blank"));
            body.setTitle(title);
            body.setDetail(detail);
            body.setProperty("code", code);
            if (!fieldErrors.isEmpty()) {
                body.setProperty("fieldErrors", fieldErrors);
            }
            if (correlationId != null) {
                body.setProperty("correlationId", correlationId);
            }
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.putAll(headers);
            responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
            return new ResponseEntity<>(body, responseHeaders, status);
        }
        finally {
            if (previousErrorCode == null) {
                MDC.remove(ERROR_CODE_MDC_KEY);
            } else {
                MDC.put(ERROR_CODE_MDC_KEY, previousErrorCode);
            }
        }
    }
}
