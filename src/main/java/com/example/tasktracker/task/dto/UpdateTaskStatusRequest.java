package com.example.tasktracker.task.dto;

import com.example.tasktracker.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

/** Body of PATCH /task-lists/{listId}/tasks/{taskId}/status, e.g. {"status": "COMPLETED"}. */
public record UpdateTaskStatusRequest(
        @NotNull(message = "Status is required")
        TaskStatus status
) {
}
