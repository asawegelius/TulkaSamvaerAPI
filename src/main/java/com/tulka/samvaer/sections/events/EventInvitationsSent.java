package com.tulka.samvaer.sections.events;

import java.util.List;
import java.util.UUID;

/** Published when members are newly invited to an event, so messaging can notify them. */
public record EventInvitationsSent(UUID eventId, UUID hostId, List<UUID> invitedMemberIds) {
}
