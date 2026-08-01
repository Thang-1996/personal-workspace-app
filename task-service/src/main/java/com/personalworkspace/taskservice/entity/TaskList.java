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

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private TaskList(String name, String description) {
        this.id = UUID.randomUUID();
        update(name, description);
    }

    public static TaskList create(String name, String description) {
        return new TaskList(name, description);
    }

    public void update(String name, String description) {
        String normalizedName = Objects.requireNonNull(name, "name không được null").trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("name không được để trống");
        }
        this.name = normalizedName;
        this.description = description == null || description.isBlank() ? null : description.trim();
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
