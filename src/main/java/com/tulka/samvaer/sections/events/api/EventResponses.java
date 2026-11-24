package com.tulka.samvaer.sections.events.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tulka.samvaer.platform.identity.MemberRef;
import com.tulka.samvaer.platform.visibility.Visibility;
import com.tulka.samvaer.sections.events.domain.EventKind;
import com.tulka.samvaer.sections.events.domain.Frequency;
import com.tulka.samvaer.sections.events.domain.RsvpStatus;

/** Response bodies, shaped exactly as the schemas in api/openapi.yaml. */
final class EventResponses {

    private EventResponses() {
    }

    record RsvpSummary(int going, int maybe, int cant, int pending) {
    }

    record BringListCounts(int total, int taken) {
    }

    record RecurrenceView(Frequency frequency, int interval, LocalDate until) {
    }

    record SeriesInfo(UUID id, RecurrenceView recurrence) {
    }

    record EventSummary(
            UUID id,
            EventKind kind,
            String title,
            Instant startsAt,
            Instant endsAt,
            boolean allDay,
            String location,
            MemberRef host,
            Visibility visibility,
            String audienceLabel,
            SeriesInfo series,
            RsvpStatus myRsvp,
            RsvpSummary rsvpSummary,
            BringListCounts bringList,
            List<String> allowedActions) {
    }

    record Attendee(MemberRef member, RsvpStatus status, String note) {
    }

    record Claim(MemberRef member, String note) {
    }

    record BringItem(UUID id, String title, String details, Integer slots, List<Claim> claimedBy,
            List<String> allowedActions) {
    }

    record ProgrammeItem(Instant startsAt, String title) {
    }

    record SleepingSpot(String place, List<MemberRef> guests, String note) {
    }

    /** {@link EventSummary} fields plus everything on the event page. */
    record EventDetail(
            @JsonUnwrapped EventSummary summary,
            String description,
            List<Attendee> attendees,
            List<BringItem> bringItems,
            List<ProgrammeItem> programme,
            List<SleepingSpot> sleeping,
            long version) {
    }
}
