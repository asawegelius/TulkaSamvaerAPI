package com.tulka.samvaer.sections.events.domain;

import java.time.Instant;
import java.util.List;

import com.tulka.samvaer.platform.visibility.Visibility;

/** The host-editable fields of an event, already validated and normalised. */
public record EventDetails(
        EventKind kind,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        boolean allDay,
        String location,
        Visibility visibility,
        List<ProgrammeItem> programme,
        List<SleepingSpot> sleeping) {
}
