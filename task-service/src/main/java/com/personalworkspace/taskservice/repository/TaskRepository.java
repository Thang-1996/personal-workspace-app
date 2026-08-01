package com.personalworkspace.taskservice.repository;

import com.personalworkspace.taskservice.entity.Task;
import com.personalworkspace.taskservice.entity.TaskStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence boundary của feature. Spring Data sinh implementation lúc runtime; controller
 * không inject repository trực tiếp mà luôn đi qua application service.
 */
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByStatusOrderByCreatedAtDesc(TaskStatus status);

    List<Task> findAllByTaskListIdOrderByCreatedAtDesc(UUID taskListId);
}
