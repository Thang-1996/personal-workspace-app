package com.personalworkspace.taskservice.repository;

import com.personalworkspace.taskservice.dto.task.TaskFilter;
import com.personalworkspace.taskservice.entity.Task;
import com.personalworkspace.taskservice.entity.TaskTag;
import jakarta.persistence.criteria.JoinType;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class TaskSpecifications {

    private TaskSpecifications() {}

    public static Specification<Task> ownedBy(UUID ownerId) {
        return (root, query, builder) -> builder.equal(root.get("ownerId"), ownerId);
    }

    public static Specification<Task> matching(TaskFilter filter) {
        Specification<Task> specification = (root, query, builder) -> builder.conjunction();
        if (filter.status() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("status"), filter.status()));
        }
        if (filter.priority() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("priority"), filter.priority()));
        }
        if (filter.listId() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("taskList").get("id"), filter.listId()));
        }
        if (filter.tagId() != null) {
            specification = specification.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(root.<Task, TaskTag>join("tags", JoinType.INNER).get("id"),
                        filter.tagId());
            });
        }
        if (filter.dueFrom() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("dueAt"), filter.dueFrom()));
        }
        if (filter.dueTo() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("dueAt"), filter.dueTo()));
        }
        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            String pattern = "%" + filter.keyword().trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)));
        }
        return specification;
    }
}
