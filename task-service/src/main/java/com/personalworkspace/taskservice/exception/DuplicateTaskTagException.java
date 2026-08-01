package com.personalworkspace.taskservice.exception;

public class DuplicateTaskTagException extends RuntimeException {
    public DuplicateTaskTagException(String name) {
        super("Task tag đã tồn tại: " + name);
    }
}
