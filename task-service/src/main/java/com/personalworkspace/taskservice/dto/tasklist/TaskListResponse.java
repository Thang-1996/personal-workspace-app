package com.personalworkspace.taskservice.dto.tasklist;

import com.personalworkspace.taskservice.entity.TaskList;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Thông tin task list")
public record TaskListResponse(
        @Schema(example = "8f4f77c0-5d25-4df7-b353-d023ab92a565")
        UUID id,
        @Schema(example = "Công việc")
        String name,
        @Schema(example = "Các công việc cần hoàn thành")
        String description,
        @Schema(description = "Phiên bản optimistic locking", example = "0")
        long version,
        @Schema(example = "2026-08-01T03:00:00Z")
        Instant createdAt,
        @Schema(example = "2026-08-01T03:00:00Z")
        Instant updatedAt) {

    public static TaskListResponse from(TaskList entity) {
        return new TaskListResponse(
                entity.getId(), entity.getName(), entity.getDescription(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
