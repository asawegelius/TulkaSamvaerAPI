package com.tulka.samvaer.platform.identity.api;

import java.util.List;
import java.util.UUID;

import com.tulka.samvaer.platform.identity.Role;

public record MeResponse(UUID id, String displayName, String initials, UUID avatarMediaId, List<Role> roles) {
}
