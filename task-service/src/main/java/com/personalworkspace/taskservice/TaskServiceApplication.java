package com.personalworkspace.taskservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động của Task Service.
 *
 * <p>{@link SpringBootApplication} gộp ba vai trò: đánh dấu lớp cấu hình, bật cơ chế tự động
 * cấu hình của Spring Boot và quét component từ package hiện tại trở xuống. Vì lớp này nằm ở
 * package gốc {@code com.personalworkspace.taskservice}, các layer như {@code controller},
 * {@code service}, {@code repository}, {@code entity}, {@code dto}, {@code exception} và
 * {@code configuration} đều được tìm thấy.
 */
@SpringBootApplication
public class TaskServiceApplication {

    private TaskServiceApplication() {
        // Không tạo instance: Spring Boot chỉ cần lớp này làm nguồn cấu hình.
    }

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }
}
