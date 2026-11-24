package com.tulka.samvaer.sections.events.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A repeating event. Each occurrence is its own {@link Event} with its own answers and bring-list. */
@Entity
@Table(name = "events_series")
public class Series {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(name = "interval_count", nullable = false)
    private int interval;

    @Column(name = "until_date")
    private LocalDate until;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Series() {
    }

    public Series(Frequency frequency, int interval, LocalDate until, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.frequency = frequency;
        this.interval = interval;
        this.until = until;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public int getInterval() {
        return interval;
    }

    public LocalDate getUntil() {
        return until;
    }
}
