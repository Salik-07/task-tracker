package com.example.tasktracker.task.dto;

import com.example.tasktracker.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body of PUT /task-lists/{listId}/tasks/{taskId}. Replaces the editable fields:
 * a missing description or dueDate clears it, a missing priority resets it to MEDIUM.
 * Status isn't here: it changes through its own PATCH .../status endpoint.
 * No @FutureOrPresent on dueDate here (see CreateTaskRequest).
 */
public record UpdateTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        LocalDate dueDate,
        TaskPriority priority
) {
}
