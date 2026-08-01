package com.personalworkspace.taskservice;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TaskServiceApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationContextStartsAndProvidesOpenApiMetadata() {
        OpenAPI openAPI = applicationContext.getBean(OpenAPI.class);

        assertThat(openAPI.getInfo().getTitle())
                .isEqualTo("Personal Workspace Task Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
    }
}
