package com.tulka.samvaer.shared.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The clock and the home time zone. Instants are stored and sent in UTC; the home zone is used
 * where a rule depends on the local calendar, such as "today" and repeating an event at the same
 * local time across daylight saving changes.
 */
@Configuration
public class TimeConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    HomeZone homeZone(@Value("${samvaer.time-zone:Europe/Copenhagen}") String zone) {
        return new HomeZone(ZoneId.of(zone));
    }
}
