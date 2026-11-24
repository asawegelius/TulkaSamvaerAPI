package com.tulka.samvaer.sections.events.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** A meal or task on "Who brings what". {@code slots == null} means anyone who wants to. */
@Entity
@Table(name = "events_bring_item")
public class BringItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(nullable = false)
    private String title;

    private String details;

    private Integer slots;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("claimedAt, id")
    private List<Claim> claims = new ArrayList<>();

    protected BringItem() {
    }

    BringItem(Event event, String title, String details, Integer slots, UUID createdBy, int position, Instant now) {
        this.id = UUID.randomUUID();
        this.event = event;
        this.title = title;
        this.details = details;
        this.slots = slots;
        this.createdBy = createdBy;
        this.position = position;
        this.createdAt = now;
    }

    public boolean isFull() {
        return slots != null && claims.size() >= slots;
    }

    public Optional<Claim> claimOf(UUID memberId) {
        return claims.stream().filter(c -> c.getMemberId().equals(memberId)).findFirst();
    }

    /** Claims a slot, or only updates the note when the member already holds one. */
    public void claim(UUID memberId, String note, boolean noteGiven, Instant now) {
        Optional<Claim> mine = claimOf(memberId);
        if (mine.isPresent()) {
            if (noteGiven) {
                mine.get().setNote(note);
            }
            return;
        }
        if (isFull()) {
            throw new ItemFullException();
        }
        claims.add(new Claim(this, memberId, note, now));
    }

    public void unclaim(UUID memberId) {
        claims.removeIf(c -> c.getMemberId().equals(memberId));
    }

    public UUID getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public String getTitle() {
        return title;
    }

    public String getDetails() {
        return details;
    }

    public Integer getSlots() {
        return slots;
    }

    public int getPosition() {
        return position;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public List<Claim> getClaims() {
        return claims;
    }

    /** Thrown when every slot is taken. */
    public static class ItemFullException extends RuntimeException {
        ItemFullException() {
            super("Every slot is taken");
        }
    }
}
