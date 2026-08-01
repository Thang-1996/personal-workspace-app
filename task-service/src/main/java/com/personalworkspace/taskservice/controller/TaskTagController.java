package com.personalworkspace.taskservice.controller;

import com.personalworkspace.taskservice.dto.tasktag.TaskTagRequest;
import com.personalworkspace.taskservice.dto.tasktag.TaskTagResponse;
import com.personalworkspace.taskservice.service.TaskTagService;
import com.personalworkspace.taskservice.security.AuthenticatedOwner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/task-tags")
@RequiredArgsConstructor
@Tag(name = "Task Tags", description = "Quản lý nhãn công việc theo owner")
public class TaskTagController {

    private final TaskTagService service;

    @PostMapping
    @Operation(summary = "Tạo task tag")
    public ResponseEntity<TaskTagResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskTagRequest request) {
        UUID ownerId = AuthenticatedOwner.from(jwt);
        TaskTagResponse created = service.create(ownerId, request);
        return ResponseEntity.created(URI.create("/api/v1/task-tags/" + created.id()))
                .body(created);
    }

    @GetMapping
    @Operation(summary = "Liệt kê task tag của owner")
    public List<TaskTagResponse> list(
            @AuthenticationPrincipal Jwt jwt) {
        return service.list(AuthenticatedOwner.from(jwt));
    }
}
