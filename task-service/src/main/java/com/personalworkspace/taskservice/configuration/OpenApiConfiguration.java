package com.personalworkspace.taskservice.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata chung được hiển thị trong Swagger UI và tài liệu OpenAPI JSON.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    /**
     * Khai báo bằng {@code @Bean} để Spring quản lý một instance OpenAPI dùng chung trong toàn
     * application context. Phương thức không chứa nghiệp vụ; nó chỉ mô tả API.
     */
    @Bean
    OpenAPI taskServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Personal Workspace Task Service API")
                        .version("v1")
                        .description("API quản lý task và task list của Personal Workspace."));
    }
}
