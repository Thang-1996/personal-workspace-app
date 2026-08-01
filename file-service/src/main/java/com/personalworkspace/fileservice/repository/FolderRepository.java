package com.personalworkspace.fileservice.repository;
import com.personalworkspace.fileservice.entity.Folder;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FolderRepository extends JpaRepository<Folder,UUID>{
 Optional<Folder> findByIdAndOwnerId(UUID id,UUID ownerId);
 List<Folder> findAllByOwnerIdAndParentIdOrderByName(UUID ownerId,UUID parentId);
 List<Folder> findAllByOwnerIdAndParentIsNullOrderByName(UUID ownerId);
 boolean existsByOwnerIdAndParentIdAndNameIgnoreCase(UUID ownerId,UUID parentId,String name);
 boolean existsByOwnerIdAndParentIsNullAndNameIgnoreCase(UUID ownerId,String name);
}
