package com.personalworkspace.taskservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaskServiceEndpointsIT {

    private final TestRestTemplate restTemplate;

    @Autowired
    TaskServiceEndpointsIT(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Test
    void healthEndpointReturnsUp() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void openApiDocumentIsAvailable() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"title\":\"Personal Workspace Task Service API\"")
                .contains("\"version\":\"v1\"")
                .contains("\"name\":\"Tasks\"")
                .contains("\"name\":\"Task Lists\"")
                .contains("\"summary\":\"Tạo task\"")
                .contains("\"summary\":\"Tạo task list\"")
                .contains("\"example\":\"Viết tài liệu API\"");
    }

    @Test
    void swaggerUiIsAvailable() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/swagger-ui.html", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsIgnoringCase("swagger");
    }

    @Test
    void taskCrudRespectsHttpAndPersistenceBoundaries() {
        String createBody = """
                {"title":"  Viết tài liệu  ","description":"PER-25"}
                """;
        ResponseEntity<String> created = restTemplate.postForEntity(
                "/api/v1/tasks", jsonRequest(createBody), String.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody())
                .contains("\"title\":\"Viết tài liệu\"")
                .contains("\"status\":\"TODO\"");

        String location = created.getHeaders().getLocation().toString();
        ResponseEntity<String> fetched = restTemplate.getForEntity(location, String.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).contains("\"description\":\"PER-25\"");
    }

    @Test
    void invalidTaskReturnsProblemDetails() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/tasks", jsonRequest("{\"title\":\"\"}"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType().toString())
                .contains("application/problem+json");
        assertThat(response.getBody())
                .contains("\"title\":\"Validation failed\"")
                .contains("\"field\":\"title\"");
    }

    @Test
    void taskListCrudAndTaskAssignmentWorkEndToEnd() {
        ResponseEntity<String> listCreated = restTemplate.postForEntity(
                "/api/v1/task-lists",
                jsonRequest("{\"name\":\"Công việc\",\"description\":\"Danh sách chính\"}"),
                String.class);

        assertThat(listCreated.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String listLocation = listCreated.getHeaders().getLocation().toString();
        String listId = listLocation.substring(listLocation.lastIndexOf('/') + 1);

        String taskBody = """
                {"title":"Task thuộc list","description":"integration","taskListId":"%s"}
                """.formatted(listId);
        ResponseEntity<String> taskCreated = restTemplate.postForEntity(
                "/api/v1/tasks", jsonRequest(taskBody), String.class);

        assertThat(taskCreated.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(taskCreated.getBody()).contains("\"taskListId\":\"" + listId + "\"");

        ResponseEntity<String> tasks =
                restTemplate.getForEntity(listLocation + "/tasks", String.class);
        assertThat(tasks.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tasks.getBody()).contains("\"title\":\"Task thuộc list\"");
    }

    private HttpEntity<String> jsonRequest(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
