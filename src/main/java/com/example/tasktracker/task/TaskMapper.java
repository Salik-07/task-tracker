package com.example.tasktracker.task;

import com.example.tasktracker.task.dto.CreateTaskRequest;
import com.example.tasktracker.task.dto.TaskResponse;
import com.example.tasktracker.task.dto.UpdateTaskRequest;
import org.springframework.stereotype.Component;

/** Converts between Task and its DTOs. Same approach as TaskListMapper. */
@Component
public class TaskMapper {

    /** Builds a new task with no list yet; the service attaches it with TaskList.addTask(). */
    public Task toEntity(CreateTaskRequest request) {
        return new Task(request.title(), request.description(), request.dueDate(), request.priority());
    }

    /** Copies the editable fields onto an existing, managed task. */
    public void updateEntity(UpdateTaskRequest request, Task task) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        // Same rule as create: no priority means MEDIUM (the column is NOT NULL).
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
    }

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                // taskList is a LAZY proxy, but the proxy already knows its id
                // (it came from task_list_id), so getId() runs no query.
                task.getTaskList().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getStatus(),
                task.getPriority(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
