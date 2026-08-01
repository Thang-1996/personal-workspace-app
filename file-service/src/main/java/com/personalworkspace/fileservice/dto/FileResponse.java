package com.personalworkspace.fileservice.dto;
import com.personalworkspace.fileservice.entity.StoredFile;
import java.time.Instant;
import java.util.UUID;
public record FileResponse(UUID id,String originalName,String contentType,long sizeBytes,String checksum,UUID folderId,String status,Instant createdAt){
 public static FileResponse from(StoredFile f){return new FileResponse(f.getId(),f.getOriginalName(),f.getContentType(),f.getSizeBytes(),f.getChecksum(),f.getFolder()==null?null:f.getFolder().getId(),f.getStatus().name(),f.getCreatedAt());}
}
