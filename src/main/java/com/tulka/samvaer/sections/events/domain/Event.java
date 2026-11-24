package com.tulka.samvaer.sections.events.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.tulka.samvaer.platform.visibility.Visibility;
import com.tulka.samvaer.platform.visibility.VisibilityScope;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.OptimisticLock;

/**
 * One event (or one occurrence of a series) and everything on its page.
 * {@code version} guards the host's edits only: answers and the bring-list change without
 * bumping it, so a guest's RSVP never makes the host's open edit form stale. Collections are
 * excluded from version checks; {@link #apply} sets {@code updatedAt}, which bumps it instead.
 */
@Entity
@Table(name = "events_event")
public class Event {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private Series series;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventKind kind;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    private String location;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", nullable = false)
    private VisibilityScope visibilityScope;

    @ElementCollection
    @CollectionTable(name = "events_event_group", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "group_id", nullable = false)
    @OrderColumn(name = "position")
    @OptimisticLock(excluded = true)
    private List<UUID> visibilityGroupIds = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OptimisticLock(excluded = true)
    private List<Answer> answers = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position, createdAt")
    @OptimisticLock(excluded = true)
    private List<BringItem> bringItems = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "events_programme_item", joinColumns = @JoinColumn(name = "event_id"))
    @OptimisticLock(excluded = true)
    private List<ProgrammeItem> programme = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "event_id", nullable = false)
    @OrderColumn(name = "position")
    @OptimisticLock(excluded = true)
    private List<SleepingSpot> sleeping = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Set by every host edit, so an edit that only touches lists still bumps {@code version}. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** Null until first saved, which also tells Spring Data to persist rather than merge. */
    @Version
    private Long version;

    protected Event() {
    }

    /** A new event; the host is on the guest list as going. */
    public Event(UUID hostId, Series series, EventDetails details, Instant now) {
        this.id = UUID.randomUUID();
        this.hostId = hostId;
        this.series = series;
        this.createdAt = now;
        apply(details, now);
        answers.add(new Answer(this, hostId, RsvpStatus.GOING, null, true, now));
    }

    /** Replaces the editable fields, programme and sleeping plan. Answers and bring-list stay. */
    public void apply(EventDetails details, Instant now) {
        this.updatedAt = now;
        this.kind = details.kind();
        this.title = details.title();
        this.description = details.description();
        this.startsAt = details.startsAt();
        this.endsAt = details.endsAt();
        this.allDay = details.allDay();
        this.location = details.location();
        this.visibilityScope = details.visibility().scope();
        this.visibilityGroupIds.clear();
        this.visibilityGroupIds.addAll(details.visibility().groupIds());
        this.programme.clear();
        this.programme.addAll(details.programme());
        this.sleeping.clear();
        details.sleeping().forEach(spot -> this.sleeping.add(spot.copy()));
    }

    public boolean isHost(UUID memberId) {
        return hostId.equals(memberId);
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    public void cancel(Instant now) {
        this.cancelledAt = now;
    }

    public Visibility getVisibility() {
        return new Visibility(visibilityScope, visibilityGroupIds);
    }

    /** Who may see it: host, everyone (ALL_MEMBERS), or group members and anyone on the guest list (GROUPS). */
    public boolean isVisibleTo(UUID viewer, Set<UUID> viewerGroupIds) {
        if (isCancelled()) {
            return false;
        }
        return getVisibility().allows(viewer, hostId, viewerGroupIds,
                answers.stream().map(Answer::getMemberId).toList());
    }

    /**
     * Puts members on the guest list as invited. Members already on it are only marked invited.
     *
     * @return the members who were not on the guest list before
     */
    public List<UUID> invite(Collection<UUID> memberIds, Instant now) {
        List<UUID> added = new ArrayList<>();
        for (UUID memberId : memberIds) {
            Optional<Answer> existing = answerOf(memberId);
            if (existing.isPresent()) {
                existing.get().markInvited();
            } else {
                answers.add(new Answer(this, memberId, RsvpStatus.PENDING, null, true, now));
                added.add(memberId);
            }
        }
        return added;
    }

    /** Sets or replaces the member's answer. */
    public void answer(UUID memberId, RsvpStatus status, String note, Instant now) {
        answerOf(memberId).ifPresentOrElse(
                a -> a.answer(status, note, now),
                () -> answers.add(new Answer(this, memberId, status, note, false, now)));
    }

    public Optional<Answer> answerOf(UUID memberId) {
        return answers.stream().filter(a -> a.getMemberId().equals(memberId)).findFirst();
    }

    public RsvpStatus statusOf(UUID memberId) {
        return answerOf(memberId).map(Answer::getStatus).orElse(RsvpStatus.PENDING);
    }

    public BringItem addBringItem(String title, String details, Integer slots, UUID createdBy, Instant now) {
        int position = bringItems.stream().mapToInt(BringItem::getPosition).max().orElse(-1) + 1;
        BringItem item = new BringItem(this, title, details, slots, createdBy, position, now);
        bringItems.add(item);
        return item;
    }

    public Optional<BringItem> bringItem(UUID itemId) {
        return bringItems.stream().filter(i -> i.getId().equals(itemId)).findFirst();
    }

    public void removeBringItem(BringItem item) {
        bringItems.remove(item);
    }

    public UUID getId() {
        return id;
    }

    public Series getSeries() {
        return series;
    }

    public EventKind getKind() {
        return kind;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public String getLocation() {
        return location;
    }

    public UUID getHostId() {
        return hostId;
    }

    public VisibilityScope getVisibilityScope() {
        return visibilityScope;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public List<BringItem> getBringItems() {
        return bringItems;
    }

    public List<ProgrammeItem> getProgramme() {
        return programme;
    }

    public List<SleepingSpot> getSleeping() {
        return sleeping;
    }

    public long getVersion() {
        return version == null ? 0 : version;
    }
}
