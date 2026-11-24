package com.tulka.samvaer.platform.groups;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Read access to groups for other modules. */
public interface GroupDirectory {

    /** Groups the member belongs to or made. These are the groups they can share with or invite. */
    Set<UUID> groupIdsOf(UUID memberId);

    /** Every member of any of the groups. */
    Set<UUID> memberIdsOf(Collection<UUID> groupIds);

    /** Names of the groups, in the order the ids were given; unknown ids are left out. */
    List<String> namesOf(List<UUID> groupIds);

    /** Group id to name for the known ids. */
    Map<UUID, String> nameMap(Collection<UUID> groupIds);
}
