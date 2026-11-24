package com.tulka.samvaer.sections.events.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import com.tulka.samvaer.platform.identity.CurrentMember;
import com.tulka.samvaer.sections.events.api.EventResponses.EventDetail;
import com.tulka.samvaer.sections.events.api.EventResponses.EventSummary;
import com.tulka.samvaer.sections.events.application.EventService;
import com.tulka.samvaer.sections.events.application.Inputs.BringItemInput;
import com.tulka.samvaer.sections.events.application.Inputs.ClaimInput;
import com.tulka.samvaer.sections.events.application.Inputs.EventInput;
import com.tulka.samvaer.sections.events.application.Inputs.EventUpdate;
import com.tulka.samvaer.sections.events.application.Inputs.InviteInput;
import com.tulka.samvaer.sections.events.application.Inputs.RsvpInput;
import com.tulka.samvaer.sections.events.domain.Event;
import com.tulka.samvaer.shared.web.PageRequests;
import com.tulka.samvaer.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Events API ({@code /api/v1/events}). Each call runs in one transaction so the response is
 * built from the same state the change was made in.
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events")
@Transactional
class EventsController {

    private final CurrentMember currentMember;
    private final EventService events;
    private final EventViewAssembler views;

    EventsController(CurrentMember currentMember, EventService events, EventViewAssembler views) {
        this.currentMember = currentMember;
        this.events = events;
        this.views = views;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(operationId = "listEvents", summary = "Events the current member can see")
    PageResponse<EventSummary> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PageRequests.DEFAULT_SIZE) int size) {
        UUID me = currentMember.id();
        var result = events.list(me, from, to, page, size);
        var items = views.summaries(result.getContent(), me);
        return new PageResponse<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @PostMapping
    @Operation(operationId = "createEvent", summary = "Create an event")
    ResponseEntity<EventDetail> create(@RequestBody EventInput body) {
        UUID me = currentMember.id();
        Event event = events.create(me, body);
        return ResponseEntity.created(URI.create("/api/v1/events/" + event.getId()))
                .body(views.detail(event, me));
    }

    @GetMapping("/{eventId}")
    @Transactional(readOnly = true)
    @Operation(operationId = "getEvent", summary = "One event with everything on its page")
    EventDetail get(@PathVariable UUID eventId) {
        UUID me = currentMember.id();
        return views.detail(events.get(me, eventId), me);
    }

    @PutMapping("/{eventId}")
    @Operation(operationId = "updateEvent", summary = "Edit an event (host only)")
    EventDetail update(@PathVariable UUID eventId, @RequestBody EventUpdate body) {
        UUID me = currentMember.id();
        return views.detail(events.update(me, eventId, body), me);
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "cancelEvent", summary = "Cancel an event (host only)")
    void cancel(@PathVariable UUID eventId) {
        events.cancel(currentMember.id(), eventId);
    }

    @PutMapping("/{eventId}/rsvp")
    @Operation(operationId = "setMyRsvp", summary = "Answer Going, Maybe or Can't")
    EventDetail rsvp(@PathVariable UUID eventId, @RequestBody RsvpInput body) {
        UUID me = currentMember.id();
        return views.detail(events.rsvp(me, eventId, body), me);
    }

    @PostMapping("/{eventId}/invites")
    @Operation(operationId = "inviteToEvent", summary = "Invite members or whole groups")
    EventDetail invite(@PathVariable UUID eventId, @RequestBody InviteInput body) {
        UUID me = currentMember.id();
        return views.detail(events.invite(me, eventId, body), me);
    }

    @PostMapping("/{eventId}/bring-items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "addBringItem", summary = "Add a meal or task to \"Who brings what\"")
    EventDetail addBringItem(@PathVariable UUID eventId, @RequestBody BringItemInput body) {
        UUID me = currentMember.id();
        return views.detail(events.addBringItem(me, eventId, body), me);
    }

    @DeleteMapping("/{eventId}/bring-items/{itemId}")
    @Operation(operationId = "removeBringItem", summary = "Remove an item from the bring-list")
    EventDetail removeBringItem(@PathVariable UUID eventId, @PathVariable UUID itemId) {
        UUID me = currentMember.id();
        return views.detail(events.removeBringItem(me, eventId, itemId), me);
    }

    @PutMapping("/{eventId}/bring-items/{itemId}/claim")
    @Operation(operationId = "claimBringItem", summary = "I'll take it")
    EventDetail claim(@PathVariable UUID eventId, @PathVariable UUID itemId,
            @RequestBody(required = false) ClaimInput body) {
        UUID me = currentMember.id();
        return views.detail(events.claim(me, eventId, itemId, body), me);
    }

    @DeleteMapping("/{eventId}/bring-items/{itemId}/claim")
    @Operation(operationId = "unclaimBringItem", summary = "Give the item back")
    EventDetail unclaim(@PathVariable UUID eventId, @PathVariable UUID itemId) {
        UUID me = currentMember.id();
        return views.detail(events.unclaim(me, eventId, itemId), me);
    }
}
