package com.tulka.samvaer.sections.events.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record ProgrammeItem(
        @Column(name = "starts_at", nullable = false) Instant startsAt,
        @Column(nullable = false) String title) {
}
