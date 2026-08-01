package com.personalworkspace.fileservice.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
@Entity @Table(name="file_links")
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class FileLink {
  @Id private UUID id;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="file_id",nullable=false) private StoredFile file;
  @Column(name="linked_entity_type",nullable=false) private String entityType;
  @Column(name="linked_entity_id",nullable=false) private UUID entityId;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  public static FileLink task(StoredFile file,UUID taskId){FileLink l=new FileLink();l.id=UUID.randomUUID();l.file=file;l.entityType="TASK";l.entityId=taskId;l.createdAt=Instant.now();return l;}
}
