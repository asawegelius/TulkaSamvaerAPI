package com.tulka.samvaer.platform.identity.infrastructure;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * STAND-IN FOR LOGIN while ADR-005 is open. Trusts the {@code X-Stub-Member-Id} header, or falls
 * back to {@code samvaer.auth.stub.default-member-id}. Anyone can claim to be anyone, so this is
 * only switched on by the {@code dev} profile and in tests ({@code samvaer.auth.mode=stub}).
 * Replace with Spring Security once the login method is chosen; {@code CurrentMember} stays.
 */
@Component
@ConditionalOnProperty(name = "samvaer.auth.mode", havingValue = "stub")
public class StubAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Stub-Member-Id";

    private static final Logger LOGGER = LoggerFactory.getLogger(StubAuthenticationFilter.class);

    private final Optional<UUID> defaultMemberId;

    public StubAuthenticationFilter(@Value("${samvaer.auth.stub.default-member-id:}") String defaultMemberId) {
        this.defaultMemberId = parse(defaultMemberId);
        LOGGER.warn("Stub authentication is ON: callers choose who they are with the {} header. "
                + "Never run this outside local development.", HEADER);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<UUID> member = parse(request.getHeader(HEADER)).or(() -> defaultMemberId);
        member.ifPresent(id -> request.setAttribute(RequestCurrentMember.ATTRIBUTE, id));
        chain.doFilter(request, response);
    }

    private static Optional<UUID> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.strip()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
