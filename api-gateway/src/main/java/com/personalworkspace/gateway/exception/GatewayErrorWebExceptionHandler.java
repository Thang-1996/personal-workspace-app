package com.personalworkspace.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalworkspace.gateway.filter.CorrelationIdFilter;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GatewayErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(error);
        }

        HttpStatus status = resolveStatus(error);
        String correlationId =
                exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME);
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "https://personal-workspace.dev/problems/downstream");
        problem.put("title", status.getReasonPhrase());
        problem.put("status", status.value());
        problem.put("detail", detail(status));
        problem.put("instance", exchange.getRequest().getPath().value());
        problem.put("correlationId", correlationId);
        problem.put("timestamp", Instant.now().toString());

        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(problem);
        } catch (Exception serializationError) {
            body = "{\"title\":\"Gateway error\",\"status\":502}"
                    .getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private HttpStatus resolveStatus(Throwable error) {
        if (contains(error, TimeoutException.class)) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (contains(error, ConnectException.class)) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (error instanceof ResponseStatusException responseStatus
                && responseStatus.getStatusCode() instanceof HttpStatus httpStatus) {
            return httpStatus;
        }
        return HttpStatus.BAD_GATEWAY;
    }

    private String detail(HttpStatus status) {
        return status == HttpStatus.GATEWAY_TIMEOUT
                ? "Downstream service did not respond before the configured timeout."
                : "Downstream service is unavailable or rejected the gateway request.";
    }

    private boolean contains(Throwable error, Class<? extends Throwable> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
