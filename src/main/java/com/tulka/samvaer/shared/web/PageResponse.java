package com.tulka.samvaer.shared.web;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** The paged list shape every list endpoint answers with: {@code { items, page, size, totalItems }}. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems) {

    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements());
    }
}
