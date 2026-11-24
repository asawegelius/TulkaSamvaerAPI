package com.tulka.samvaer.shared.error;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * An expected failure that becomes an RFC 9457 problem details answer with a stable {@code code}.
 * Modules throw it from their application layer; {@link GlobalExceptionHandler} renders it.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String title;
    private final String detail;
    private final List<FieldError> fieldErrors;

    public ApiException(HttpStatus status, String code, String title, String detail, List<FieldError> fieldErrors) {
        super(code + ": " + title);
        this.status = status;
        this.code = code;
        this.title = title;
        this.detail = detail;
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public ApiException(HttpStatus status, String code, String title, String detail) {
        this(status, code, title, detail, List.of());
    }

    /** The item does not exist or the caller may not see it. Never reveals which. */
    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Not found", null);
    }

    /** The caller may see the item but not do this. */
    public static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Not allowed", null);
    }

    public static ApiException validation(List<FieldError> fieldErrors) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION, "Invalid input",
                "Some fields need attention.", fieldErrors);
    }

    public static ApiException staleVersion() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.STALE_VERSION, "Changed by someone else",
                "Reload and try again.");
    }

    public static ApiException notLoggedIn() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.NOT_LOGGED_IN, "Not signed in", null);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String detail() {
        return detail;
    }

    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }
}
