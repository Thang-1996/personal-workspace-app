package com.personalworkspace.fileservice.exception;
import org.springframework.http.HttpStatus;
public class FileApiException extends RuntimeException {private final HttpStatus status;public FileApiException(HttpStatus s,String m){super(m);status=s;}public HttpStatus status(){return status;}}
