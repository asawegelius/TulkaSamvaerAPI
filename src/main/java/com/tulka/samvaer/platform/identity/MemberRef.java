package com.tulka.samvaer.platform.identity;

import java.util.UUID;

/** A member as shown next to an answer, claim or bed. */
public record MemberRef(UUID id, String displayName, String initials, UUID avatarMediaId) {

    /** Shown for an id that no longer belongs to a member. */
    public static MemberRef former(UUID id) {
        return new MemberRef(id, "Former member", "?", null);
    }
}
