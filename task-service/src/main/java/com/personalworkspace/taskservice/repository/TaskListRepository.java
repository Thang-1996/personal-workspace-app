package com.personalworkspace.taskservice.repository;

import com.personalworkspace.taskservice.entity.TaskList;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskListRepository extends JpaRepository<TaskList, UUID> {

    boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);

    java.util.Optional<TaskList> findByIdAndOwnerId(UUID id, UUID ownerId);

    java.util.List<TaskList> findAllByOwnerIdOrderByPositionAscNameAsc(UUID ownerId);
}
