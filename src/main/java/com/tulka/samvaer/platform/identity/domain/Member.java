package com.tulka.samvaer.platform.identity.domain;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import com.tulka.samvaer.platform.identity.MemberRef;
import com.tulka.samvaer.platform.identity.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_member")
public class Member {

    @Id
    private UUID id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String initials;

    @Column(name = "avatar_media_id")
    private UUID avatarMediaId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "identity_member_role", joinColumns = @JoinColumn(name = "member_id"))
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    protected Member() {
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public MemberRef toRef() {
        return new MemberRef(id, displayName, initials, avatarMediaId);
    }
}
