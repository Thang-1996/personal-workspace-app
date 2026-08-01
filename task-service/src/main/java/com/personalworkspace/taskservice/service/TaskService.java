package com.personalworkspace.taskservice.service;

import com.personalworkspace.taskservice.dto.task.CreateTaskRequest;
import com.personalworkspace.taskservice.dto.task.TaskResponse;
import com.personalworkspace.taskservice.dto.task.UpdateTaskRequest;
import com.personalworkspace.taskservice.entity.Task;
import com.personalworkspace.taskservice.entity.TaskList;
import com.personalworkspace.taskservice.entity.TaskStatus;
import com.personalworkspace.taskservice.exception.TaskNotFoundException;
import com.personalworkspace.taskservice.exception.TaskListNotFoundException;
import com.personalworkspace.taskservice.repository.TaskRepository;
import com.personalworkspace.taskservice.repository.TaskListRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        return TaskResponse.from(
                taskRepository.save(Task.create(
                        request.title(), request.description(), findTaskList(request.taskListId()))));
    }

    public TaskResponse get(UUID taskId) {
        return TaskResponse.from(findRequired(taskId));
    }

    public List<TaskResponse> list(TaskStatus status) {
        List<Task> tasks = status == null
                ? taskRepository.findAll()
                : taskRepository.findAllByStatusOrderByCreatedAtDesc(status);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public TaskResponse update(UUID taskId, UpdateTaskRequest request) {
        Task task = findRequired(taskId);
        task.updateDetails(request.title(), request.description());
        task.changeStatus(request.status());
        task.moveTo(findTaskList(request.taskListId()));
        // Hibernate dirty checking ghi thay đổi khi transaction commit.
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(UUID taskId) {
        taskRepository.delete(findRequired(taskId));
    }

    private Task findRequired(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private TaskList findTaskList(UUID taskListId) {
        if (taskListId == null) {
            return null;
        }
        return taskListRepository.findById(taskListId)
                .orElseThrow(() -> new TaskListNotFoundException(taskListId));
    }
}
