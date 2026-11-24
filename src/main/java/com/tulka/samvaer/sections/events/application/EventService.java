package com.tulka.samvaer.sections.events.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.tulka.samvaer.platform.groups.GroupDirectory;
import com.tulka.samvaer.platform.visibility.VisibilityScope;
import com.tulka.samvaer.sections.events.EventCancelled;
import com.tulka.samvaer.sections.events.EventInvitationsSent;
import com.tulka.samvaer.sections.events.application.Inputs.BringItemInput;
import com.tulka.samvaer.sections.events.application.Inputs.ClaimInput;
import com.tulka.samvaer.sections.events.application.Inputs.EventInput;
import com.tulka.samvaer.sections.events.application.Inputs.EventUpdate;
import com.tulka.samvaer.sections.events.application.Inputs.InviteInput;
import com.tulka.samvaer.sections.events.application.Inputs.RsvpInput;
import com.tulka.samvaer.sections.events.domain.Answer;
import com.tulka.samvaer.sections.events.domain.BringItem;
import com.tulka.samvaer.sections.events.domain.Event;
import com.tulka.samvaer.sections.events.domain.EventDetails;
import com.tulka.samvaer.sections.events.domain.EventRules;
import com.tulka.samvaer.sections.events.domain.ProgrammeItem;
import com.tulka.samvaer.sections.events.domain.Recurrence;
import com.tulka.samvaer.sections.events.domain.RsvpStatus;
import com.tulka.samvaer.sections.events.domain.Series;
import com.tulka.samvaer.sections.events.domain.SleepingSpot;
import com.tulka.samvaer.sections.events.infrastructure.BringItemRepository;
import com.tulka.samvaer.sections.events.infrastructure.EventRepository;
import com.tulka.samvaer.sections.events.infrastructure.SeriesRepository;
import com.tulka.samvaer.shared.config.HomeZone;
import com.tulka.samvaer.shared.error.ApiException;
import com.tulka.samvaer.shared.error.FieldErrors;
import com.tulka.samvaer.shared.web.PageRequests;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every events use case. {@code viewer} is the signed-in member. */
@Service
@Transactional
public class EventService {

    /** Series occurrences are created up to this far ahead. */
    static final Period SERIES_HORIZON = Period.ofYears(2);

    /** Stands in for "no groups" so the visibility query never gets an empty IN list. */
    private static final UUID NO_GROUP = new UUID(0, 0);

    private static final Instant END_OF_TIME = Instant.parse("9999-12-31T00:00:00Z");

    private final EventRepository events;
    private final SeriesRepository series;
    private final BringItemRepository bringItems;
    private final GroupDirectory groups;
    private final EventInputValidator validator;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;
    private final ZoneId zone;

    public EventService(EventRepository events, SeriesRepository series, BringItemRepository bringItems,
            GroupDirectory groups, EventInputValidator validator, ApplicationEventPublisher publisher, Clock clock,
            HomeZone homeZone) {
        this.events = events;
        this.series = series;
        this.bringItems = bringItems;
        this.groups = groups;
        this.validator = validator;
        this.publisher = publisher;
        this.clock = clock;
        this.zone = homeZone.zoneId();
    }

