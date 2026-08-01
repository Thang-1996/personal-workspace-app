package com.personalworkspace.taskservice.dto.tasktag;

import java.time.Instant;
import java.util.UUID;

public record TaskTagResponse(
        UUID id, UUID ownerId, String name, String color, Instant createdAt) {}
