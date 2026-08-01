package com.personalworkspace.taskservice.dto.tasklist;

import jakarta.validation.constraints.Size;

/** Null nghĩa là giữ nguyên; giá trị false/0 vẫn là cập nhật hợp lệ. */
public record PatchTaskListRequest(
        @Size(min = 1, max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 20) String color,
        Integer position,
        Boolean archived) {}
