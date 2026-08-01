package com.personalworkspace.taskservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_lists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskList {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 20)
    private String color;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean archived;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private TaskList(UUID ownerId, String name, String description, String color, int position) {
        this.id = UUID.randomUUID();
        this.ownerId = Objects.requireNonNull(ownerId);
        update(name, description, color, position, false);
    }

    public static TaskList create(
            UUID ownerId, String name, String description, String color, int position) {
        return new TaskList(ownerId, name, description, color, position);
    }

    public void update(
            String name, String description, String color, int position, boolean archived) {
        String normalizedName = Objects.requireNonNull(name, "name không được null").trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("name không được để trống");
        }
        this.name = normalizedName;
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.color = color == null || color.isBlank() ? null : color.trim();
        this.position = position;
        this.archived = archived;
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

}
