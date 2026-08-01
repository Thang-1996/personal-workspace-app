package com.personalworkspace.taskservice.repository;

import com.personalworkspace.taskservice.entity.Task;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Persistence boundary của feature. Spring Data sinh implementation lúc runtime; controller
 * không inject repository trực tiếp mà luôn đi qua application service.
 */
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    java.util.Optional<Task> findByIdAndOwnerId(UUID id, UUID ownerId);

    org.springframework.data.domain.Page<Task> findAllByTaskListIdAndOwnerId(
            UUID taskListId, UUID ownerId, org.springframework.data.domain.Pageable pageable);
}
