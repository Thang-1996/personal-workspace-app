package com.personalworkspace.taskservice.mapper;

import com.personalworkspace.taskservice.dto.tasklist.TaskListResponse;
import com.personalworkspace.taskservice.entity.TaskList;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskListMapper {
    TaskListResponse toResponse(TaskList taskList);
}
