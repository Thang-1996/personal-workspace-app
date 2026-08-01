package com.personalworkspace.fileservice.dto;
import com.personalworkspace.fileservice.entity.Folder;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public final class FolderDtos {
 private FolderDtos(){}
 public record Request(@NotBlank @Size(max=120) String name,UUID parentId){}
 public record Patch(@NotBlank @Size(max=120) String name){}
 public record Response(UUID id,String name,UUID parentId,Instant createdAt){public static Response from(Folder f){return new Response(f.getId(),f.getName(),f.getParent()==null?null:f.getParent().getId(),f.getCreatedAt());}}
}
