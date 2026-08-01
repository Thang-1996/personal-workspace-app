package com.personalworkspace.taskservice.controller;

import com.personalworkspace.taskservice.dto.task.TaskResponse;
import com.personalworkspace.taskservice.dto.tasklist.TaskListRequest;
import com.personalworkspace.taskservice.dto.tasklist.TaskListResponse;
import com.personalworkspace.taskservice.dto.tasklist.PatchTaskListRequest;
import com.personalworkspace.taskservice.service.TaskListService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/v1/task-lists")
@RequiredArgsConstructor
@Tag(name = "Task Lists", description = "Quản lý danh sách dùng để phân nhóm công việc")
public class TaskListController {

    private static final String DEV_OWNER = "00000000-0000-0000-0000-000000000001";
    private final TaskListService taskListService;

    @PostMapping
    @Operation(summary = "Tạo task list")
    @ApiResponse(responseCode = "201", description = "Đã tạo task list")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Tên task list đã tồn tại",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public ResponseEntity<TaskListResponse> create(
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId,
            @Valid @RequestBody TaskListRequest request) {
        TaskListResponse created = taskListService.create(ownerId, request);
        return ResponseEntity.created(URI.create("/api/v1/task-lists/" + created.id()))
                .body(created);
    }

    @GetMapping
    @Operation(summary = "Liệt kê task list")
    public List<TaskListResponse> list(
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId) {
        return taskListService.list(ownerId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết task list")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task list",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public TaskListResponse get(
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId,
            @PathVariable UUID id) {
        return taskListService.get(ownerId, id);
    }

    @GetMapping("/{id}/tasks")
    @Operation(summary = "Liệt kê task thuộc một task list")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task list",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public Page<TaskResponse> getTasks(
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId,
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return taskListService.getTasks(ownerId, id, pageable);
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
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId,
            @PathVariable UUID id, @Valid @RequestBody TaskListRequest request) {
        return taskListService.update(ownerId, id, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Cập nhật một phần task list")
    public TaskListResponse patch(
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId,
            @PathVariable UUID id, @Valid @RequestBody PatchTaskListRequest request) {
        return taskListService.patch(ownerId, id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa task list", description = "Các task trong list được giữ lại và chuyển thành chưa phân nhóm.")
    @ApiResponse(responseCode = "204", description = "Đã xóa task list")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task list",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public ResponseEntity<Void> delete(
            @RequestHeader(name = "X-Owner-Id", defaultValue = DEV_OWNER) UUID ownerId,
            @PathVariable UUID id) {
        taskListService.delete(ownerId, id);
        return ResponseEntity.noContent().build();
    }
}
