package com.example.tasktracker.tasklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /task-lists/{id}. PUT replaces the editable fields, so every field
 * is sent; a missing description means "clear it".
 * Same fields and rules as the create request today, but kept separate so each
 * can change on its own (e.g. a field allowed on create but not on update).
 */
public record UpdateTaskListRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description
) {
}
