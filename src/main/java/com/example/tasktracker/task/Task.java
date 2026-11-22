package com.example.tasktracker.task;

import com.example.tasktracker.tasklist.TaskList;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One task inside a TaskList. Maps to the task table. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    // The owning side of the relationship: this field holds the task_list_id foreign key
    // (column name from the naming strategy: taskList -> task_list_id).
    // LAZY: loading a task doesn't load its list; Hibernate puts a placeholder (proxy)
    // there and only queries the list if you actually use it.
    // optional = false: every task has a list (matches NOT NULL).
    // Set through TaskList.addTask(), which keeps both sides in sync.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Setter
    private TaskList taskList;

    @Setter
    private String title;

    @Setter
    private String description;

    @Setter
    private LocalDate dueDate;

    // STRING stores the name ("OPEN"). The default, ORDINAL, stores the position (0, 1),
    // which silently corrupts data if anyone reorders the constants.
    @Enumerated(EnumType.STRING)
    @Setter
    private TaskStatus status = TaskStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Setter
    private TaskPriority priority = TaskPriority.MEDIUM;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public Task(String title, String description, LocalDate dueDate, TaskPriority priority) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        if (priority != null) {
            this.priority = priority;
        }
    }
}
