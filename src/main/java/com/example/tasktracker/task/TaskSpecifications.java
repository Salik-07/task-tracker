package com.example.tasktracker.task;

import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Reusable WHERE pieces for Task queries. Each method returns one condition;
 * callers chain them with .and(). A null filter returns unrestricted(), which
 * adds nothing, so optional filters need no if/else in the caller:
 * <pre>
 * inList(listId).and(hasStatus(status)).and(hasPriority(priority))
 * </pre>
 */
public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    // task_list_id = ?  (taskList.id is the foreign key column, so no join)
    public static Specification<Task> inList(UUID taskListId) {
        return (root, query, cb) -> cb.equal(root.get("taskList").get("id"), taskListId);
    }

    // status = ?  or no condition when status is null
    public static Specification<Task> hasStatus(TaskStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    // priority = ?  or no condition when priority is null
    public static Specification<Task> hasPriority(TaskPriority priority) {
        if (priority == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }
}
