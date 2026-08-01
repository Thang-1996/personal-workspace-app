package com.personalworkspace.taskservice.dto.task;

import com.personalworkspace.taskservice.entity.TaskPriority;
import com.personalworkspace.taskservice.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import java.util.Set;

/** Immutable API contract tách JSON response khỏi mutable JPA entity. */
@Schema(description = "Thông tin task")
public record TaskResponse(
        @Schema(example = "d922f289-0e39-41d7-96db-e425b2363d85")
        UUID id,
        UUID ownerId,
        @Schema(example = "Viết tài liệu API")
        String title,
        @Schema(example = "Tài liệu Swagger cho Task Service")
        String description,
        @Schema(example = "TODO")
        TaskStatus status,
        TaskPriority priority,
        Instant dueAt,
        Instant completedAt,
        int position,
        @Schema(description = "Phiên bản optimistic locking", example = "0")
        long version,
        UUID taskListId,
        Set<UUID> tagIds,
        @Schema(example = "2026-08-01T03:00:00Z")
        Instant createdAt,
        @Schema(example = "2026-08-01T03:00:00Z")
        Instant updatedAt) {}
