package com.personalworkspace.fileservice.repository;
import com.personalworkspace.fileservice.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StoredFileRepository extends JpaRepository<StoredFile,UUID> {
  Optional<StoredFile> findByIdAndOwnerIdAndStatus(UUID id,UUID ownerId,FileStatus status);
  List<StoredFile> findAllByOwnerIdAndStatusOrderByCreatedAtDesc(UUID ownerId,FileStatus status);
  List<StoredFile> findAllByOwnerIdAndFolderIdAndStatusOrderByCreatedAtDesc(UUID ownerId,UUID folderId,FileStatus status);
}
