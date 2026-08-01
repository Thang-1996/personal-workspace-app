package com.personalworkspace.taskservice.dto.tasklist;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Thông tin task list")
public record TaskListResponse(
        @Schema(example = "8f4f77c0-5d25-4df7-b353-d023ab92a565")
        UUID id,
        UUID ownerId,
        @Schema(example = "Công việc")
        String name,
        @Schema(example = "Các công việc cần hoàn thành")
        String description,
        String color,
        int position,
        boolean archived,
        @Schema(description = "Phiên bản optimistic locking", example = "0")
        long version,
        @Schema(example = "2026-08-01T03:00:00Z")
        Instant createdAt,
        @Schema(example = "2026-08-01T03:00:00Z")
        Instant updatedAt) {}
