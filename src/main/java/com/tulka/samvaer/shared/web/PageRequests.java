package com.tulka.samvaer.shared.web;

import com.tulka.samvaer.shared.error.FieldErrors;

/** Validates the shared {@code page} and {@code size} query parameters. */
public final class PageRequests {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    public static void validate(int page, int size, FieldErrors errors) {
        errors.check(page >= 0, "page", "Page must be 0 or more.");
        errors.check(size >= 1 && size <= MAX_SIZE, "size", "Size must be between 1 and " + MAX_SIZE + ".");
    }
}
