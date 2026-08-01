package com.personalworkspace.taskservice.dto.task;

import com.personalworkspace.taskservice.entity.Task;
import com.personalworkspace.taskservice.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Immutable API contract tách JSON response khỏi mutable JPA entity. */
@Schema(description = "Thông tin task")
public record TaskResponse(
        @Schema(example = "d922f289-0e39-41d7-96db-e425b2363d85")
        UUID id,
        @Schema(example = "Viết tài liệu API")
        String title,
        @Schema(example = "Tài liệu Swagger cho Task Service")
        String description,
        @Schema(example = "TODO")
        TaskStatus status,
        @Schema(description = "Phiên bản optimistic locking", example = "0")
        long version,
        UUID taskListId,
        @Schema(example = "2026-08-01T03:00:00Z")
        Instant createdAt,
        @Schema(example = "2026-08-01T03:00:00Z")
        Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                task.getVersion(),
                task.getTaskList() == null ? null : task.getTaskList().getId(),
                task.getCreatedAt(), task.getUpdatedAt());
    }
}
