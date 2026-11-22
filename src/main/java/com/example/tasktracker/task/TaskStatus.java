package com.example.tasktracker.task;

/**
 * Whether a task is still to do or done.
 * Stored in the database as the constant's name ("OPEN", "COMPLETED"),
 * so these names must match the CHECK constraint in V1__init.sql.
 */
public enum TaskStatus {
    OPEN,
    COMPLETED
}
