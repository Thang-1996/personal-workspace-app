package com.personalworkspace.taskservice.dto.task;

import com.personalworkspace.taskservice.entity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import java.time.Instant;
import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dữ liệu tạo task")
public record CreateTaskRequest(
        @Schema(description = "Tiêu đề công việc", example = "Viết tài liệu API")
        @NotBlank(message = "title không được để trống")
        @Size(max = 200, message = "title tối đa 200 ký tự")
        String title,
        @Schema(description = "Nội dung chi tiết", example = "Mô tả và ví dụ cho các endpoint")
        @Size(max = 2000, message = "description tối đa 2000 ký tự")
        String description,
        @Schema(description = "Task list chứa task; bỏ trống nếu chưa phân nhóm",
                example = "8f4f77c0-5d25-4df7-b353-d023ab92a565")
        UUID taskListId,
        @Schema(description = "Độ ưu tiên", example = "HIGH")
        TaskPriority priority,
        @Schema(description = "Hạn hoàn thành theo UTC", example = "2026-08-15T10:00:00Z")
        Instant dueAt,
        @Schema(description = "Vị trí thủ công trong danh sách", example = "0")
        int position,
        @Schema(description = "Các tag gắn vào task")
        Set<UUID> tagIds) {}
