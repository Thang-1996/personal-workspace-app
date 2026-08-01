package com.personalworkspace.taskservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JPA aggregate root của feature Task. Entity bảo vệ invariant qua factory và behavior method;
 * không public setter để adapter HTTP không thể bỏ qua rule của domain.
 */
@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false)
    private int position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_list_id")
    private TaskList taskList;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "task_tag_relations",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<TaskTag> tags = new LinkedHashSet<>();

    /** Optimistic locking ngăn request cũ âm thầm ghi đè thay đổi mới. */
    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Task(UUID id, UUID ownerId, String title, String description) {
        this.id = Objects.requireNonNull(id);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.title = normalizeRequired(title);
        this.description = normalizeOptional(description);
        this.status = TaskStatus.TODO;
        this.priority = TaskPriority.MEDIUM;
    }

    public static Task create(
            UUID ownerId, String title, String description, TaskPriority priority,
            Instant dueAt, int position, TaskList taskList, Set<TaskTag> tags) {
        Task task = new Task(UUID.randomUUID(), ownerId, title, description);
        task.priority = priority == null ? TaskPriority.MEDIUM : priority;
        task.dueAt = dueAt;
        task.position = position;
        task.taskList = taskList;
        task.replaceTags(tags);
        return task;
    }

    public void updateDetails(String title, String description) {
        this.title = normalizeRequired(title);
        this.description = normalizeOptional(description);
    }

    public void changeStatus(TaskStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "status không được null");
        this.completedAt = newStatus == TaskStatus.DONE ? Instant.now() : null;
    }

    public void moveTo(TaskList taskList) {
        this.taskList = taskList;
    }

    public void reschedule(TaskPriority priority, Instant dueAt, int position) {
        this.priority = Objects.requireNonNull(priority, "priority không được null");
        this.dueAt = dueAt;
        this.position = position;
    }

    public void replaceTags(Set<TaskTag> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    private static String normalizeRequired(String value) {
        String normalized = Objects.requireNonNull(value, "title không được null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("title không được để trống");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
