package com.tulka.samvaer.platform

import com.tulka.samvaer.support.ApiSpec

class PlatformApiSpec extends ApiSpec {

    def "nobody signed in answers 401 with auth.not-logged-in"() {
        when:
        def response = get(null, '/api/v1/me')

        then:
        response.status == 401
        response.code == 'auth.not-logged-in'
    }

    def "an unknown member id is not signed in"() {
        expect:
        get(UUID.randomUUID(), '/api/v1/events').status == 401
    }

    def "me returns the member and roles"() {
        when:
        def me = get(ASA, '/api/v1/me')

        then:
        me.status == 200
        me.body.displayName == 'Åsa'
        me.body.roles == ['ADMIN', 'INNER_CIRCLE', 'MEMBER']
    }

    def "my groups are the groups I belong to, sorted by name, with mine flagged"() {
        when:
        def groups = get(ASA, '/api/v1/me/groups').body

        then:
        groups*.name == ['Best friends', 'Cousins', 'Family']
        groups.find { it.name == 'Cousins' }.mine == false
        groups.find { it.name == 'Family' }.mine == true
        groups.find { it.name == 'Family' }.memberCount == 5

        and:
        get(EMIL, '/api/v1/me/groups').body*.name == ['Cousins']
    }

    def "members are sorted the Danish way and can be searched"() {
        when:
        def all = get(JONAS, '/api/v1/members').body*.displayName

        then: 'Å sorts after Z'
        all.first() == 'Emil'
        all.last() == 'Åsa'
        all.size() == 13

        and:
        get(JONAS, '/api/v1/members?q=mor').body*.displayName == ['Morfar', 'Mormor']
    }
}
