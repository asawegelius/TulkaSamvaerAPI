package com.tulka.samvaer

import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest
class TulkaSamvaerApiApplicationSpec extends Specification {

    def "context loads"() {
        expect:
        true
    }
}
