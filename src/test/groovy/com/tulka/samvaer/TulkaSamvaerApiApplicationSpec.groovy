package com.tulka.samvaer

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
class TulkaSamvaerApiApplicationSpec extends Specification {

    def "context loads"() {
        expect:
        true
    }
}
