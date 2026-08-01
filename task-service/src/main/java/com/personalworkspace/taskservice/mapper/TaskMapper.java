package com.personalworkspace.taskservice.mapper;

import com.personalworkspace.taskservice.dto.task.TaskResponse;
import com.personalworkspace.taskservice.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "taskListId", source = "taskList.id")
    @Mapping(target = "tagIds", expression = "java(task.getTags().stream()"
            + ".map(com.personalworkspace.taskservice.entity.TaskTag::getId)"
            + ".collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)))")
    TaskResponse toResponse(Task task);
}
