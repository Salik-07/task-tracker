package com.example.tasktracker.tasklist.dto;

/**
 * Body of POST /task-lists. A record is an immutable data class: Java generates
 * the constructor, accessors (title(), description()), equals, hashCode and toString.
 * Validation (@NotBlank, @Size) comes in step 7.
 */
public record CreateTaskListRequest(String title, String description) {
}
