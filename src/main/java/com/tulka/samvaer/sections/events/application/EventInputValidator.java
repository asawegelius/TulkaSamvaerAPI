package com.tulka.samvaer.sections.events.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.tulka.samvaer.platform.identity.MemberDirectory;
import com.tulka.samvaer.platform.visibility.Visibility;
import com.tulka.samvaer.platform.visibility.VisibilityScope;
import com.tulka.samvaer.sections.events.application.Inputs.BringItemInput;
import com.tulka.samvaer.sections.events.application.Inputs.EventFields;
import com.tulka.samvaer.sections.events.application.Inputs.InviteInput;
import com.tulka.samvaer.sections.events.application.Inputs.ProgrammeItemInput;
import com.tulka.samvaer.sections.events.application.Inputs.RecurrenceInput;
import com.tulka.samvaer.sections.events.application.Inputs.SleepingSpotInput;
import com.tulka.samvaer.sections.events.domain.EventDetails;
import com.tulka.samvaer.sections.events.domain.ProgrammeItem;
import com.tulka.samvaer.sections.events.domain.Recurrence;
import com.tulka.samvaer.sections.events.domain.SleepingSpot;
import com.tulka.samvaer.shared.error.FieldErrors;
import org.springframework.stereotype.Component;

/** Checks request bodies and turns them into domain values. Limits follow api/openapi.yaml. */
@Component
class EventInputValidator {

    static final int TITLE_MAX = 120;
    static final int DESCRIPTION_MAX = 4000;
    static final int LOCATION_MAX = 120;
    static final int PROGRAMME_TITLE_MAX = 120;
    static final int PLACE_MAX = 60;
    static final int SPOT_NOTE_MAX = 80;
    static final int ITEM_TITLE_MAX = 80;
    static final int ITEM_DETAILS_MAX = 200;
    static final int CLAIM_NOTE_MAX = 80;
    static final int RSVP_NOTE_MAX = 200;
    static final int INTERVAL_MAX = 99;

    private final MemberDirectory members;

    EventInputValidator(MemberDirectory members) {
        this.members = members;
    }

    /**
     * @param usableGroupIds groups the host may share with: their own groups, plus on an edit the
     *                       groups already on the event
     */
    EventDetails details(EventFields in, Set<UUID> usableGroupIds, FieldErrors errors) {
        String title = trimToNull(in.title());
        errors.check(title != null, "title", "Give the event a name.");
        checkMax(errors, "title", title, TITLE_MAX);
        errors.check(in.kind() != null, "kind", "Unknown kind.");
        String description = trimToNull(in.description());
        checkMax(errors, "description", description, DESCRIPTION_MAX);
        String location = trimToNull(in.location());
        checkMax(errors, "location", location, LOCATION_MAX);

        Instant startsAt = in.startsAt();
        Instant endsAt = in.endsAt();
        errors.check(startsAt != null, "startsAt", "Pick a start.");
        errors.check(endsAt != null, "endsAt", "Pick an end.");
        if (startsAt != null && endsAt != null) {
            errors.check(endsAt.isAfter(startsAt), "endsAt", "The end must be after the start.");
        }

        Visibility visibility = visibility(in, usableGroupIds, errors);
        List<ProgrammeItem> programme = programme(in.programme(), errors);
        List<SleepingSpot> sleeping = sleeping(in.sleeping(), errors);

        return new EventDetails(in.kind(), title, description, startsAt, endsAt,
                Boolean.TRUE.equals(in.allDay()), location, visibility, programme, sleeping);
    }

    private Visibility visibility(EventFields in, Set<UUID> usableGroupIds, FieldErrors errors) {
        if (in.visibility() == null || in.visibility().scope() == null) {
            errors.add("visibility", "Choose who can see it.");
            return new Visibility(VisibilityScope.ONLY_ME, List.of());
        }
        VisibilityScope scope = in.visibility().scope();
        List<UUID> groupIds = distinct(in.visibility().groupIds());
        if (scope == VisibilityScope.GROUPS) {
            if (groupIds.isEmpty()) {
                errors.add("visibility.groupIds", "Pick at least one group.");
            } else if (!usableGroupIds.containsAll(groupIds)) {
                errors.add("visibility.groupIds", "You can only share with your own groups.");
            }
        }
        return new Visibility(scope, groupIds);
    }

    private List<ProgrammeItem> programme(List<ProgrammeItemInput> items, FieldErrors errors) {
        List<ProgrammeItem> programme = new ArrayList<>();
        List<ProgrammeItemInput> list = items == null ? List.of() : items;
        for (int i = 0; i < list.size(); i++) {
            ProgrammeItemInput item = list.get(i);
            String path = "programme." + i;
            String title = item == null ? null : trimToNull(item.title());
            errors.check(title != null, path + ".title", "What happens?");
            checkMax(errors, path + ".title", title, PROGRAMME_TITLE_MAX);
            errors.check(item != null && item.startsAt() != null, path + ".startsAt", "Pick a time.");
            if (title != null && item.startsAt() != null) {
                programme.add(new ProgrammeItem(item.startsAt(), title));
            }
        }
        return programme;
    }

