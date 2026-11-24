/**
 * Members, roles and the signed-in member. Login itself is still open (ADR-005); until it is
 * decided, {@code infrastructure.StubAuthenticationFilter} identifies the caller in dev and tests.
 */
@ApplicationModule(displayName = "Identity")
package com.tulka.samvaer.platform.identity;

import org.springframework.modulith.ApplicationModule;
