package com.tulka.samvaer.platform.identity.infrastructure;

import java.util.UUID;

import com.tulka.samvaer.platform.identity.CurrentMember;
import com.tulka.samvaer.shared.error.ApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Reads the member that the authentication filter put on the request, and checks that the
 * member exists. With no filter active (login not decided yet) every call answers 401.
 */
@Component
class RequestCurrentMember implements CurrentMember {

    static final String ATTRIBUTE = RequestCurrentMember.class.getName() + ".memberId";

    private final MemberRepository members;

    RequestCurrentMember(MemberRepository members) {
        this.members = members;
    }

    @Override
    public UUID id() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object value = attributes == null ? null : attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (!(value instanceof UUID id) || !members.existsById(id)) {
            throw ApiException.notLoggedIn();
        }
        return id;
    }
}
