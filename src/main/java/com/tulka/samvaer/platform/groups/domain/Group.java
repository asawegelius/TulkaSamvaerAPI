package com.tulka.samvaer.platform.groups.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "groups_group")
public class Group {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "groups_membership", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "member_id", nullable = false)
    private Set<UUID> memberIds = new HashSet<>();

    protected Group() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Set<UUID> getMemberIds() {
        return memberIds;
    }

    public boolean includes(UUID memberId) {
        return ownerId.equals(memberId) || memberIds.contains(memberId);
    }
}
