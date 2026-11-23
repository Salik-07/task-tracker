package com.example.tasktracker.tasklist.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * What the API returns for a task list. Tasks aren't included: they have their
 * own paged endpoint, and loading them here would bring back N+1.
 */
public record TaskListResponse(
        UUID id,
        String title,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
