package com.example.tasktracker.task.dto;

import com.example.tasktracker.task.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body of POST /task-lists/{listId}/tasks. The list comes from the URL, not the body.
 * No status: a new task always starts OPEN.
 * priority may be null, which means MEDIUM.
 */
public record CreateTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        // Only on create: a new task can't already be overdue. Not on update, so an
        // old task whose date has passed can still be edited. null is allowed.
        @FutureOrPresent(message = "Due date must be today or later")
        LocalDate dueDate,       // JSON "2026-12-31"

        TaskPriority priority    // JSON "HIGH"; Jackson maps it by name
) {
}
