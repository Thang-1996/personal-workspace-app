package com.personalworkspace.taskservice.controller;

import com.personalworkspace.taskservice.dto.task.TaskResponse;
import com.personalworkspace.taskservice.dto.tasklist.TaskListRequest;
import com.personalworkspace.taskservice.dto.tasklist.TaskListResponse;
import com.personalworkspace.taskservice.dto.tasklist.PatchTaskListRequest;
import com.personalworkspace.taskservice.service.TaskListService;
import com.personalworkspace.taskservice.security.AuthenticatedOwner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/v1/task-lists")
@RequiredArgsConstructor
@Tag(name = "Task Lists", description = "Quản lý danh sách dùng để phân nhóm công việc")
public class TaskListController {

    private final TaskListService taskListService;

    @PostMapping
    @Operation(summary = "Tạo task list")
    @ApiResponse(responseCode = "201", description = "Đã tạo task list")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Tên task list đã tồn tại",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public ResponseEntity<TaskListResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskListRequest request) {
        UUID ownerId = AuthenticatedOwner.from(jwt);
        TaskListResponse created = taskListService.create(ownerId, request);
        return ResponseEntity.created(URI.create("/api/v1/task-lists/" + created.id()))
                .body(created);
    }

    @GetMapping
    @Operation(summary = "Liệt kê task list")
    public List<TaskListResponse> list(
            @AuthenticationPrincipal Jwt jwt) {
        return taskListService.list(AuthenticatedOwner.from(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết task list")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task list",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public TaskListResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return taskListService.get(AuthenticatedOwner.from(jwt), id);
    }

    @GetMapping("/{id}/tasks")
    @Operation(summary = "Liệt kê task thuộc một task list")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task list",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public Page<TaskResponse> getTasks(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return taskListService.getTasks(AuthenticatedOwner.from(jwt), id, pageable);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật toàn bộ task list")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task list",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Tên task list đã tồn tại",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public TaskListResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @Valid @RequestBody TaskListRequest request) {
        return taskListService.update(AuthenticatedOwner.from(jwt), id, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Cập nhật một phần task list")
    public TaskListResponse patch(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @Valid @RequestBody PatchTaskListRequest request) {
        return taskListService.patch(AuthenticatedOwner.from(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa task list", description = "Các task trong list được giữ lại và chuyển thành chưa phân nhóm.")
    @ApiResponse(responseCode = "204", description = "Đã xóa task list")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task list",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        taskListService.delete(AuthenticatedOwner.from(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
