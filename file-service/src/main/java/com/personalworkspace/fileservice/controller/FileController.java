package com.personalworkspace.fileservice.controller;
import com.personalworkspace.fileservice.dto.FileResponse;
import com.personalworkspace.fileservice.service.FileApplicationService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/v1/files") @RequiredArgsConstructor
public class FileController {
 private final FileApplicationService service;
 @PostMapping(value="/upload",consumes="multipart/form-data") public FileResponse upload(@AuthenticationPrincipal Jwt jwt,@RequestPart("file")MultipartFile file,@RequestParam(required=false)UUID folderId){return service.upload(owner(jwt),file,folderId);}
 @GetMapping public List<FileResponse> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false)UUID folderId,@RequestParam(required=false)String name,@RequestParam(required=false)String type){return service.list(owner(jwt),folderId,name,type);}
 @GetMapping("/{id}/download") public ResponseEntity<InputStreamResource> download(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.download(owner(jwt),id);}
 @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){service.delete(owner(jwt),id);return ResponseEntity.noContent().build();}
 @PostMapping("/{id}/links/tasks/{taskId}") public ResponseEntity<Void> link(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@PathVariable UUID taskId){service.linkTask(owner(jwt),id,taskId);return ResponseEntity.noContent().build();}
 private UUID owner(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
