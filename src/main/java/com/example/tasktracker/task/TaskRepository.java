package com.example.tasktracker.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for Task. Every query is scoped to one list, so a task can only be
 * reached through the list it belongs to.
 * <p>
 * Derived queries: Spring Data reads the method name and writes the query.
 * "TaskListId" resolves to the path taskList.id, which is the task_list_id column,
 * so no join is needed.
 * <p>
 * JpaSpecificationExecutor adds findAll(Specification, Pageable), used for the
 * filtered task list. The conditions live in TaskSpecifications.
 */
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    // WHERE id = ? AND task_list_id = ?
    // Empty if the task doesn't exist OR belongs to another list.
    Optional<Task> findByIdAndTaskListId(UUID id, UUID taskListId);
}
