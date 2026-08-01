package com.personalworkspace.taskservice.entity;

/**
 * Trạng thái vòng đời tối thiểu của task. Enum ngăn domain nhận chuỗi tùy ý; lưu tên enum
 * trong database an toàn hơn ordinal khi thứ tự enum thay đổi.
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}
