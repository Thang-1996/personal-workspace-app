package com.personalworkspace.taskservice.exception;

public class DuplicateTaskListException extends RuntimeException {

    public DuplicateTaskListException(String name) {
        super("Task list đã tồn tại với tên " + name);
    }
}
