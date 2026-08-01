package com.personalworkspace.taskservice.service;

import com.personalworkspace.taskservice.dto.task.CreateTaskRequest;
import com.personalworkspace.taskservice.dto.task.TaskResponse;
import com.personalworkspace.taskservice.dto.task.UpdateTaskRequest;
import com.personalworkspace.taskservice.dto.task.TaskFilter;
import com.personalworkspace.taskservice.entity.Task;
import com.personalworkspace.taskservice.entity.TaskList;
import com.personalworkspace.taskservice.entity.TaskStatus;
import com.personalworkspace.taskservice.entity.TaskTag;
import com.personalworkspace.taskservice.exception.TaskNotFoundException;
import com.personalworkspace.taskservice.exception.TaskListNotFoundException;
import com.personalworkspace.taskservice.repository.TaskRepository;
import com.personalworkspace.taskservice.repository.TaskListRepository;
import com.personalworkspace.taskservice.repository.TaskTagRepository;
import com.personalworkspace.taskservice.mapper.TaskMapper;
import com.personalworkspace.taskservice.exception.TaskTagNotFoundException;
import com.personalworkspace.taskservice.repository.TaskSpecifications;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Application boundary điều phối use case. Transaction thuộc service, không thuộc controller;
 * repository và entity không bị lộ ra ngoài API boundary.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskListRepository taskListRepository;
    private final TaskTagRepository taskTagRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public TaskResponse create(UUID ownerId, CreateTaskRequest request) {
        Task task = Task.create(
                ownerId, request.title(), request.description(), request.priority(), request.dueAt(),
                request.position(), findTaskList(ownerId, request.taskListId()),
                findTags(ownerId, request.tagIds()));
        return taskMapper.toResponse(taskRepository.save(task));
    }

    public TaskResponse get(UUID ownerId, UUID taskId) {
        return taskMapper.toResponse(findRequired(ownerId, taskId));
    }

    public Page<TaskResponse> list(UUID ownerId, TaskFilter filter, Pageable pageable) {
        return taskRepository.findAll(
                        TaskSpecifications.ownedBy(ownerId)
                                .and(TaskSpecifications.matching(filter)),
                        pageable)
                .map(taskMapper::toResponse);
    }

    @Transactional
    public TaskResponse update(UUID ownerId, UUID taskId, UpdateTaskRequest request) {
        Task task = findRequired(ownerId, taskId);
        task.updateDetails(request.title(), request.description());
        task.changeStatus(request.status());
        task.moveTo(findTaskList(ownerId, request.taskListId()));
        task.reschedule(request.priority(), request.dueAt(), request.position());
        task.replaceTags(findTags(ownerId, request.tagIds()));
        // Hibernate dirty checking ghi thay đổi khi transaction commit.
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse changeStatus(UUID ownerId, UUID taskId, TaskStatus status) {
        Task task = findRequired(ownerId, taskId);
        task.changeStatus(status);
        return taskMapper.toResponse(task);
    }

    @Transactional
    public void delete(UUID ownerId, UUID taskId) {
        taskRepository.delete(findRequired(ownerId, taskId));
    }

    private Task findRequired(UUID ownerId, UUID taskId) {
        return taskRepository.findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private TaskList findTaskList(UUID ownerId, UUID taskListId) {
        if (taskListId == null) {
            return null;
        }
        return taskListRepository.findByIdAndOwnerId(taskListId, ownerId)
                .orElseThrow(() -> new TaskListNotFoundException(taskListId));
    }

    private Set<TaskTag> findTags(UUID ownerId, Set<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Set.of();
        }
        Set<TaskTag> tags = new LinkedHashSet<>(
                taskTagRepository.findAllByOwnerIdAndIdIn(ownerId, tagIds));
        if (tags.size() != tagIds.size()) {
            UUID missing = tagIds.stream()
                    .filter(id -> tags.stream().noneMatch(tag -> tag.getId().equals(id)))
                    .findFirst()
                    .orElseThrow();
            throw new TaskTagNotFoundException(missing);
        }
        return tags;
    }
}
