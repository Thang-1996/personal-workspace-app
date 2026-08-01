package com.personalworkspace.taskservice.dto.task;

import com.personalworkspace.taskservice.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Dữ liệu thay thế một task")
public record UpdateTaskRequest(
        @Schema(description = "Tiêu đề công việc", example = "Hoàn thiện tài liệu API")
        @NotBlank(message = "title không được để trống")
        @Size(max = 200, message = "title tối đa 200 ký tự")
        String title,
        @Schema(description = "Nội dung chi tiết", example = "Đã bổ sung ví dụ Swagger")
        @Size(max = 2000, message = "description tối đa 2000 ký tự")
        String description,
        @Schema(description = "Trạng thái hiện tại", example = "IN_PROGRESS")
        @NotNull(message = "status không được null")
        TaskStatus status,
        @Schema(description = "Task list chứa task; null để bỏ phân nhóm",
                example = "8f4f77c0-5d25-4df7-b353-d023ab92a565")
        UUID taskListId) {}
