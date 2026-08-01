package com.personalworkspace.taskservice.service;

import com.personalworkspace.taskservice.dto.task.TaskResponse;
import com.personalworkspace.taskservice.dto.tasklist.TaskListRequest;
import com.personalworkspace.taskservice.dto.tasklist.TaskListResponse;
import com.personalworkspace.taskservice.entity.TaskList;
import com.personalworkspace.taskservice.exception.DuplicateTaskListException;
import com.personalworkspace.taskservice.exception.TaskListNotFoundException;
import com.personalworkspace.taskservice.repository.TaskListRepository;
import com.personalworkspace.taskservice.repository.TaskRepository;
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

    @Transactional
    public TaskListResponse create(TaskListRequest request) {
        ensureUniqueName(request.name());
        return TaskListResponse.from(
                taskListRepository.save(TaskList.create(request.name(), request.description())));
    }

    public TaskListResponse get(UUID id) {
        return TaskListResponse.from(findRequired(id));
    }

    public List<TaskListResponse> list() {
        return taskListRepository.findAll().stream().map(TaskListResponse::from).toList();
    }

    public List<TaskResponse> getTasks(UUID id) {
        findRequired(id);
        return taskRepository.findAllByTaskListIdOrderByCreatedAtDesc(id).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public TaskListResponse update(UUID id, TaskListRequest request) {
        TaskList entity = findRequired(id);
        if (!entity.getName().equalsIgnoreCase(request.name())) {
            ensureUniqueName(request.name());
        }
        entity.update(request.name(), request.description());
        return TaskListResponse.from(entity);
    }

    @Transactional
    public void delete(UUID id) {
        TaskList entity = findRequired(id);
        if (!taskRepository.findAllByTaskListIdOrderByCreatedAtDesc(id).isEmpty()) {
            throw new IllegalStateException("Không thể xóa task list đang chứa task");
        }
        taskListRepository.delete(entity);
    }

    private TaskList findRequired(UUID id) {
        return taskListRepository.findById(id)
                .orElseThrow(() -> new TaskListNotFoundException(id));
    }

    private void ensureUniqueName(String name) {
        if (taskListRepository.existsByNameIgnoreCase(name.trim())) {
            throw new DuplicateTaskListException(name);
        }
    }
}
