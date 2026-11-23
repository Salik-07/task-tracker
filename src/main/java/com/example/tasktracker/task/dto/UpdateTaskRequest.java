package com.example.tasktracker.task.dto;

import com.example.tasktracker.task.TaskPriority;

import java.time.LocalDate;

/**
 * Body of PUT /task-lists/{listId}/tasks/{taskId}. Replaces the editable fields:
 * a missing description or dueDate clears it, a missing priority resets it to MEDIUM.
 * Status isn't here: it changes through its own PATCH .../status endpoint.
 */
public record UpdateTaskRequest(
        String title,
        String description,
        LocalDate dueDate,
        TaskPriority priority
) {
}
