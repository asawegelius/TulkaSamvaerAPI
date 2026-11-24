package com.tulka.samvaer.sections.events.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** "I'll take it": one member holding one slot of a bring item. */
@Entity
@Table(name = "events_claim")
public class Claim {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private BringItem item;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    private String note;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    protected Claim() {
    }

    Claim(BringItem item, UUID memberId, String note, Instant claimedAt) {
        this.id = UUID.randomUUID();
        this.item = item;
        this.memberId = memberId;
        this.note = note;
        this.claimedAt = claimedAt;
    }

    void setNote(String note) {
        this.note = note;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public String getNote() {
        return note;
    }
}
