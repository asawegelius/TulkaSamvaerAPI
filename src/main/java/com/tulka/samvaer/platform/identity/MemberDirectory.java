package com.tulka.samvaer.platform.identity;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Read access to members for other modules. */
public interface MemberDirectory {

    /** Refs for the given ids. Unknown ids map to {@link MemberRef#former(UUID)}. */
    Map<UUID, MemberRef> refsOf(Collection<UUID> memberIds);

    /** The subset of {@code memberIds} that are members. */
    Set<UUID> existing(Collection<UUID> memberIds);
}
