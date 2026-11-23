package com.example.tasktracker.task.dto;

import com.example.tasktracker.task.TaskPriority;

import java.time.LocalDate;

/**
 * Body of POST /task-lists/{listId}/tasks. The list comes from the URL, not the body.
 * No status: a new task always starts OPEN.
 * priority may be null, which means MEDIUM. Validation comes in step 7.
 */
public record CreateTaskRequest(
        String title,
        String description,
        LocalDate dueDate,       // JSON "2026-12-31"
        TaskPriority priority    // JSON "HIGH"; Jackson maps it by name
) {
}