    /** Visible events overlapping the calendar dates [from, to], soonest first. {@code from} defaults to today. */
    @Transactional(readOnly = true)
    public Page<Event> list(UUID viewer, LocalDate from, LocalDate to, int page, int size) {
        FieldErrors errors = new FieldErrors();
        PageRequests.validate(page, size, errors);
        LocalDate first = from != null ? from : LocalDate.now(clock.withZone(zone));
        if (to != null) {
            errors.check(!to.isBefore(first), "to", "The last date must be on or after the first.");
        }
        errors.throwIfAny();

        Instant fromInstant = first.atStartOfDay(zone).toInstant();
        Instant toInstant = to == null ? END_OF_TIME : to.plusDays(1).atStartOfDay(zone).toInstant();
        Set<UUID> viewerGroups = new HashSet<>(groups.groupIdsOf(viewer));
        if (viewerGroups.isEmpty()) {
            viewerGroups.add(NO_GROUP);
        }
        return events.findVisible(viewer, viewerGroups, fromInstant, toInstant, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Event get(UUID viewer, UUID eventId) {
        return findVisible(viewer, eventId);
    }

    /**
     * Creates an event with the caller as host (going). With a recurrence, creates the series and
     * its upcoming occurrences, each with its own guest list and bring-list, and returns the first.
     */
    public Event create(UUID viewer, EventInput in) {
        Set<UUID> usableGroups = groups.groupIdsOf(viewer);
        FieldErrors errors = new FieldErrors();
        EventDetails details = validator.details(in, usableGroups, errors);
        Recurrence recurrence = validator.recurrence(in.recurrence(), in.startsAt(), zone, errors);
        Set<UUID> invitees = validator.invitees(in.invite(), "invite", usableGroups, groups, errors);
        if (details.visibility().scope() == VisibilityScope.ONLY_ME && !invitees.isEmpty()) {
            errors.add("invite", "A private event can't have guests. Share it with groups or all members.");
        }
        List<BringItemInput> items = validator.bringItems(in.bringItems(), errors);
        errors.throwIfAny();

        Instant now = clock.instant();
        Set<UUID> audience = audience(details, invitees, viewer);

        if (recurrence == null) {
            Event event = newEvent(viewer, null, details, audience, items, now);
            events.flush();
            return event;
        }

        Series saved = series.save(new Series(recurrence.frequency(), recurrence.interval(), recurrence.until(), now));
        Instant horizon = now.atZone(zone).plus(SERIES_HORIZON).toInstant();
        List<Event> created = new ArrayList<>();
        for (int step : recurrence.steps(details.startsAt(), zone, horizon)) {
            created.add(newEvent(viewer, saved, shifted(details, recurrence, step), audience, items, now));
        }
        events.flush();
        return created.getFirst();
    }

    /** Edits this occurrence (host only). Answers and the bring-list are kept. */
    public Event update(UUID viewer, UUID eventId, EventUpdate in) {
        Event event = findVisible(viewer, eventId);
        requireHost(event, viewer);
        Set<UUID> usableGroups = new HashSet<>(groups.groupIdsOf(viewer));
        usableGroups.addAll(event.getVisibility().groupIds());
        FieldErrors errors = new FieldErrors();
        EventDetails details = validator.details(in, usableGroups, errors);
        errors.check(in.version() != null, "version", "Missing version.");
        errors.throwIfAny();
        if (in.version() != event.getVersion()) {
            throw ApiException.staleVersion();
        }
        event.apply(details, clock.instant());
        if (details.visibility().scope() == VisibilityScope.GROUPS) {
            inviteAndAnnounce(event, groups.memberIdsOf(details.visibility().groupIds()));
        }
        events.flush();
        return event;
    }

    /** Cancels the event (host only). It then answers 404 to everyone. */
    public void cancel(UUID viewer, UUID eventId) {
        Event event = findVisible(viewer, eventId);
        requireHost(event, viewer);
        event.cancel(clock.instant());
        List<UUID> audience = event.getAnswers().stream().map(Answer::getMemberId)
                .filter(id -> !id.equals(viewer)).toList();
        publisher.publishEvent(new EventCancelled(event.getId(), viewer, audience));
    }

    /** Going, Maybe or Can't. Answering again replaces the earlier answer. */
    public Event rsvp(UUID viewer, UUID eventId, RsvpInput in) {
        Event event = findVisible(viewer, eventId);
        require(event, viewer, EventRules.RSVP);
        FieldErrors errors = new FieldErrors();
        RsvpStatus status = in == null ? null : in.status();
        errors.check(status != null && status != RsvpStatus.PENDING, "status", "Choose Going, Maybe or Can't.");
        String note = validator.note(in == null ? null : in.note(), "note", EventInputValidator.RSVP_NOTE_MAX, errors);
        errors.throwIfAny();
        event.answer(viewer, status, note, clock.instant());
        return event;
    }

    /** Invites members or whole groups (host only). Members already on the guest list are ignored. */
    public Event invite(UUID viewer, UUID eventId, InviteInput in) {
        Event event = findVisible(viewer, eventId);
        require(event, viewer, EventRules.INVITE);
        Set<UUID> usableGroups = groups.groupIdsOf(viewer);
        FieldErrors errors = new FieldErrors();
        Set<UUID> invitees = validator.invitees(in, "", usableGroups, groups, errors);
        errors.throwIfAny();
        inviteAndAnnounce(event, invitees);
        return event;
    }

    /** Adds a meal or task to "Who brings what". Anyone who can see the event may add. */
    public Event addBringItem(UUID viewer, UUID eventId, BringItemInput in) {
        Event event = findVisible(viewer, eventId);
        require(event, viewer, EventRules.ADD_BRING_ITEM);
        FieldErrors errors = new FieldErrors();
        BringItemInput item = validator.bringItem(in, "", errors);
        errors.throwIfAny();
        event.addBringItem(item.getTitle(), item.getDetails(), item.getSlots(), viewer, clock.instant());
        return event;
    }

    /** Removes an item (host or the member who added it). */
    public Event removeBringItem(UUID viewer, UUID eventId, UUID itemId) {
        Event event = findVisible(viewer, eventId);
        BringItem item = event.bringItem(itemId).orElseThrow(ApiException::notFound);
        if (!EventRules.itemActions(event, item, viewer).contains(EventRules.REMOVE)) {
            throw ApiException.forbidden();
        }
        event.removeBringItem(item);
        return event;
    }

    /**
     * "I'll take it". Holds a row lock on the item, so two members racing for the last slot get
     * one claim and one 409 {@code events.item-full}. Claiming again only updates the note.
     */
    public Event claim(UUID viewer, UUID eventId, UUID itemId, ClaimInput in) {
        Event event = findVisible(viewer, eventId);
        BringItem item = bringItems.lockForClaim(eventId, itemId).orElseThrow(ApiException::notFound);
        FieldErrors errors = new FieldErrors();
        String rawNote = in == null ? null : in.note();
        String note = validator.note(rawNote, "note", EventInputValidator.CLAIM_NOTE_MAX, errors);
        errors.throwIfAny();
        try {
            item.claim(viewer, note, rawNote != null, clock.instant());
        } catch (BringItem.ItemFullException e) {
            throw new ApiException(HttpStatus.CONFLICT, "events.item-full", "Already taken", "Someone was quicker.");
        }
        bringItems.flush();
        return event;
    }

    /** Gives the item back. Doing it when holding no claim changes nothing. */
    public Event unclaim(UUID viewer, UUID eventId, UUID itemId) {
        Event event = findVisible(viewer, eventId);
        BringItem item = event.bringItem(itemId).orElseThrow(ApiException::notFound);
        item.unclaim(viewer);
        return event;
    }

    private Event newEvent(UUID host, Series series, EventDetails details, Set<UUID> audience,
            List<BringItemInput> items, Instant now) {
        Event event = new Event(host, series, details, now);
        List<UUID> invited = event.invite(audience, now);
        for (BringItemInput item : items) {
            event.addBringItem(item.getTitle(), item.getDetails(), item.getSlots(), host, now);
        }
        events.save(event);
        announce(event, invited);
        return event;
    }

    /** Named invitees plus, for GROUPS, every member of the groups; never the host. */
    private Set<UUID> audience(EventDetails details, Set<UUID> invitees, UUID host) {
        Set<UUID> audience = new LinkedHashSet<>(invitees);
        if (details.visibility().scope() == VisibilityScope.GROUPS) {
            audience.addAll(groups.memberIdsOf(details.visibility().groupIds()));
        }
        audience.remove(host);
        return audience;
    }

    private EventDetails shifted(EventDetails d, Recurrence recurrence, int step) {
        if (step == 0) {
            return d;
        }
        List<ProgrammeItem> programme = d.programme().stream()
                .map(p -> new ProgrammeItem(recurrence.shift(p.startsAt(), step, zone), p.title()))
                .toList();
        List<SleepingSpot> sleeping = d.sleeping().stream().map(SleepingSpot::copy).toList();
        return new EventDetails(d.kind(), d.title(), d.description(), recurrence.shift(d.startsAt(), step, zone),
                recurrence.shift(d.endsAt(), step, zone), d.allDay(), d.location(), d.visibility(), programme,
                sleeping);
    }

    private void inviteAndAnnounce(Event event, Set<UUID> memberIds) {
        Set<UUID> others = new LinkedHashSet<>(memberIds);
        others.remove(event.getHostId());
        announce(event, event.invite(others, clock.instant()));
    }

    private void announce(Event event, List<UUID> invited) {
        if (!invited.isEmpty()) {
            publisher.publishEvent(new EventInvitationsSent(event.getId(), event.getHostId(), List.copyOf(invited)));
        }
    }

    private Event findVisible(UUID viewer, UUID eventId) {
        Event event = events.findById(eventId).orElseThrow(ApiException::notFound);
        if (!event.isVisibleTo(viewer, groups.groupIdsOf(viewer))) {
            throw ApiException.notFound();
        }
        return event;
    }

    private static void requireHost(Event event, UUID viewer) {
        if (!event.isHost(viewer)) {
            throw ApiException.forbidden();
        }
    }

    private static void require(Event event, UUID viewer, String action) {
        if (!EventRules.eventActions(event, viewer).contains(action)) {
            throw ApiException.forbidden();
        }
    }
}
