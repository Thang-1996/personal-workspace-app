package com.personalworkspace.fileservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.personalworkspace.fileservice.storage.ObjectStorage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileServiceEndpointsTest {

    private static final String OWNER_A = "10000000-0000-0000-0000-000000000001";
    private static final String OWNER_B = "20000000-0000-0000-0000-000000000002";

    private final TestRestTemplate restTemplate;
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ObjectStorage objectStorage;

    @Autowired
    FileServiceEndpointsTest(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @BeforeEach
    void configureTestDoubles() throws Exception {
        objects.clear();
        given(jwtDecoder.decode(anyString())).willAnswer(invocation -> {
            String token = invocation.getArgument(0);
            boolean hasRole = !token.startsWith("no-role-");
            String subject = token.replace("no-role-", "");
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject(subject)
                    .issuedAt(Instant.now().minusSeconds(30))
                    .expiresAt(Instant.now().plusSeconds(300))
                    .claim("realm_access", Map.of(
                            "roles", hasRole ? List.of("USER") : List.of()))
                    .build();
        });
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            objects.put(key, invocation.<java.io.InputStream>getArgument(1).readAllBytes());
            return null;
        }).when(objectStorage).put(anyString(), any(), anyLong(), anyString());
        given(objectStorage.get(anyString())).willAnswer(invocation -> {
            byte[] content = objects.get(invocation.getArgument(0));
            return new ByteArrayInputStream(content);
        });
        doAnswer(invocation -> {
            objects.remove(invocation.getArgument(0));
            return null;
        }).when(objectStorage).delete(anyString());
    }

    @Test
    void healthIsPublicButFileApisRequireExpectedRole() {
        ResponseEntity<String> health =
                restTemplate.getForEntity("/actuator/health", String.class);
        ResponseEntity<String> anonymous =
                restTemplate.getForEntity("/api/v1/files", String.class);
        ResponseEntity<String> wrongRole = restTemplate.exchange(
                "/api/v1/files",
                HttpMethod.GET,
                bearer("no-role-" + OWNER_A),
                String.class);

        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongRole.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void uploadDownloadLinkAndDeleteWorkForOwner() throws Exception {
        ResponseEntity<String> uploaded = upload(OWNER_A, "notes.txt", "hello workspace");

        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(uploaded.getBody())
                .contains("\"originalName\":\"notes.txt\"")
                .contains("\"contentType\":\"text/plain\"")
                .contains("\"status\":\"READY\"");
        String fileId = jsonField(uploaded.getBody(), "id");

        ResponseEntity<byte[]> downloaded = restTemplate.exchange(
                "/api/v1/files/" + fileId + "/download",
                HttpMethod.GET,
                bearer(OWNER_A),
                byte[].class);
        assertThat(downloaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloaded.getBody()).isEqualTo("hello workspace".getBytes());

        UUID taskId = UUID.randomUUID();
        ResponseEntity<Void> linked = restTemplate.exchange(
                "/api/v1/files/" + fileId + "/links/tasks/" + taskId,
                HttpMethod.POST,
                bearer(OWNER_A),
                Void.class);
        assertThat(linked.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/v1/files/" + fileId,
                HttpMethod.DELETE,
                bearer(OWNER_A),
                Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(objectStorage).delete(anyString());

        ResponseEntity<String> missing = restTemplate.exchange(
                "/api/v1/files/" + fileId + "/download",
                HttpMethod.GET,
                bearer(OWNER_A),
                String.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void filesAndFoldersAreIsolatedByOwner() {
        ResponseEntity<String> folder = restTemplate.exchange(
                "/api/v1/folders",
                HttpMethod.POST,
                json(OWNER_A, "{\"name\":\"Private\"}"),
                String.class);
        assertThat(folder.getStatusCode()).isEqualTo(HttpStatus.OK);
        String folderId = jsonField(folder.getBody(), "id");

        ResponseEntity<String> uploaded = upload(OWNER_A, "private.txt", "secret");
        String fileId = jsonField(uploaded.getBody(), "id");

        ResponseEntity<String> otherOwnerFile = restTemplate.exchange(
                "/api/v1/files/" + fileId + "/download",
                HttpMethod.GET,
                bearer(OWNER_B),
                String.class);
        ResponseEntity<String> otherOwnerFolders = restTemplate.exchange(
                "/api/v1/folders?parentId=" + folderId,
                HttpMethod.GET,
                bearer(OWNER_B),
                String.class);

        assertThat(otherOwnerFile.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(otherOwnerFolders.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(otherOwnerFolders.getBody()).isEqualTo("[]");
    }

    @Test
    void serverDetectedMimeTypeRejectsSpoofedExecutable() {
        ResponseEntity<String> response = upload(
                OWNER_A,
                "malware.pdf",
                "MZ\u0090\u0000fake executable");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).contains("Detected MIME type is not allowed");
        assertThat(objects).isEmpty();
    }

    private ResponseEntity<String> upload(String owner, String filename, String content) {
        ByteArrayResource resource = new ByteArrayResource(content.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        HttpHeaders headers = bearerHeaders(owner);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return restTemplate.postForEntity(
                "/api/v1/files/upload",
                new HttpEntity<>(body, headers),
                String.class);
    }

    private HttpEntity<Void> bearer(String owner) {
        return new HttpEntity<>(bearerHeaders(owner));
    }

    private HttpEntity<String> json(String owner, String body) {
        HttpHeaders headers = bearerHeaders(owner);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders bearerHeaders(String owner) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(owner);
        return headers;
    }

    private String jsonField(String json, String field) {
        String prefix = "\"" + field + "\":\"";
        int start = json.indexOf(prefix) + prefix.length();
        return json.substring(start, json.indexOf('"', start));
    }
}
