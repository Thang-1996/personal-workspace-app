package com.personalworkspace.fileservice.event;

import java.time.Instant;
import java.util.UUID;

public final class FileEvents {

    private FileEvents() {
    }

    public record FileUploaded(
            UUID fileId,
            UUID ownerId,
            String contentType,
            long sizeBytes,
            Instant occurredAt) {
    }

    public record FileDeleted(UUID fileId, UUID ownerId, Instant occurredAt) {
    }
}
