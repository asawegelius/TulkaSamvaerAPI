package com.tulka.samvaer.shared.error;

import java.util.ArrayList;
import java.util.List;

/** Collects field errors and throws one 400 answer naming all of them. */
public final class FieldErrors {

    private final List<FieldError> errors = new ArrayList<>();

    public FieldErrors add(String field, String message) {
        errors.add(new FieldError(field, message));
        return this;
    }

    public FieldErrors check(boolean ok, String field, String message) {
        if (!ok) {
            add(field, message);
        }
        return this;
    }

    public boolean has(String field) {
        return errors.stream().anyMatch(e -> e.field().equals(field));
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public void throwIfAny() {
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
    }
}
