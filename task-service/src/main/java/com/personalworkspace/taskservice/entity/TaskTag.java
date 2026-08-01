package com.personalworkspace.taskservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_task_tags_owner_name", columnNames = {"owner_id", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskTag {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String color;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private TaskTag(UUID ownerId, String name, String color) {
        this.id = UUID.randomUUID();
        this.ownerId = Objects.requireNonNull(ownerId);
        this.name = normalizeName(name);
        this.color = normalizeOptional(color);
    }

    public static TaskTag create(UUID ownerId, String name, String color) {
        return new TaskTag(ownerId, name, color);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    private static String normalizeName(String value) {
        String normalized = Objects.requireNonNull(value).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("name không được để trống");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
