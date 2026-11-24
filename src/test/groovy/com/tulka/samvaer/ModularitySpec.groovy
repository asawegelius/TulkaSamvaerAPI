package com.tulka.samvaer

import org.springframework.modulith.core.ApplicationModules
import spock.lang.Specification

/** ADR-001: the build fails when a module reaches into another module's internals. */
class ModularitySpec extends Specification {

    def modules = ApplicationModules.of(TulkaSamvaerApiApplication)

    def "module boundaries hold"() {
        when:
        modules.verify()

        then:
        noExceptionThrown()
    }

    def "sections and platform packages are the modules"() {
        expect:
        modules*.basePackage*.name as Set == [
            'com.tulka.samvaer.shared',
            'com.tulka.samvaer.platform.identity',
            'com.tulka.samvaer.platform.groups',
            'com.tulka.samvaer.platform.visibility',
            'com.tulka.samvaer.sections.events',
        ] as Set
    }
}
