package com.tulka.samvaer.shared.error;

/**
 * Stable error codes shared by all modules. Sections add their own codes with their
 * section key as prefix, for example {@code events.item-full}. The UI translates by code.
 */
public final class ErrorCode {

    public static final String VALIDATION = "common.validation";
    public static final String INVALID_JSON = "common.invalid-json";
    public static final String NOT_FOUND = "common.not-found";
    public static final String FORBIDDEN = "common.forbidden";
    public static final String STALE_VERSION = "common.stale-version";
    public static final String METHOD_NOT_ALLOWED = "common.method-not-allowed";
    public static final String UNSUPPORTED_MEDIA_TYPE = "common.unsupported-media-type";
    public static final String BAD_REQUEST = "common.bad-request";
    public static final String INTERNAL = "common.internal";
    public static final String NOT_LOGGED_IN = "auth.not-logged-in";

    private ErrorCode() {
    }
}
