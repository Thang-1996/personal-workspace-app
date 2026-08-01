package com.personalworkspace.taskservice.service;

import com.personalworkspace.taskservice.dto.tasktag.TaskTagRequest;
import com.personalworkspace.taskservice.dto.tasktag.TaskTagResponse;
import com.personalworkspace.taskservice.entity.TaskTag;
import com.personalworkspace.taskservice.exception.DuplicateTaskTagException;
import com.personalworkspace.taskservice.mapper.TaskTagMapper;
import com.personalworkspace.taskservice.repository.TaskTagRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskTagService {

    private final TaskTagRepository repository;
    private final TaskTagMapper mapper;

    @Transactional
    public TaskTagResponse create(UUID ownerId, TaskTagRequest request) {
        if (repository.existsByOwnerIdAndNameIgnoreCase(ownerId, request.name().trim())) {
            throw new DuplicateTaskTagException(request.name());
        }
        return mapper.toResponse(repository.save(
                TaskTag.create(ownerId, request.name(), request.color())));
    }

    public List<TaskTagResponse> list(UUID ownerId) {
        return repository.findAllByOwnerIdOrderByNameAsc(ownerId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
