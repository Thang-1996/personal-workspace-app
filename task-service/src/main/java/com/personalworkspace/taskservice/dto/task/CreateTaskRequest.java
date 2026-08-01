package com.personalworkspace.taskservice.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
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
        UUID taskListId) {}
