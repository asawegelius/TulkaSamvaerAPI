package com.tulka.samvaer

import com.tulka.samvaer.support.ContractValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import spock.lang.Specification

/** Every operation in openapi.yaml has an endpoint here, and every /api endpoint is in openapi.yaml. */
@SpringBootTest
@ActiveProfiles('test')
class ContractCoverageSpec extends Specification {

    @Autowired
    @Qualifier('requestMappingHandlerMapping')
    RequestMappingHandlerMapping mappings

    def "implemented endpoints match the contract"() {
        given:
        def contract = new ContractValidator().operations()
        def implemented = [] as Set<String>
        mappings.handlerMethods.keySet().each { info ->
            info.pathPatternsCondition?.patternValues?.findAll { it.startsWith('/api/') }?.each { path ->
                info.methodsCondition.methods.each { implemented << "${it} ${path}".toString() }
            }
        }

        expect:
        contract - implemented == [] as Set
        implemented - contract == [] as Set
    }
}
