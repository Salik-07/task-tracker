package com.example.tasktracker.task.dto;

import com.example.tasktracker.task.TaskPriority;
import com.example.tasktracker.task.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** What the API returns for a task. The list is referenced by id, not embedded. */
public record TaskResponse(
        UUID id,
        UUID taskListId,
        String title,
        String description,
        LocalDate dueDate,
        TaskStatus status,
        TaskPriority priority,
        Instant createdAt,
        Instant updatedAt
) {
}
