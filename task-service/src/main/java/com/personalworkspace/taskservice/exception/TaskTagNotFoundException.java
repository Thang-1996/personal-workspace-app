package com.personalworkspace.taskservice.exception;

import java.util.UUID;

public class TaskTagNotFoundException extends RuntimeException {
    public TaskTagNotFoundException(UUID id) {
        super("Không tìm thấy task tag: " + id);
    }
}
