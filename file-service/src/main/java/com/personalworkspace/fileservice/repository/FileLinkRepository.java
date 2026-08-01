package com.personalworkspace.fileservice.repository;
import com.personalworkspace.fileservice.entity.FileLink;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FileLinkRepository extends JpaRepository<FileLink,UUID>{
 boolean existsByFileIdAndEntityTypeAndEntityId(UUID fileId,String entityType,UUID entityId);
}