    private List<SleepingSpot> sleeping(List<SleepingSpotInput> spots, FieldErrors errors) {
        List<SleepingSpot> sleeping = new ArrayList<>();
        List<SleepingSpotInput> list = spots == null ? List.of() : spots;
        for (int i = 0; i < list.size(); i++) {
            SleepingSpotInput spot = list.get(i);
            String path = "sleeping." + i;
            String place = spot == null ? null : trimToNull(spot.place());
            errors.check(place != null, path + ".place", "Where do they sleep?");
            checkMax(errors, path + ".place", place, PLACE_MAX);
            String note = spot == null ? null : trimToNull(spot.note());
            checkMax(errors, path + ".note", note, SPOT_NOTE_MAX);
            List<UUID> guests = spot == null ? List.of() : distinct(spot.guestIds());
            errors.check(members.existing(guests).containsAll(guests), path + ".guestIds", "Unknown member.");
            if (place != null) {
                sleeping.add(new SleepingSpot(place, guests, note));
            }
        }
        return sleeping;
    }

    Recurrence recurrence(RecurrenceInput in, Instant startsAt, java.time.ZoneId zone, FieldErrors errors) {
        if (in == null) {
            return null;
        }
        errors.check(in.frequency() != null, "recurrence.frequency", "Choose how often.");
        int interval = in.interval() == null ? 1 : in.interval();
        errors.check(interval >= 1 && interval <= INTERVAL_MAX, "recurrence.interval",
                "Repeat every 1 to " + INTERVAL_MAX + ".");
        if (in.until() != null && startsAt != null) {
            errors.check(!in.until().isBefore(startsAt.atZone(zone).toLocalDate()), "recurrence.until",
                    "The last date must be on or after the start.");
        }
        return new Recurrence(in.frequency(), interval, in.until());
    }

    /** Invitees: named members plus members of named groups. Groups must be the host's own. */
    Set<UUID> invitees(InviteInput in, String path, Set<UUID> usableGroupIds,
            com.tulka.samvaer.platform.groups.GroupDirectory groups, FieldErrors errors) {
        if (in == null) {
            return Set.of();
        }
        List<UUID> memberIds = distinct(in.memberIds());
        List<UUID> groupIds = distinct(in.groupIds());
        errors.check(members.existing(memberIds).containsAll(memberIds), prefix(path) + "memberIds",
                "Unknown member.");
        errors.check(usableGroupIds.containsAll(groupIds), prefix(path) + "groupIds",
                "You can only invite your own groups.");
        Set<UUID> result = new LinkedHashSet<>(memberIds);
        if (!groupIds.isEmpty()) {
            result.addAll(groups.memberIdsOf(groupIds));
        }
        return result;
    }

    /** Bring items given when creating an event. Rows with no title are skipped, like empty form rows. */
    List<BringItemInput> bringItems(List<BringItemInput> items, FieldErrors errors) {
        List<BringItemInput> result = new ArrayList<>();
        List<BringItemInput> list = items == null ? List.of() : items;
        for (int i = 0; i < list.size(); i++) {
            BringItemInput item = list.get(i);
            if (item == null || trimToNull(item.getTitle()) == null) {
                continue;
            }
            result.add(bringItem(item, "bringItems." + i + ".", errors));
        }
        return result;
    }

    BringItemInput bringItem(BringItemInput in, String prefix, FieldErrors errors) {
        String title = in == null ? null : trimToNull(in.getTitle());
        errors.check(title != null, prefix + "title", "What should someone bring?");
        checkMax(errors, prefix + "title", title, ITEM_TITLE_MAX);
        String details = in == null ? null : trimToNull(in.getDetails());
        checkMax(errors, prefix + "details", details, ITEM_DETAILS_MAX);
        Integer slots = in == null ? Integer.valueOf(1) : in.getSlots();
        errors.check(slots == null || slots >= 1, prefix + "slots", "At least one person.");
        return new BringItemInput(title, details, slots);
    }

    String note(String note, String field, int max, FieldErrors errors) {
        String trimmed = trimToNull(note);
        checkMax(errors, field, trimmed, max);
        return trimmed;
    }

    private static String prefix(String path) {
        return path == null || path.isEmpty() ? "" : path + ".";
    }

    private static void checkMax(FieldErrors errors, String field, String value, int max) {
        if (value != null && value.length() > max && !errors.has(field)) {
            errors.add(field, "At most " + max + " characters.");
        }
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<UUID> distinct(Collection<UUID> ids) {
        return ids == null ? List.of() : ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }
}
