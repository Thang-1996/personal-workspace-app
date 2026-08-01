package com.personalworkspace.taskservice.exception;

import java.net.URI;

/**
 * URI ổn định dùng để phân loại Problem Details. Client có thể dựa vào type thay vì so sánh
 * chuỗi message vốn có thể thay đổi hoặc được dịch.
 */
public final class ApiProblemType {

    public static final URI VALIDATION_ERROR =
            URI.create("https://personal-workspace.example/problems/validation-error");
    public static final URI RESOURCE_NOT_FOUND =
            URI.create("https://personal-workspace.example/problems/resource-not-found");
    public static final URI CONFLICT =
            URI.create("https://personal-workspace.example/problems/conflict");
    public static final URI INTERNAL_ERROR =
            URI.create("https://personal-workspace.example/problems/internal-error");

    private ApiProblemType() {
        // Utility class chỉ chứa hằng số.
    }
}
