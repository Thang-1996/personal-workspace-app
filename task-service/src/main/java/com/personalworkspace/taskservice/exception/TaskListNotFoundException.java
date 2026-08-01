package com.personalworkspace.taskservice.exception;

import java.util.UUID;

public class TaskListNotFoundException extends RuntimeException {

    public TaskListNotFoundException(UUID id) {
        super("Không tìm thấy task list với id " + id);
    }
}
