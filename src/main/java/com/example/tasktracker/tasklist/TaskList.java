package com.example.tasktracker.tasklist;

import com.example.tasktracker.task.Task;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A named list of tasks. Maps to the task_list table.
 * The table and column names come from Spring Boot's naming strategy:
 * TaskList -> task_list, createdAt -> created_at.
 * <p>
 * Lombok: @Getter on every field, @Setter only where app code may change a value.
 * No @Data on entities: its equals/hashCode/toString compare and print every field,
 * which breaks with lazy relations and changing values.
 */
@Entity
@Getter
// JPA needs a no-arg constructor to build objects from rows. Protected keeps app code
// using the real constructor below.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskList {

    // Hibernate generates a time-ordered UUID v7 in Java when the entity is saved,
    // matching the uuidv7() default in V1__init.sql.
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Setter
    private String title;

    @Setter
    private String description;

    // Set by Hibernate on insert, then never written again.
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    // Set by Hibernate on insert and on every update.
    @UpdateTimestamp
    private Instant updatedAt;

    // The inverse side: mappedBy = "taskList" means Task.taskList owns the foreign key,
    // and this list is just a view of it. @OneToMany is LAZY by default.
    // cascade = ALL: saving or deleting the list does the same to its tasks.
    // orphanRemoval: a task removed from this list is deleted from the database.
    // No Lombok getter: callers get a read-only view and must use addTask/removeTask.
    @OneToMany(mappedBy = "taskList", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    private List<Task> tasks = new ArrayList<>();

    public TaskList(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    /** Adds a task and sets its list, so both sides of the relationship agree. */
    public void addTask(Task task) {
        tasks.add(task);
        task.setTaskList(this);
    }

    /** Removes a task; orphanRemoval then deletes it from the database. */
    public void removeTask(Task task) {
        tasks.remove(task);
        task.setTaskList(null);
    }
}
