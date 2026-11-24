package com.tulka.samvaer.platform.visibility;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Who may see an item. {@code groupIds} is non-empty for GROUPS and empty otherwise. */
public record Visibility(VisibilityScope scope, List<UUID> groupIds) {

    public Visibility {
        groupIds = scope == VisibilityScope.GROUPS && groupIds != null ? List.copyOf(groupIds) : List.of();
    }

    /**
     * Whether {@code viewer} may see an item with this visibility.
     *
     * @param ownerId        the item's owner, who always sees it
     * @param viewerGroupIds groups the viewer belongs to
     * @param namedAudience  members the item names directly (for example invited guests), who see a GROUPS item
     */
    public boolean allows(UUID viewer, UUID ownerId, Set<UUID> viewerGroupIds, Collection<UUID> namedAudience) {
        if (ownerId.equals(viewer)) {
            return true;
        }
        return switch (scope) {
            case ONLY_ME -> false;
            case ALL_MEMBERS -> true;
            case GROUPS -> namedAudience.contains(viewer) || groupIds.stream().anyMatch(viewerGroupIds::contains);
        };
    }
}
