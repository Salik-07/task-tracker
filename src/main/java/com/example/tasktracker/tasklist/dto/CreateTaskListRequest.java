package com.example.tasktracker.tasklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /task-lists. A record is an immutable data class: Java generates
 * the constructor, accessors (title(), description()), equals, hashCode and toString.
 * <p>
 * The annotations are Bean Validation rules. They're only checked when the
 * controller marks the parameter with @Valid. The limits match the database:
 * title is varchar(255) with a not-blank CHECK.
 */
public record CreateTaskListRequest(
        @NotBlank(message = "Title is required")                       // not null, not "" and not "   "
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")  // null is allowed
        String description
) {
}
