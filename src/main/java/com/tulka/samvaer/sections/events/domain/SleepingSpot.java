package com.tulka.samvaer.sections.events.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/** A place to sleep and who sleeps there. */
@Entity
@Table(name = "events_sleeping_spot")
public class SleepingSpot {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String place;

    private String note;

    @ElementCollection
    @CollectionTable(name = "events_sleeping_guest", joinColumns = @JoinColumn(name = "spot_id"))
    @Column(name = "member_id", nullable = false)
    @OrderColumn(name = "position")
    private List<UUID> guestIds = new ArrayList<>();

    protected SleepingSpot() {
    }

    public SleepingSpot(String place, List<UUID> guestIds, String note) {
        this.id = UUID.randomUUID();
        this.place = place;
        this.guestIds = new ArrayList<>(guestIds);
        this.note = note;
    }

    public SleepingSpot copy() {
        return new SleepingSpot(place, guestIds, note);
    }

    public String getPlace() {
        return place;
    }

    public String getNote() {
        return note;
    }

    public List<UUID> getGuestIds() {
        return guestIds;
    }
}
