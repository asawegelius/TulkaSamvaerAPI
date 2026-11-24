package com.tulka.samvaer.platform.groups.api;

import java.util.UUID;

public record GroupResponse(UUID id, String name, int memberCount, boolean mine) {
}
