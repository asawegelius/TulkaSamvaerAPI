package com.tulka.samvaer.sections.events.api;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tulka.samvaer.platform.groups.GroupDirectory;
import com.tulka.samvaer.platform.identity.MemberDirectory;
import com.tulka.samvaer.platform.identity.MemberRef;
import com.tulka.samvaer.sections.events.api.EventResponses.Attendee;
import com.tulka.samvaer.sections.events.api.EventResponses.BringListCounts;
import com.tulka.samvaer.sections.events.api.EventResponses.EventDetail;
import com.tulka.samvaer.sections.events.api.EventResponses.EventSummary;
import com.tulka.samvaer.sections.events.api.EventResponses.RecurrenceView;
import com.tulka.samvaer.sections.events.api.EventResponses.RsvpSummary;
import com.tulka.samvaer.sections.events.api.EventResponses.SeriesInfo;
import com.tulka.samvaer.sections.events.domain.Answer;
import com.tulka.samvaer.sections.events.domain.BringItem;
import com.tulka.samvaer.sections.events.domain.Claim;
import com.tulka.samvaer.sections.events.domain.Event;
import com.tulka.samvaer.sections.events.domain.EventRules;
import com.tulka.samvaer.sections.events.domain.ProgrammeItem;
import com.tulka.samvaer.sections.events.domain.RsvpStatus;
import com.tulka.samvaer.sections.events.domain.Series;
import com.tulka.samvaer.sections.events.domain.SleepingSpot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Builds responses for one viewer: their answer, their allowed actions, names for every member id. */
@Component
class EventViewAssembler {

    private final MemberDirectory members;
    private final GroupDirectory groups;

    EventViewAssembler(MemberDirectory members, GroupDirectory groups) {
        this.members = members;
        this.groups = groups;
    }

    @Transactional(readOnly = true)
    List<EventSummary> summaries(List<Event> events, UUID viewer) {
        Map<UUID, MemberRef> refs = members.refsOf(events.stream().map(Event::getHostId).toList());
        Map<UUID, String> groupNames = groups.nameMap(events.stream()
                .flatMap(e -> e.getVisibility().groupIds().stream()).collect(Collectors.toSet()));
        return events.stream().map(e -> summary(e, viewer, refs, groupNames)).toList();
    }

    EventDetail detail(Event event, UUID viewer) {
        Map<UUID, MemberRef> refs = members.refsOf(memberIds(event));
        Map<UUID, String> groupNames = groups.nameMap(event.getVisibility().groupIds());
        Comparator<Answer> guestOrder = Comparator.comparing(Answer::getStatus)
                .thenComparing(a -> refs.get(a.getMemberId()).displayName(), String.CASE_INSENSITIVE_ORDER);
        return new EventDetail(
                summary(event, viewer, refs, groupNames),
                event.getDescription(),
                event.getAnswers().stream().sorted(guestOrder)
                        .map(a -> new Attendee(refs.get(a.getMemberId()), a.getStatus(), a.getNote()))
                        .toList(),
                event.getBringItems().stream().map(i -> bringItem(event, i, viewer, refs)).toList(),
                event.getProgramme().stream().sorted(Comparator.comparing(ProgrammeItem::startsAt))
                        .map(p -> new EventResponses.ProgrammeItem(p.startsAt(), p.title()))
                        .toList(),
                event.getSleeping().stream().map(s -> sleepingSpot(s, refs)).toList(),
                event.getVersion());
    }

    private EventSummary summary(Event e, UUID viewer, Map<UUID, MemberRef> refs, Map<UUID, String> groupNames) {
        int taken = (int) e.getBringItems().stream().filter(i -> !i.getClaims().isEmpty()).count();
        return new EventSummary(
                e.getId(),
                e.getKind(),
                e.getTitle(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.isAllDay(),
                e.getLocation(),
                refs.getOrDefault(e.getHostId(), MemberRef.former(e.getHostId())),
                e.getVisibility(),
                audienceLabel(e, groupNames),
                seriesInfo(e.getSeries()),
                e.statusOf(viewer),
                new RsvpSummary(count(e, RsvpStatus.GOING), count(e, RsvpStatus.MAYBE), count(e, RsvpStatus.CANT),
                        count(e, RsvpStatus.PENDING)),
                new BringListCounts(e.getBringItems().size(), taken),
                EventRules.eventActions(e, viewer));
    }

    private static String audienceLabel(Event e, Map<UUID, String> groupNames) {
        return switch (e.getVisibilityScope()) {
            case ONLY_ME -> "Only me";
            case ALL_MEMBERS -> "All members";
            case GROUPS -> e.getVisibility().groupIds().stream()
                    .map(groupNames::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.joining(", "));
        };
    }

    private static SeriesInfo seriesInfo(Series series) {
        if (series == null) {
            return null;
        }
        return new SeriesInfo(series.getId(),
                new RecurrenceView(series.getFrequency(), series.getInterval(), series.getUntil()));
    }

    private static int count(Event e, RsvpStatus status) {
        return (int) e.getAnswers().stream().filter(a -> a.getStatus() == status).count();
    }

    private static EventResponses.BringItem bringItem(Event event, BringItem item, UUID viewer,
            Map<UUID, MemberRef> refs) {
        return new EventResponses.BringItem(item.getId(), item.getTitle(), item.getDetails(), item.getSlots(),
                item.getClaims().stream().map(c -> claim(c, refs)).toList(),
                EventRules.itemActions(event, item, viewer));
    }

    private static EventResponses.Claim claim(Claim claim, Map<UUID, MemberRef> refs) {
        return new EventResponses.Claim(refs.get(claim.getMemberId()), claim.getNote());
    }

    private static EventResponses.SleepingSpot sleepingSpot(SleepingSpot spot, Map<UUID, MemberRef> refs) {
        return new EventResponses.SleepingSpot(spot.getPlace(),
                spot.getGuestIds().stream().map(refs::get).toList(), spot.getNote());
    }

    private static Collection<UUID> memberIds(Event e) {
        Set<UUID> ids = new HashSet<>();
        ids.add(e.getHostId());
        e.getAnswers().forEach(a -> ids.add(a.getMemberId()));
        e.getBringItems().forEach(i -> i.getClaims().forEach(c -> ids.add(c.getMemberId())));
        e.getSleeping().forEach(s -> ids.addAll(s.getGuestIds()));
        return ids;
    }
}
