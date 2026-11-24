package com.tulka.samvaer.sections.events.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.tulka.samvaer.platform.visibility.VisibilityScope;
import com.tulka.samvaer.sections.events.domain.EventKind;
import com.tulka.samvaer.sections.events.domain.Frequency;
import com.tulka.samvaer.sections.events.domain.RsvpStatus;

/** Request bodies of the events API. Fields are nullable; {@link EventInputValidator} checks them. */
public final class Inputs {

    private Inputs() {
    }

    /** Fields shared by create and edit. */
    public interface EventFields {
        EventKind kind();

        String title();

        String description();

        Instant startsAt();

        Instant endsAt();

        Boolean allDay();

        String location();

        VisibilityInput visibility();

        List<ProgrammeItemInput> programme();

        List<SleepingSpotInput> sleeping();
    }

    public record EventInput(
            EventKind kind,
            String title,
            String description,
            Instant startsAt,
            Instant endsAt,
            Boolean allDay,
            String location,
            VisibilityInput visibility,
            List<ProgrammeItemInput> programme,
            List<SleepingSpotInput> sleeping,
            RecurrenceInput recurrence,
            InviteInput invite,
            List<BringItemInput> bringItems) implements EventFields {
    }

    public record EventUpdate(
            EventKind kind,
            String title,
            String description,
            Instant startsAt,
            Instant endsAt,
            Boolean allDay,
            String location,
            VisibilityInput visibility,
            List<ProgrammeItemInput> programme,
            List<SleepingSpotInput> sleeping,
            Long version) implements EventFields {
    }

    public record VisibilityInput(VisibilityScope scope, List<UUID> groupIds) {
    }

    public record ProgrammeItemInput(Instant startsAt, String title) {
    }

    public record SleepingSpotInput(String place, List<UUID> guestIds, String note) {
    }

    public record RecurrenceInput(Frequency frequency, Integer interval, LocalDate until) {
    }

    public record InviteInput(List<UUID> memberIds, List<UUID> groupIds) {
    }

    public record RsvpInput(RsvpStatus status, String note) {
    }

    public record ClaimInput(String note) {
    }

    /**
     * A bring-list item. {@code slots} left out means 1; {@code "slots": null} means anyone who
     * wants to, so the two cases are told apart.
     */
    public static final class BringItemInput {
        private String title;
        private String details;
        private Integer slots = 1;

        public BringItemInput() {
        }

        public BringItemInput(String title, String details, Integer slots) {
            this.title = title;
            this.details = details;
            this.slots = slots;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }

        public Integer getSlots() {
            return slots;
        }

        @JsonSetter("slots")
        public void setSlots(Integer slots) {
            this.slots = slots;
        }
    }
}
