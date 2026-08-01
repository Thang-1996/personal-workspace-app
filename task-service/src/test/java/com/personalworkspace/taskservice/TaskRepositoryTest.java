package com.personalworkspace.taskservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.personalworkspace.taskservice.dto.task.TaskFilter;
import com.personalworkspace.taskservice.entity.Task;
import com.personalworkspace.taskservice.entity.TaskPriority;
import com.personalworkspace.taskservice.entity.TaskStatus;
import com.personalworkspace.taskservice.repository.TaskRepository;
import com.personalworkspace.taskservice.repository.TaskSpecifications;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    private static final UUID OWNER = UUID.fromString(
            "40000000-0000-0000-0000-000000000004");

    @Autowired
    TaskRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void specificationCombinesOwnerFilterKeywordAndPagination() {
        repository.save(Task.create(
                OWNER, "Thiết kế database", "PostgreSQL", TaskPriority.HIGH,
                Instant.parse("2026-08-15T10:00:00Z"), 0, null, Set.of()));
        repository.save(Task.create(
                UUID.randomUUID(), "Task owner khác", "PostgreSQL", TaskPriority.HIGH,
                null, 0, null, Set.of()));
        entityManager.flush();

        TaskFilter filter = new TaskFilter(
                TaskStatus.TODO, TaskPriority.HIGH, null, null,
                null, null, "database");
        var result = repository.findAll(
                TaskSpecifications.ownedBy(OWNER).and(TaskSpecifications.matching(filter)),
                PageRequest.of(0, 1));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(Task::getTitle)
                .containsExactly("Thiết kế database");
    }

    @Test
    void dirtyCheckingIncrementsOptimisticVersion() {
        Task task = repository.saveAndFlush(Task.create(
                OWNER, "Optimistic lock", null, TaskPriority.MEDIUM,
                null, 0, null, Set.of()));
        long originalVersion = task.getVersion();

        task.changeStatus(TaskStatus.IN_PROGRESS);
        entityManager.flush();
        entityManager.clear();

        Task reloaded = repository.findByIdAndOwnerId(task.getId(), OWNER).orElseThrow();
        assertThat(reloaded.getVersion()).isGreaterThan(originalVersion);
    }
}
