package com.personalworkspace.fileservice.controller;
import com.personalworkspace.fileservice.dto.FolderDtos.*;
import com.personalworkspace.fileservice.service.FolderService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/folders") @RequiredArgsConstructor
public class FolderController {
 private final FolderService service;
 @PostMapping public Response create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Request r){return service.create(owner(jwt),r);}
 @GetMapping public List<Response> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false)UUID parentId){return service.list(owner(jwt),parentId);}
 @PatchMapping("/{id}") public Response patch(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Patch r){return service.patch(owner(jwt),id,r);}
 private UUID owner(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
