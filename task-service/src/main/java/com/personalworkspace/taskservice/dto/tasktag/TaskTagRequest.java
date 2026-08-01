package com.personalworkspace.taskservice.dto.tasktag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskTagRequest(
        @NotBlank @Size(max = 100)
        @Schema(example = "Backend")
        String name,
        @Size(max = 20)
        @Schema(example = "#7C3AED")
        String color) {}
