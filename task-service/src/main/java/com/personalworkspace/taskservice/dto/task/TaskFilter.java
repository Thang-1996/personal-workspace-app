package com.personalworkspace.taskservice.dto.task;

import com.personalworkspace.taskservice.entity.TaskPriority;
import com.personalworkspace.taskservice.entity.TaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskFilter(
        TaskStatus status,
        TaskPriority priority,
        UUID listId,
        UUID tagId,
        Instant dueFrom,
        Instant dueTo,
        String keyword) {}
