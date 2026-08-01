package com.personalworkspace.fileservice.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
@Entity @Table(name="folders")
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Folder {
  @Id private UUID id;
  @Column(name="owner_id",nullable=false) private UUID ownerId;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parent_id") private Folder parent;
  @Column(nullable=false) private String name;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
  public static Folder create(UUID owner,String name,Folder parent){
    Folder f=new Folder();f.id=UUID.randomUUID();f.ownerId=owner;f.name=name.trim();f.parent=parent;
    f.createdAt=f.updatedAt=Instant.now();return f;
  }
  public void rename(String value){name=value.trim();updatedAt=Instant.now();}
}
