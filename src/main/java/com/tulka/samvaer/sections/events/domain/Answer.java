package com.tulka.samvaer.sections.events.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One member on an event's guest list: invited (status PENDING until they answer) and/or
 * answered. On an open (all members) event, members who answer without an invite get a row too.
 */
@Entity
@Table(name = "events_answer")
public class Answer {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RsvpStatus status;

    private String note;

    @Column(nullable = false)
    private boolean invited;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Answer() {
    }

    Answer(Event event, UUID memberId, RsvpStatus status, String note, boolean invited, Instant now) {
        this.id = UUID.randomUUID();
        this.event = event;
        this.memberId = memberId;
        this.status = status;
        this.note = note;
        this.invited = invited;
        this.updatedAt = now;
    }

    void answer(RsvpStatus status, String note, Instant now) {
        this.status = status;
        this.note = note;
        this.updatedAt = now;
    }

    void markInvited() {
        this.invited = true;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public RsvpStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public boolean isInvited() {
        return invited;
    }
}
