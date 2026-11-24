package com.tulka.samvaer.shared.config;

import java.time.ZoneId;

/** The farm's time zone, used for calendar rules (today, recurring events). */
public record HomeZone(ZoneId zoneId) {
}
