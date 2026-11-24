package com.tulka.samvaer.platform.identity;

import java.util.UUID;

/** The member making the current request. */
public interface CurrentMember {

    /**
     * The signed-in member's id.
     *
     * @throws com.tulka.samvaer.shared.error.ApiException 401 {@code auth.not-logged-in} when nobody is signed in
     */
    UUID id();
}
