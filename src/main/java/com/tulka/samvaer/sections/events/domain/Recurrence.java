package com.tulka.samvaer.sections.events.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * How a series repeats. Occurrences keep the first occurrence's local wall-clock time in the
 * home zone, so a 18:00 event stays at 18:00 across daylight saving changes. Each step is
 * counted from the first occurrence, so a series on 31 January falls on the last day of shorter
 * months instead of drifting.
 */
public record Recurrence(Frequency frequency, int interval, LocalDate until) {

    /** Occurrences created up front for a new series. */
    public static final int MAX_OCCURRENCES = 24;

    /**
     * The steps (0 = the first occurrence) to create for a series starting at {@code firstStart}:
     * at most {@link #MAX_OCCURRENCES}, none starting after {@code horizon} or after {@code until}.
     */
    public List<Integer> steps(Instant firstStart, ZoneId zone, Instant horizon) {
        List<Integer> steps = new ArrayList<>();
        for (int step = 0; steps.size() < MAX_OCCURRENCES; step++) {
            ZonedDateTime start = shift(firstStart, step, zone).atZone(zone);
            if (until != null && start.toLocalDate().isAfter(until)) {
                break;
            }
            if (step > 0 && start.toInstant().isAfter(horizon)) {
                break;
            }
            steps.add(step);
        }
        return steps;
    }

    /** {@code instant} moved {@code step} repetitions forward, keeping its local time in {@code zone}. */
    public Instant shift(Instant instant, int step, ZoneId zone) {
        ZonedDateTime local = instant.atZone(zone);
        long amount = (long) step * interval;
        ZonedDateTime shifted = switch (frequency) {
            case WEEKLY -> local.plusWeeks(amount);
            case MONTHLY -> local.plusMonths(amount);
            case YEARLY -> local.plusYears(amount);
        };
        return shifted.toInstant();
    }
}
