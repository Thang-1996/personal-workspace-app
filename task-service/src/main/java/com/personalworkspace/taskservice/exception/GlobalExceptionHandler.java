package com.personalworkspace.taskservice.exception;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Điểm tập trung để chuyển exception từ tầng HTTP thành RFC 9457 Problem Details.
 *
 * <p>Controller không tự bắt exception hoặc tự ghép JSON lỗi. Cách này giữ controller mỏng,
 * đảm bảo mọi endpoint dùng cùng một error contract và cho phép bổ sung mapping cho business
 * exception sau này mà không sửa từng controller.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            TaskNotFoundException.class, TaskListNotFoundException.class,
            TaskTagNotFoundException.class})
    ResponseEntity<ProblemDetail> handleNotFound(RuntimeException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(ApiProblemType.RESOURCE_NOT_FOUND);
        problem.setTitle("Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler({
            DuplicateTaskListException.class, DuplicateTaskTagException.class,
            IllegalStateException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ProblemDetail> handleConflict(RuntimeException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(ApiProblemType.CONFLICT);
        problem.setTitle("Conflict");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Chuyển lỗi Bean Validation trên request DTO thành HTTP 400 kèm danh sách field lỗi.
     */
    @Override
    @Nullable
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<Map<String, String>> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Dữ liệu request không hợp lệ.");
        problem.setType(ApiProblemType.VALIDATION_ERROR);
        problem.setTitle("Validation failed");
        problem.setProperty("errors", violations);

        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private Map<String, String> toViolation(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() == null
                ? "Giá trị không hợp lệ."
                : fieldError.getDefaultMessage();
        return Map.of("field", fieldError.getField(), "message", message);
    }
}
