package com.personalworkspace.taskservice.controller;

import com.personalworkspace.taskservice.dto.task.CreateTaskRequest;
import com.personalworkspace.taskservice.dto.task.TaskResponse;
import com.personalworkspace.taskservice.dto.task.UpdateTaskRequest;
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
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + created.id())).body(created);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Lấy chi tiết task")
    @ApiResponse(responseCode = "200", description = "Tìm thấy task")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public TaskResponse get(@PathVariable UUID taskId) {
        return taskService.get(taskId);
    }

    @GetMapping
    @Operation(summary = "Liệt kê task", description = "Có thể lọc theo trạng thái; bỏ trống để lấy tất cả.")
    public List<TaskResponse> list(
            @Parameter(description = "Trạng thái cần lọc", example = "TODO")
            @RequestParam(required = false) TaskStatus status) {
        return taskService.list(status);
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Cập nhật toàn bộ task")
    @ApiResponse(responseCode = "200", description = "Đã cập nhật task")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task hoặc task list",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public TaskResponse update(
            @PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(taskId, request);
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Xóa task")
    @ApiResponse(responseCode = "204", description = "Đã xóa task")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy task",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public ResponseEntity<Void> delete(@PathVariable UUID taskId) {
        taskService.delete(taskId);
        return ResponseEntity.noContent().build();
    }
}
