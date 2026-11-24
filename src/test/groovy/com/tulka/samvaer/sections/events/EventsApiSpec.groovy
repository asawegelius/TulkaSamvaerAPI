package com.tulka.samvaer.sections.events

import com.tulka.samvaer.support.ApiSpec

import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EventsApiSpec extends ApiSpec {

    // --- create -----------------------------------------------------------------------------

    def "creating an event makes the caller host and going, and invites the chosen groups"() {
        when:
        def response = post(ASA, '/api/v1/events', eventBody(invite: [memberIds: [LINE.toString()]]))
        def event = response.body

        then:
        response.status == 201
        response.location == "/api/v1/events/${event.id}"
        event.host.displayName == 'Åsa'
        event.myRsvp == 'GOING'
        event.audienceLabel == 'Best friends'
        event.allowedActions == ['edit', 'cancel', 'invite', 'rsvp', 'add-bring-item']
        event.series == null

        and: 'six best friends plus Line are pending'
        event.rsvpSummary == [going: 1, maybe: 0, cant: 0, pending: 7]
        event.attendees.first().member.displayName == 'Åsa'
        event.attendees*.status.count('PENDING') == 7
    }

    def "a guest sees the event with their own answer and actions"() {
        given:
        def event = createEvent(ASA)

        when:
        def seen = get(SOFIE, "/api/v1/events/${event.id}").body

        then:
        seen.myRsvp == 'PENDING'
        seen.allowedActions == ['rsvp', 'add-bring-item']
    }

    def "bring items, programme and sleeping plan can be given when creating"() {
        given:
        def start = Instant.parse(eventBody().startsAt as String)

        when:
        def event = createEvent(ASA, [
            programme : [[startsAt: start.plusSeconds(3600).toString(), title: 'Long game'],
                         [startsAt: start.toString(), title: 'Arrive']],
            sleeping  : [[place: 'Guest apartment', guestIds: [SOFIE.toString(), KASPER.toString()]],
                         [place: 'Office', guestIds: [PETER.toString()], note: 'sofa bed']],
            bringItems: [[title: 'Friday dinner', details: 'Something for 8'], [title: '  '],
                         [title: 'Two new games each', slots: null], [title: 'Sunday clean-up', slots: 3]],
        ])

        then: 'programme is sorted by time'
        event.programme*.title == ['Arrive', 'Long game']
        event.sleeping*.place == ['Guest apartment', 'Office']
        event.sleeping[0].guests*.displayName == ['Sofie', 'Kasper']
        event.sleeping[1].note == 'sofa bed'

        and: 'empty rows are skipped; slots default to 1, null means anyone'
        event.bringItems*.title == ['Friday dinner', 'Two new games each', 'Sunday clean-up']
        event.bringItems*.slots == [1, null, 3]
        event.bringList == [total: 3, taken: 0]
    }

    def "invalid input names the fields"() {
        when:
        def response = post(ASA, '/api/v1/events', eventBody(
                title: ' ', endsAt: eventBody().startsAt, visibility: [scope: 'GROUPS', groupIds: []]))

        then:
        response.status == 400
        response.code == 'common.validation'
        response.body.fieldErrors*.field as Set == ['title', 'endsAt', 'visibility.groupIds'] as Set
    }

    def "sharing with a group you are not in is refused"() {
        expect:
        post(EMIL, '/api/v1/events', eventBody(visibility: [scope: 'GROUPS', groupIds: [FAMILY.toString()]]))
                .body.fieldErrors*.field == ['visibility.groupIds']
    }

    def "an unknown kind is a field error, not a crash"() {
        when:
        def response = post(ASA, '/api/v1/events', eventBody(kind: 'PARTY'))

        then:
        response.status == 400
        response.code == 'common.validation'
        response.body.fieldErrors*.field == ['kind']
    }

    def "a private event cannot have guests"() {
        expect:
        post(ASA, '/api/v1/events', eventBody(visibility: [scope: 'ONLY_ME', groupIds: []],
                invite: [memberIds: [JONAS.toString()]])).body.fieldErrors*.field == ['invite']
    }

    def "malformed JSON answers common.invalid-json"() {
        expect:
        post(ASA, '/api/v1/events', '{"title": ').code == 'common.invalid-json'
    }

    // --- visibility -------------------------------------------------------------------------

    def "Only me: nobody but the host can see it, and nobody can answer"() {
        given:
        def event = createEvent(ASA, [visibility: [scope: 'ONLY_ME', groupIds: []]])

        expect:
        event.audienceLabel == 'Only me'
        event.allowedActions == ['edit', 'cancel', 'add-bring-item']
        get(LINE, "/api/v1/events/${event.id}").status == 404
        get(LINE, '/api/v1/events').body.items.isEmpty()
        put(ASA, "/api/v1/events/${event.id}/rsvp", [status: 'GOING']).status == 403
    }

    def "Groups: group members and invited guests see it, others get 404"() {
        given:
        def event = createEvent(ASA, [invite: [memberIds: [NORA.toString()]]])

        expect:
        get(JONAS, "/api/v1/events/${event.id}").status == 200
        get(NORA, "/api/v1/events/${event.id}").status == 200
        get(MORMOR, "/api/v1/events/${event.id}").status == 404
        get(MORMOR, "/api/v1/events/${event.id}").code == 'common.not-found'
    }

    def "All members: everyone can see and answer; only invited members count as pending"() {
        given:
        def event = createEvent(LINE, [visibility: [scope: 'ALL_MEMBERS', groupIds: []],
                                      invite: [memberIds: [MORMOR.toString()]]])

        when:
        def seen = get(EMIL, "/api/v1/events/${event.id}").body
        def answered = put(EMIL, "/api/v1/events/${event.id}/rsvp", [status: 'MAYBE', note: 'Maybe Sunday']).body

        then:
        seen.audienceLabel == 'All members'
        seen.myRsvp == 'PENDING'
        seen.rsvpSummary == [going: 1, maybe: 0, cant: 0, pending: 1]
        answered.myRsvp == 'MAYBE'
        answered.rsvpSummary == [going: 1, maybe: 1, cant: 0, pending: 1]
        answered.attendees.find { it.member.displayName == 'Emil' }.note == 'Maybe Sunday'
    }

    def "the list shows exactly the events each member can open"() {
        given:
        createEvent(ASA, [title: 'Private', visibility: [scope: 'ONLY_ME', groupIds: []]])
        createEvent(ASA, [title: 'Friends'])
        createEvent(LINE, [title: 'Cousins', visibility: [scope: 'GROUPS', groupIds: [COUSINS.toString()]],
                           invite: [memberIds: [HENRIK.toString()]]])
        createEvent(MORMOR, [title: 'Open', visibility: [scope: 'ALL_MEMBERS', groupIds: []]])
        def all = jdbc.queryForList('select id from events_event', UUID)

        expect:
        [ASA, JONAS, HENRIK, LINE, EMIL, MORFAR].every { member ->
            def listed = get(member, '/api/v1/events?size=100').body.items*.id as Set
            def openable = all.findAll { get(member, "/api/v1/events/$it").status == 200 }*.toString() as Set
            listed == openable
        }

        and:
        get(ASA, '/api/v1/events').body.items*.title as Set == ['Private', 'Friends', 'Cousins', 'Open'] as Set
        get(HENRIK, '/api/v1/events').body.items*.title as Set == ['Friends', 'Cousins', 'Open'] as Set
        get(MORFAR, '/api/v1/events').body.items*.title == ['Open']
    }

    // --- list -------------------------------------------------------------------------------

    def "the list is soonest first, paged, and filtered by calendar dates"() {
        given:
        def today = LocalDate.now(HOME)
        [5, 1, 3].each { days ->
            def start = at(today.plusDays(days), 18)
            createEvent(ASA, [title: "In $days days", startsAt: start.toString(), endsAt: start.plusSeconds(3600).toString()])
        }
        def past = at(today.minusDays(3), 18)
        createEvent(ASA, [title: 'Last week', startsAt: past.toString(), endsAt: past.plusSeconds(3600).toString()])

        when:
        def first = get(ASA, '/api/v1/events?size=2').body
        def second = get(ASA, '/api/v1/events?size=2&page=1').body

        then: 'from defaults to today'
        first.items*.title == ['In 1 days', 'In 3 days']
        first.totalItems == 3
        second.items*.title == ['In 5 days']

        and:
        get(ASA, "/api/v1/events?from=${today.minusDays(7)}&to=${today.plusDays(3)}").body.items*.title ==
                ['Last week', 'In 1 days', 'In 3 days']
    }

    def "bad list parameters are field errors"() {
        expect:
        get(ASA, '/api/v1/events?from=2026-12-24&to=2026-12-01').body.fieldErrors*.field == ['to']
        get(ASA, '/api/v1/events?size=500').body.fieldErrors*.field == ['size']
        get(ASA, '/api/v1/events?from=tomorrow').body.fieldErrors*.field == ['from']
    }

    def "a malformed id is simply not found"() {
        expect:
        get(ASA, '/api/v1/events/not-a-uuid').status == 404
    }

    // --- RSVP -------------------------------------------------------------------------------

    def "answering again replaces the earlier answer"() {
        given:
        def event = createEvent(ASA)

        when:
        put(JONAS, "/api/v1/events/${event.id}/rsvp", [status: 'GOING'])
        def after = put(JONAS, "/api/v1/events/${event.id}/rsvp", [status: 'CANT', note: 'Sorry']).body

        then:
        after.myRsvp == 'CANT'
        after.rsvpSummary == [going: 1, maybe: 0, cant: 1, pending: 5]
        after.attendees.last().member.displayName == 'Jonas'
    }

    def "PENDING is not an answer"() {
        given:
        def event = createEvent(ASA)

        expect:
        put(JONAS, "/api/v1/events/${event.id}/rsvp", [status: 'PENDING']).body.fieldErrors*.field == ['status']
    }

    // --- edit and cancel --------------------------------------------------------------------

    def "only the host edits, and a stale version answers 409"() {
        given:
        def event = createEvent(ASA)
        def edit = eventBody(title: 'Board game weekend, take two', version: event.version)

        expect: 'a guest may see it but not edit it'
        put(JONAS, "/api/v1/events/${event.id}", edit).status == 403

        when: 'a guest answers and adds an item, which does not make the host\'s form stale'
        put(JONAS, "/api/v1/events/${event.id}/rsvp", [status: 'GOING'])
        post(JONAS, "/api/v1/events/${event.id}/bring-items", [title: 'Snacks'])
        def edited = put(ASA, "/api/v1/events/${event.id}", edit)

        then:
        edited.status == 200
        edited.body.title == 'Board game weekend, take two'
        edited.body.version == event.version + 1
        edited.body.attendees.find { it.member.displayName == 'Jonas' }.status == 'GOING'

        and: 'saving again with the old version is refused'
        put(ASA, "/api/v1/events/${event.id}", edit).with { status == 409 && code == 'common.stale-version' }

        and: 'an edit that only changes the sleeping plan also counts'
        put(ASA, "/api/v1/events/${event.id}", edit + [version: event.version + 1,
                sleeping: [[place: 'Office', guestIds: [PETER.toString()]]]]).body.version == event.version + 2
    }

    def "an edit that shares with another group invites its members, keeping existing answers"() {
        given:
        def event = createEvent(ASA)
        put(JONAS, "/api/v1/events/${event.id}/rsvp", [status: 'MAYBE'])

        when:
        def edited = put(ASA, "/api/v1/events/${event.id}", eventBody(version: event.version,
                visibility: [scope: 'GROUPS', groupIds: [BEST_FRIENDS.toString(), FAMILY.toString()]])).body

        then:
        edited.audienceLabel == 'Best friends, Family'
        edited.rsvpSummary == [going: 1, maybe: 1, cant: 0, pending: 9]
    }

    def "the host cancels; the event is gone for everyone"() {
        given:
        def event = createEvent(ASA)

        expect:
        delete(JONAS, "/api/v1/events/${event.id}").status == 403
        delete(ASA, "/api/v1/events/${event.id}").status == 204
        get(ASA, "/api/v1/events/${event.id}").status == 404
        get(JONAS, '/api/v1/events').body.items.isEmpty()
    }

    // --- invites ----------------------------------------------------------------------------

    def "the host invites members and groups; members already invited are ignored"() {
        given:
        def event = createEvent(ASA)

        when:
        def invited = post(ASA, "/api/v1/events/${event.id}/invites",
                [memberIds: [JONAS.toString(), NORA.toString()], groupIds: [COUSINS.toString()]]).body

        then: 'Nora and Emil are new, Line comes with Cousins'
        invited.rsvpSummary.pending == 6 + 3
        get(EMIL, "/api/v1/events/${event.id}").status == 200

        and:
        post(JONAS, "/api/v1/events/${event.id}/invites", [memberIds: [EMIL.toString()]]).status == 403
        post(ASA, "/api/v1/events/${event.id}/invites", [memberIds: [UUID.randomUUID().toString()]])
                .body.fieldErrors*.field == ['memberIds']
    }

    // --- recurring events -------------------------------------------------------------------

    def "a recurring event creates its occurrences, each with its own answers and bring-list"() {
        given:
        def today = LocalDate.now(HOME)
        def start = at(today.plusDays(1), 19)

        when:
        def first = createEvent(ASA, [
            title     : 'Pottery evening',
            startsAt  : start.toString(),
            endsAt    : start.plusSeconds(2 * 3600).toString(),
            programme : [[startsAt: start.plusSeconds(1800).toString(), title: 'Glazing']],
            recurrence: [frequency: 'WEEKLY', interval: 2, until: today.plusDays(1 + 6 * 7).toString()],
            bringItems: [[title: 'Cake']],
        ])
        def listed = get(ASA, '/api/v1/events?size=100').body.items

        then: 'every other week, up to and including the last date'
        first.series.recurrence == [frequency: 'WEEKLY', interval: 2, until: today.plusDays(1 + 6 * 7).toString()]
        listed.size() == 4
        listed*.series*.id.unique() == [first.series.id]
        listed.collect { Instant.parse(it.startsAt).atZone(HOME).toLocalTime().toString() }.unique() == ['19:00']

        when: 'Jonas answers and claims on the second occurrence only'
        def second = listed[1]
        put(JONAS, "/api/v1/events/${second.id}/rsvp", [status: 'GOING'])
        def secondDetail = get(JONAS, "/api/v1/events/${second.id}").body
        put(JONAS, "/api/v1/events/${second.id}/bring-items/${secondDetail.bringItems[0].id}/claim")

        then:
        get(JONAS, "/api/v1/events/${first.id}").body.myRsvp == 'PENDING'
        get(JONAS, "/api/v1/events/${first.id}").body.bringList.taken == 0
        get(JONAS, "/api/v1/events/${second.id}").body.with { myRsvp == 'GOING' && bringList.taken == 1 }

        and: 'the programme moved with the occurrence'
        Instant.parse(secondDetail.programme[0].startsAt) == Instant.parse(second.startsAt).plusSeconds(1800)
    }

    def "a recurrence's last date cannot be before the start"() {
        expect:
        post(ASA, '/api/v1/events', eventBody(recurrence: [frequency: 'YEARLY', until: '2000-01-01']))
                .body.fieldErrors*.field == ['recurrence.until']
    }

    // --- who brings what --------------------------------------------------------------------

    def "anyone who can see the event adds items; slots left out means one"() {
        given:
        def event = createEvent(ASA)

        when:
        def added = post(SOFIE, "/api/v1/events/${event.id}/bring-items", [title: 'Lift from the station'])

        then:
        added.status == 201
        added.body.bringItems[0].slots == 1
        added.body.bringItems[0].allowedActions == ['claim', 'remove']

        and:
        post(MORMOR, "/api/v1/events/${event.id}/bring-items", [title: 'Cake']).status == 404
        post(SOFIE, "/api/v1/events/${event.id}/bring-items", [title: '']).body.fieldErrors*.field == ['title']
    }

    def "claiming takes a slot; a full item answers 409 events.item-full"() {
        given:
        def event = createEvent(ASA, [bringItems: [[title: 'Friday dinner']]])
        def item = event.bringItems[0].id
        def path = "/api/v1/events/${event.id}/bring-items/${item}/claim"

        when:
        def claimed = put(SOFIE, path, [note: 'chili']).body

        then:
        claimed.bringItems[0].claimedBy == [[member: [id: SOFIE.toString(), displayName: 'Sofie', initials: 'S', avatarMediaId: null], note: 'chili']]
        claimed.bringItems[0].allowedActions == ['unclaim']
        claimed.bringList == [total: 1, taken: 1]

        and: 'the host sees it as taken and can still remove it'
        get(ASA, "/api/v1/events/${event.id}").body.bringItems[0].allowedActions == ['remove']

        and:
        put(KASPER, path, null).with { status == 409 && code == 'events.item-full' }

        when: 'claiming again only updates the note'
        def renamed = put(SOFIE, path, [note: 'lasagne']).body

        then:
        renamed.bringItems[0].claimedBy*.note == ['lasagne']

        when:
        def returned = delete(SOFIE, path).body

        then:
        returned.bringItems[0].claimedBy.isEmpty()
        put(KASPER, path, null).status == 200
    }

    def "an item for anyone takes every claim"() {
        given:
        def event = createEvent(ASA, [bringItems: [[title: 'Two new games each', slots: null]]])
        def path = "/api/v1/events/${event.id}/bring-items/${event.bringItems[0].id}/claim"

        when:
        [JONAS, SOFIE, KASPER, MAJA].each { put(it, path, null) }

        then:
        get(PETER, "/api/v1/events/${event.id}").body.bringItems[0].with {
            claimedBy.size() == 4 && allowedActions == ['claim']
        }
    }

    def "only the host or the member who added an item removes it"() {
        given:
        def event = createEvent(ASA)
        def item = post(PETER, "/api/v1/events/${event.id}/bring-items", [title: 'Lift']).body.bringItems[0].id
        def path = "/api/v1/events/${event.id}/bring-items/${item}"

        expect:
        delete(JONAS, path).status == 403
        delete(PETER, path).body.bringItems.isEmpty()
        delete(ASA, path).status == 404
    }

    def "two members racing for the last slot: one gets it, the other a 409"() {
        given:
        def event = createEvent(ASA, [bringItems: [[title: 'Saturday lunch']]])
        def path = "/api/v1/events/${event.id}/bring-items/${event.bringItems[0].id}/claim"
        def racers = [JONAS, SOFIE, KASPER, MAJA, PETER]
        def pool = Executors.newFixedThreadPool(racers.size())
        def go = new CountDownLatch(1)

        when:
        def futures = racers.collect { member -> pool.submit({ go.await(); put(member, path, null).status } as Callable<Integer>) }
        go.countDown()
        def statuses = futures*.get(30, TimeUnit.SECONDS)
        pool.shutdown()

        then:
        statuses.count(200) == 1
        statuses.count(409) == racers.size() - 1
        get(ASA, "/api/v1/events/${event.id}").body.bringItems[0].claimedBy.size() == 1
    }
}
