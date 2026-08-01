package com.personalworkspace.taskservice.controller;

import com.personalworkspace.taskservice.dto.task.CreateTaskRequest;
import com.personalworkspace.taskservice.dto.task.TaskResponse;
import com.personalworkspace.taskservice.dto.task.UpdateTaskRequest;
import com.personalworkspace.taskservice.dto.task.ChangeTaskStatusRequest;
import com.personalworkspace.taskservice.dto.task.TaskFilter;
import com.personalworkspace.taskservice.entity.TaskPriority;
import com.personalworkspace.taskservice.entity.TaskStatus;
import com.personalworkspace.taskservice.service.TaskService;
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

    private static final String DEV_OWNER = "00000000-0000-0000-0000-000000000001";
    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Tạo task", description = "Task mới có trạng thái mặc định TODO.")
    @ApiResponse(responseCode = "201", description = "Đã tạo task")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public ResponseEntity<TaskResponse> create(
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.create(ownerId, request);
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + created.id())).body(created);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Lấy chi tiết task")
    @ApiResponse(responseCode = "200", description = "Tìm thấy task")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public TaskResponse get(
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId,
            @PathVariable UUID taskId) {
        return taskService.get(ownerId, taskId);
    }

    @GetMapping
    @Operation(summary = "Liệt kê task", description = "Có thể lọc theo trạng thái; bỏ trống để lấy tất cả.")
    public Page<TaskResponse> list(
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId,
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
        return taskService.list(ownerId,
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
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId,
            @PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(ownerId, taskId, request);
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Thay đổi trạng thái task")
    public TaskResponse changeStatus(
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId,
            @PathVariable UUID taskId,
            @Valid @RequestBody ChangeTaskStatusRequest request) {
        return taskService.changeStatus(ownerId, taskId, request.status());
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Xóa task")
    @ApiResponse(responseCode = "204", description = "Đã xóa task")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public ResponseEntity<Void> delete(
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId,
            @PathVariable UUID taskId) {
        taskService.delete(ownerId, taskId);
        return ResponseEntity.noContent().build();
    }
}
