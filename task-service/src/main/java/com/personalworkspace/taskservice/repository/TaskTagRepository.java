package com.personalworkspace.taskservice.repository;

import com.personalworkspace.taskservice.entity.TaskTag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskTagRepository extends JpaRepository<TaskTag, UUID> {
    boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);
    List<TaskTag> findAllByOwnerIdOrderByNameAsc(UUID ownerId);
    List<TaskTag> findAllByOwnerIdAndIdIn(UUID ownerId, Collection<UUID> ids);
}
