package com.personalworkspace.taskservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaskServiceEndpointsIT {

    private static final String DEFAULT_OWNER = "00000000-0000-0000-0000-000000000001";

    private final TestRestTemplate restTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    TaskServiceEndpointsIT(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @BeforeEach
    void decodeTestTokens() {
        given(jwtDecoder.decode(anyString())).willAnswer(invocation -> {
            String token = invocation.getArgument(0);
            boolean hasRole = !token.startsWith("no-role-");
            String subject = token.replace("no-role-", "");
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject(subject)
                    .issuedAt(Instant.now().minusSeconds(30))
                    .expiresAt(Instant.now().plusSeconds(300))
                    .claim("realm_access",
                            Map.of("roles", hasRole ? List.of("USER") : List.of()))
                    .build();
        });
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
        ResponseEntity<String> fetched = restTemplate.exchange(
                location, HttpMethod.GET, ownerRequest(DEFAULT_OWNER), String.class);
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
        ResponseEntity<String> duplicateList = restTemplate.postForEntity(
                "/api/v1/task-lists",
                jsonRequest("{\"name\":\"công việc\"}"),
                String.class);
        assertThat(duplicateList.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateList.getBody()).contains("Task list đã tồn tại");

        String listLocation = listCreated.getHeaders().getLocation().toString();
        String listId = listLocation.substring(listLocation.lastIndexOf('/') + 1);

        String taskBody = """
                {"title":"Task thuộc list","description":"integration","taskListId":"%s"}
                """.formatted(listId);
        ResponseEntity<String> taskCreated = restTemplate.postForEntity(
                "/api/v1/tasks", jsonRequest(taskBody), String.class);

        assertThat(taskCreated.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(taskCreated.getBody()).contains("\"taskListId\":\"" + listId + "\"");

        ResponseEntity<String> tasks = restTemplate.exchange(
                listLocation + "/tasks", HttpMethod.GET,
                ownerRequest(DEFAULT_OWNER), String.class);
        assertThat(tasks.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tasks.getBody()).contains("\"title\":\"Task thuộc list\"");
    }

    @Test
    void ownerCannotReadAnotherOwnersTask() {
        String ownerA = "10000000-0000-0000-0000-000000000001";
        String ownerB = "20000000-0000-0000-0000-000000000002";
        ResponseEntity<String> created = restTemplate.exchange(
                "/api/v1/tasks", HttpMethod.POST,
                ownerJsonRequest(ownerA, "{\"title\":\"Riêng tư\"}"), String.class);

        String location = created.getHeaders().getLocation().toString();
        ResponseEntity<String> hidden = restTemplate.exchange(
                location, HttpMethod.GET, ownerRequest(ownerB), String.class);
        ResponseEntity<String> visible = restTemplate.exchange(
                location, HttpMethod.GET, ownerRequest(ownerA), String.class);

        assertThat(hidden.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(visible.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(visible.getBody()).contains("\"ownerId\":\"" + ownerA + "\"");
    }

    @Test
    void filterPaginationTagsAndStatusPatchWorkTogether() {
        String owner = "30000000-0000-0000-0000-000000000003";
        ResponseEntity<String> tag = restTemplate.exchange(
                "/api/v1/task-tags", HttpMethod.POST,
                ownerJsonRequest(owner, "{\"name\":\"Backend\",\"color\":\"#7C3AED\"}"),
                String.class);
        assertThat(tag.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String tagId = tag.getHeaders().getLocation().toString();
        tagId = tagId.substring(tagId.lastIndexOf('/') + 1);

        ResponseEntity<String> duplicate = restTemplate.exchange(
                "/api/v1/task-tags", HttpMethod.POST,
                ownerJsonRequest(owner, "{\"name\":\"backend\"}"), String.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        String taskJson = """
                {
                  "title":"Thiết kế PostgreSQL",
                  "description":"PER-7 filter keyword",
                  "priority":"HIGH",
                  "dueAt":"2026-08-15T10:00:00Z",
                  "tagIds":["%s"]
                }
                """.formatted(tagId);
        ResponseEntity<String> created = restTemplate.exchange(
                "/api/v1/tasks", HttpMethod.POST, ownerJsonRequest(owner, taskJson), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String location = created.getHeaders().getLocation().toString();

        ResponseEntity<String> page = restTemplate.exchange(
                "/api/v1/tasks?priority=HIGH&tagId=" + tagId
                        + "&keyword=postgres&page=0&size=1&sort=dueAt,asc",
                HttpMethod.GET, ownerRequest(owner), String.class);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody())
                .contains("\"totalElements\":1")
                .contains("\"title\":\"Thiết kế PostgreSQL\"");

        ResponseEntity<String> patched = restTemplate.exchange(
                location + "/status", HttpMethod.PATCH,
                ownerJsonRequest(owner, "{\"status\":\"DONE\"}"), String.class);
        assertThat(patched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patched.getBody())
                .contains("\"status\":\"DONE\"")
                .contains("\"completedAt\":");

        ResponseEntity<String> fetched = restTemplate.exchange(
                location, HttpMethod.GET, ownerRequest(owner), String.class);
        assertThat(fetched.getBody()).doesNotContain("\"version\":0");
    }

    @Test
    void apiRequiresJwtAndExpectedRole() {
        ResponseEntity<String> noToken =
                restTemplate.getForEntity("/api/v1/tasks", String.class);
        ResponseEntity<String> wrongRole = restTemplate.exchange(
                "/api/v1/tasks", HttpMethod.GET,
                bearerRequest("no-role-" + DEFAULT_OWNER), String.class);

        assertThat(noToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongRole.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private HttpEntity<String> jsonRequest(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(DEFAULT_OWNER);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<String> ownerJsonRequest(String ownerId, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(ownerId);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> ownerRequest(String ownerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ownerId);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<Void> bearerRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
