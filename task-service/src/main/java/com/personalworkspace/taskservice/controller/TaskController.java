package com.personalworkspace.taskservice.controller;

import com.personalworkspace.taskservice.dto.task.CreateTaskRequest;
import com.personalworkspace.taskservice.dto.task.TaskResponse;
import com.personalworkspace.taskservice.dto.task.UpdateTaskRequest;
import com.personalworkspace.taskservice.dto.task.ChangeTaskStatusRequest;
import com.personalworkspace.taskservice.dto.task.TaskFilter;
import com.personalworkspace.taskservice.entity.TaskPriority;
import com.personalworkspace.taskservice.entity.TaskStatus;
import com.personalworkspace.taskservice.service.TaskService;
import com.personalworkspace.taskservice.security.AuthenticatedOwner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter mỏng: bind/validate request, gọi service và chọn HTTP status/header. Không truy
 * cập repository, không mở transaction và không chứa business rule.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Tạo, tra cứu, cập nhật và xóa công việc")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Tạo task", description = "Task mới có trạng thái mặc định TODO.")
    @ApiResponse(responseCode = "201", description = "Đã tạo task")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public ResponseEntity<TaskResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTaskRequest request) {
        UUID ownerId = AuthenticatedOwner.from(jwt);
        TaskResponse created = taskService.create(ownerId, request);
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + created.id())).body(created);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Lấy chi tiết task")
    @ApiResponse(responseCode = "200", description = "Tìm thấy task")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public TaskResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID taskId) {
        return taskService.get(AuthenticatedOwner.from(jwt), taskId);
    }

    @GetMapping
    @Operation(summary = "Liệt kê task", description = "Có thể lọc theo trạng thái; bỏ trống để lấy tất cả.")
    public Page<TaskResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Trạng thái cần lọc", example = "TODO")
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID listId,
            @RequestParam(required = false) UUID tagId,
            @RequestParam(required = false) Instant dueFrom,
            @RequestParam(required = false) Instant dueTo,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return taskService.list(AuthenticatedOwner.from(jwt),
                new TaskFilter(status, priority, listId, tagId, dueFrom, dueTo, keyword),
                pageable);
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Cập nhật toàn bộ task")
    @ApiResponse(responseCode = "200", description = "Đã cập nhật task")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task hoặc task list",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public TaskResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(AuthenticatedOwner.from(jwt), taskId, request);
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Thay đổi trạng thái task")
    public TaskResponse changeStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID taskId,
            @Valid @RequestBody ChangeTaskStatusRequest request) {
        return taskService.changeStatus(AuthenticatedOwner.from(jwt), taskId, request.status());
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Xóa task")
    @ApiResponse(responseCode = "204", description = "Đã xóa task")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID taskId) {
        taskService.delete(AuthenticatedOwner.from(jwt), taskId);
        return ResponseEntity.noContent().build();
    }
}
