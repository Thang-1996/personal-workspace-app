package com.personalworkspace.taskservice.dto.tasklist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dữ liệu tạo hoặc cập nhật task list")
public record TaskListRequest(
        @Schema(description = "Tên duy nhất của danh sách", example = "Công việc")
        @NotBlank(message = "name không được để trống")
        @Size(max = 100, message = "name tối đa 100 ký tự")
        String name,
        @Schema(description = "Mô tả danh sách", example = "Các công việc cần hoàn thành")
        @Size(max = 500, message = "description tối đa 500 ký tự")
        String description) {}
