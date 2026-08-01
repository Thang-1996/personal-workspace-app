package com.personalworkspace.fileservice.exception;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(FileApiException.class) ResponseEntity<ProblemDetail> handle(FileApiException e){ProblemDetail p=ProblemDetail.forStatusAndDetail(e.status(),e.getMessage());p.setTitle(e.status().getReasonPhrase());return ResponseEntity.status(e.status()).body(p);}
 @ExceptionHandler(Exception.class) ResponseEntity<ProblemDetail> generic(Exception e){ProblemDetail p=ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,"File operation failed");return ResponseEntity.status(500).body(p);}
}
