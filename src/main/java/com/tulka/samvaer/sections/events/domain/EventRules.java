package com.tulka.samvaer.sections.events.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tulka.samvaer.platform.visibility.VisibilityScope;

/**
 * What a member may do with an event they can see. The API sends these as {@code allowedActions};
 * the service checks the same rules before acting.
 */
public final class EventRules {

    public static final String EDIT = "edit";
    public static final String CANCEL = "cancel";
    public static final String INVITE = "invite";
    public static final String RSVP = "rsvp";
    public static final String ADD_BRING_ITEM = "add-bring-item";

    public static final String CLAIM = "claim";
    public static final String UNCLAIM = "unclaim";
    public static final String REMOVE = "remove";

    private EventRules() {
    }

    /**
     * Host: edit, cancel, invite (not on a private "Only me" event). Everyone who can see it:
     * rsvp (not on a private event) and add-bring-item.
     */
    public static List<String> eventActions(Event event, UUID viewer) {
        boolean host = event.isHost(viewer);
        boolean shared = event.getVisibilityScope() != VisibilityScope.ONLY_ME;
        List<String> actions = new ArrayList<>();
        if (host) {
            actions.add(EDIT);
            actions.add(CANCEL);
            if (shared) {
                actions.add(INVITE);
            }
        }
        if (shared) {
            actions.add(RSVP);
        }
        actions.add(ADD_BRING_ITEM);
        return actions;
    }

    /** unclaim when the viewer holds a slot, otherwise claim while slots are free; remove for host or creator. */
    public static List<String> itemActions(Event event, BringItem item, UUID viewer) {
        List<String> actions = new ArrayList<>();
        if (item.claimOf(viewer).isPresent()) {
            actions.add(UNCLAIM);
        } else if (!item.isFull()) {
            actions.add(CLAIM);
        }
        if (event.isHost(viewer) || item.getCreatedBy().equals(viewer)) {
            actions.add(REMOVE);
        }
        return actions;
    }
}
