package com.personalworkspace.taskservice.service;

import com.personalworkspace.taskservice.dto.task.TaskResponse;
import com.personalworkspace.taskservice.dto.tasklist.TaskListRequest;
import com.personalworkspace.taskservice.dto.tasklist.TaskListResponse;
import com.personalworkspace.taskservice.dto.tasklist.PatchTaskListRequest;
import com.personalworkspace.taskservice.entity.TaskList;
import com.personalworkspace.taskservice.exception.DuplicateTaskListException;
import com.personalworkspace.taskservice.exception.TaskListNotFoundException;
import com.personalworkspace.taskservice.repository.TaskListRepository;
import com.personalworkspace.taskservice.repository.TaskRepository;
import com.personalworkspace.taskservice.mapper.TaskListMapper;
import com.personalworkspace.taskservice.mapper.TaskMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskListService {

    private final TaskListRepository taskListRepository;
    private final TaskRepository taskRepository;
    private final TaskListMapper taskListMapper;
    private final TaskMapper taskMapper;

    @Transactional
    public TaskListResponse create(UUID ownerId, TaskListRequest request) {
        ensureUniqueName(ownerId, request.name());
        return taskListMapper.toResponse(taskListRepository.save(TaskList.create(
                ownerId, request.name(), request.description(), request.color(), request.position())));
    }

    public TaskListResponse get(UUID ownerId, UUID id) {
        return taskListMapper.toResponse(findRequired(ownerId, id));
    }

    public List<TaskListResponse> list(UUID ownerId) {
        return taskListRepository.findAllByOwnerIdOrderByPositionAscNameAsc(ownerId).stream()
                .map(taskListMapper::toResponse).toList();
    }

    public org.springframework.data.domain.Page<TaskResponse> getTasks(
            UUID ownerId, UUID id, org.springframework.data.domain.Pageable pageable) {
        findRequired(ownerId, id);
        return taskRepository.findAllByTaskListIdAndOwnerId(id, ownerId, pageable)
                .map(taskMapper::toResponse);
    }

    @Transactional
    public TaskListResponse update(UUID ownerId, UUID id, TaskListRequest request) {
        TaskList entity = findRequired(ownerId, id);
        if (!entity.getName().equalsIgnoreCase(request.name())) {
            ensureUniqueName(ownerId, request.name());
        }
        entity.update(request.name(), request.description(), request.color(),
                request.position(), request.archived());
        return taskListMapper.toResponse(entity);
    }

    @Transactional
    public TaskListResponse patch(UUID ownerId, UUID id, PatchTaskListRequest request) {
        TaskList entity = findRequired(ownerId, id);
        String name = request.name() == null ? entity.getName() : request.name();
        if (!entity.getName().equalsIgnoreCase(name)) {
            ensureUniqueName(ownerId, name);
        }
        entity.update(
                name,
                request.description() == null ? entity.getDescription() : request.description(),
                request.color() == null ? entity.getColor() : request.color(),
                request.position() == null ? entity.getPosition() : request.position(),
                request.archived() == null ? entity.isArchived() : request.archived());
        return taskListMapper.toResponse(entity);
    }

    @Transactional
    public void delete(UUID ownerId, UUID id) {
        TaskList entity = findRequired(ownerId, id);
        if (taskRepository.findAllByTaskListIdAndOwnerId(
                id, ownerId, org.springframework.data.domain.Pageable.ofSize(1)).hasContent()) {
            throw new IllegalStateException("Không thể xóa task list đang chứa task");
        }
        taskListRepository.delete(entity);
    }

    private TaskList findRequired(UUID ownerId, UUID id) {
        return taskListRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new TaskListNotFoundException(id));
    }

    private void ensureUniqueName(UUID ownerId, String name) {
        if (taskListRepository.existsByOwnerIdAndNameIgnoreCase(ownerId, name.trim())) {
            throw new DuplicateTaskListException(name);
        }
    }
}
