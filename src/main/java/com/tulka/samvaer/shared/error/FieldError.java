package com.tulka.samvaer.shared.error;

/** One invalid field in a 400 answer. {@code field} is a dotted path such as {@code visibility.groupIds}. */
public record FieldError(String field, String message) {
}
