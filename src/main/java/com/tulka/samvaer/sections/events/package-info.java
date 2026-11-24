/**
 * Events section (key {@code events}): events, invites, RSVPs, "who brings what", programme,
 * sleeping plan and recurring events. All rules live here; the frontend shows what
 * {@code allowedActions} lists. API prefix {@code /api/v1/events}, tables {@code events_*}.
 */
@ApplicationModule(displayName = "Events")
package com.tulka.samvaer.sections.events;

import org.springframework.modulith.ApplicationModule;
