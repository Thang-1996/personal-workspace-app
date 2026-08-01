package com.personalworkspace.taskservice.dto.task;

import com.personalworkspace.taskservice.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeTaskStatusRequest(
        @NotNull(message = "status không được null")
        @Schema(example = "DONE")
        TaskStatus status) {}
