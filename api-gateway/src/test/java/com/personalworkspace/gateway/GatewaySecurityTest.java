package com.personalworkspace.gateway;

import static org.mockito.BDDMockito.given;

import com.personalworkspace.gateway.filter.CorrelationIdFilter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewaySecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RouteLocator routeLocator;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @BeforeEach
    void decodeWrongRoleToken() {
        Jwt jwt = Jwt.withTokenValue("wrong-role")
                .header("alg", "none")
                .subject("11111111-1111-1111-1111-111111111111")
                .issuedAt(Instant.now().minusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("aud", List.of("workspace-api"))
                .claim("realm_access", Map.of("roles", List.of()))
                .build();
        given(jwtDecoder.decode("wrong-role")).willReturn(Mono.just(jwt));
    }

    @Test
    void apiWithoutTokenReturnsUnauthorized() {
        webTestClient.get().uri("/api/v1/tasks").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void authenticatedTokenWithoutRequiredRoleReturnsForbidden() {
        webTestClient.get().uri("/api/v1/tasks")
                .headers(headers -> headers.setBearerAuth("wrong-role"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void gatewayContainsTaskAndFileRoutes() {
        List<String> routeIds = routeLocator.getRoutes().map(route -> route.getId())
                .collectList().block();

        org.assertj.core.api.Assertions.assertThat(routeIds)
                .contains("task-service", "file-service");
    }

    @Test
    void correlationIdIsReturnedEvenWhenRequestIsUnauthorized() {
        webTestClient.get().uri("/api/v1/tasks")
                .header(CorrelationIdFilter.HEADER_NAME, "test-correlation")
                .exchange()
                .expectHeader().valueEquals(
                        CorrelationIdFilter.HEADER_NAME, "test-correlation")
                .expectHeader().exists(HttpHeaders.WWW_AUTHENTICATE);
    }
}
