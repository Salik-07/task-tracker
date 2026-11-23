package com.example.tasktracker.common;

/**
 * Thrown by services when a requested row doesn't exist (or isn't reachable,
 * e.g. a task asked for through the wrong list).
 * <p>
 * Unchecked (extends RuntimeException), so callers don't need try/catch or
 * "throws" clauses. It also makes @Transactional roll back, which only happens
 * automatically for unchecked exceptions.
 * The global error handler (step 7) will turn it into a 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);   // e.g. "TaskList not found: 0193..."
    }
}
