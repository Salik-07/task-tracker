package com.example.tasktracker.task;

/**
 * How important a task is.
 * Stored in the database as the constant's name ("HIGH", "MEDIUM", "LOW"),
 * so these names must match the CHECK constraint in V1__init.sql.
 */
public enum TaskPriority {
    HIGH,
    MEDIUM,
    LOW
}
