package com.tulka.samvaer.sections.events

import com.tulka.samvaer.sections.events.domain.Frequency
import com.tulka.samvaer.sections.events.domain.Recurrence
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RecurrenceSpec extends Specification {

    static final ZoneId CPH = ZoneId.of('Europe/Copenhagen')

    static at(String local) { LocalDateTime.parse(local).atZone(CPH).toInstant() }

    static local(instant) { instant.atZone(CPH).toLocalDateTime().toString() }

    def "weekly keeps the local time across the switch to summer time"() {
        given:
        def weekly = new Recurrence(Frequency.WEEKLY, 1, null)

        expect: 'Thursday 19:00 stays 19:00 after 29 March 2026'
        local(weekly.shift(at('2026-03-26T19:00'), 1, CPH)) == '2026-04-02T19:00'
    }

    def "monthly from the 31st falls on the last day of shorter months"() {
        given:
        def monthly = new Recurrence(Frequency.MONTHLY, 1, null)
        def first = at('2027-01-31T12:00')

        expect:
        local(monthly.shift(first, 1, CPH)) == '2027-02-28T12:00'
        local(monthly.shift(first, 2, CPH)) == '2027-03-31T12:00'
    }

    def "yearly every 1 year with an interval"() {
        given:
        def every2 = new Recurrence(Frequency.YEARLY, 2, null)

        expect:
        local(every2.shift(at('2026-12-24T17:00'), 3, CPH)) == '2032-12-24T17:00'
    }

    def "steps stop at until, at the horizon, and at the cap"() {
        given:
        def first = at('2026-12-01T18:00')

        expect: 'until is inclusive by local date'
        new Recurrence(Frequency.WEEKLY, 1, LocalDate.parse('2026-12-15')).steps(first, CPH, at('2030-01-01T00:00')) == [0, 1, 2]

        and: 'nothing after the horizon, but the first occurrence always'
        new Recurrence(Frequency.YEARLY, 1, null).steps(first, CPH, at('2028-06-01T00:00')) == [0, 1]
        new Recurrence(Frequency.YEARLY, 1, null).steps(first, CPH, at('2026-01-01T00:00')) == [0]

        and: 'at most 24 occurrences up front'
        new Recurrence(Frequency.WEEKLY, 1, null).steps(first, CPH, at('2030-01-01T00:00')).size() == Recurrence.MAX_OCCURRENCES
    }
}
