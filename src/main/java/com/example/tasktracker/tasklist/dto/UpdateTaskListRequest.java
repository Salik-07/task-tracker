package com.example.tasktracker.tasklist.dto;

/**
 * Body of PUT /task-lists/{id}. PUT replaces the editable fields, so every field
 * is sent; a missing description means "clear it".
 * Same fields as the create request today, but kept separate so each can change
 * on its own (e.g. a field allowed on create but not on update).
 */
public record UpdateTaskListRequest(String title, String description) {
}
