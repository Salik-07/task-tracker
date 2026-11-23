package com.example.tasktracker.task.dto;

import com.example.tasktracker.task.TaskStatus;

/** Body of PATCH /task-lists/{listId}/tasks/{taskId}/status, e.g. {"status": "COMPLETED"}. */
public record UpdateTaskStatusRequest(TaskStatus status) {
}
