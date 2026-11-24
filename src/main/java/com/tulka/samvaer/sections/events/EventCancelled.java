package com.tulka.samvaer.sections.events;

import java.util.List;
import java.util.UUID;

/** Published when the host cancels an event, with everyone who had answered or been invited. */
public record EventCancelled(UUID eventId, UUID hostId, List<UUID> audienceMemberIds) {
}
