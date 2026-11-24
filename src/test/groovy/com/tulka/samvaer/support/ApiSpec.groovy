package com.tulka.samvaer.support

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Shared
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Base for API tests: the whole app on H2 with the sample members and groups, stub login, and
 * every response checked against openapi.yaml.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
abstract class ApiSpec extends Specification {

    static UUID id(int n) { UUID.fromString(String.format('00000000-0000-4000-8000-%012d', n)) }

    // Sample members (same ids as the frontend mock)
    static final UUID ASA = id(1), JONAS = id(2), SOFIE = id(3), KASPER = id(4), MAJA = id(5), PETER = id(6),
                      HENRIK = id(7), LINE = id(8), MORMOR = id(9), MORFAR = id(10), JENS = id(11), EMIL = id(12),
                      NORA = id(13)
    // Family: Åsa, Line, Mormor, Morfar, Jens. Best friends: Åsa, Jonas, Sofie, Kasper, Maja, Peter, Henrik.
    // Cousins (made by Line): Åsa, Line, Emil, Nora.
    static final UUID FAMILY = id(101), BEST_FRIENDS = id(102), COUSINS = id(103)

    static final ZoneId HOME = ZoneId.of('Europe/Copenhagen')

    @Shared
    static ContractValidator contract = new ContractValidator()

    @Autowired
    MockMvc mvc

    @Autowired
    JdbcTemplate jdbc

    def setup() {
        ['events_claim', 'events_bring_item', 'events_answer', 'events_event_group', 'events_programme_item',
         'events_sleeping_guest', 'events_sleeping_spot', 'events_event', 'events_series'].each {
            jdbc.update("delete from $it")
        }
    }

    Response get(UUID as, String path) { call(as, 'GET', path, null) }

    Response post(UUID as, String path, Object body) { call(as, 'POST', path, body) }

    Response put(UUID as, String path, Object body = null) { call(as, 'PUT', path, body) }

    Response delete(UUID as, String path) { call(as, 'DELETE', path, null) }

    Response call(UUID as, String method, String path, Object body) {
        def request = MockMvcRequestBuilders.request(method, URI.create(path))
        if (as != null) {
            request.header('X-Stub-Member-Id', as.toString())
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON)
                    .content(body instanceof String ? body : JsonOutput.toJson(body))
        }
        def result = mvc.perform(request).andReturn().response
        result.characterEncoding = 'UTF-8'
        String text = result.contentAsString
        contract.check(method, path, result.status, result.contentType, text)
        new Response(status: result.status, body: text ? new JsonSlurper().parseText(text) : null,
                location: result.getHeader('Location'))
    }

    /** A valid create body; override fields with named arguments. */
    static Map eventBody(Map overrides = [:]) {
        def start = LocalDate.now(HOME).plusDays(10).atTime(18, 0).atZone(HOME).toInstant()
        [
            kind      : 'GATHERING',
            title     : 'Board game weekend',
            startsAt  : start.toString(),
            endsAt    : start.plusSeconds(2 * 24 * 3600).toString(),
            allDay    : false,
            location  : 'Our house',
            visibility: [scope: 'GROUPS', groupIds: [BEST_FRIENDS.toString()]],
        ] + overrides
    }

    /** Creates an event as {@code host} and returns its body. */
    Map createEvent(UUID host, Map overrides = [:]) {
        def response = post(host, '/api/v1/events', eventBody(overrides))
        assert response.status == 201: response.body
        response.body as Map
    }

    static Instant at(LocalDate date, int hour, int minute = 0) {
        date.atTime(hour, minute).atZone(HOME).toInstant()
    }

    static class Response {
        int status
        Object body
        String location

        String getCode() { (body as Map)?.code }
    }
}
