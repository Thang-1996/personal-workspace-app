package com.personalworkspace.fileservice.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
@Entity @Table(name="files")
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class StoredFile {
  @Id private UUID id;
  @Column(name="owner_id",nullable=false) private UUID ownerId;
  @Column(name="original_name",nullable=false) private String originalName;
  @Column(name="storage_key",nullable=false,unique=true) private String storageKey;
  @Column(name="content_type",nullable=false) private String contentType;
  @Column(name="size_bytes",nullable=false) private long sizeBytes;
  @Column(nullable=false,length=64) private String checksum;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="folder_id") private Folder folder;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private FileStatus status;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
  public static StoredFile pending(UUID owner,String name,String key,String type,long size,String checksum,Folder folder){
    StoredFile f=new StoredFile(); f.id=UUID.randomUUID(); f.ownerId=owner; f.originalName=name; f.storageKey=key;
    f.contentType=type; f.sizeBytes=size; f.checksum=checksum; f.folder=folder; f.status=FileStatus.PENDING;
    f.createdAt=f.updatedAt=Instant.now(); return f;
  }
  public void ready(){status=FileStatus.READY;updatedAt=Instant.now();}
  public void deleted(){status=FileStatus.DELETED;updatedAt=Instant.now();}
}
