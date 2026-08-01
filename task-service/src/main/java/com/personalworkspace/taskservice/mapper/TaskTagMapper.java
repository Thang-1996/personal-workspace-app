package com.personalworkspace.taskservice.mapper;

import com.personalworkspace.taskservice.dto.tasktag.TaskTagResponse;
import com.personalworkspace.taskservice.entity.TaskTag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskTagMapper {
    TaskTagResponse toResponse(TaskTag tag);
}
