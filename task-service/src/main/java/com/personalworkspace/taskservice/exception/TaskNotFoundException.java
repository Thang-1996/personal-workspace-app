package com.personalworkspace.taskservice.exception;

import java.util.UUID;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(UUID taskId) {
        super("Không tìm thấy task với id " + taskId);
    }
}
