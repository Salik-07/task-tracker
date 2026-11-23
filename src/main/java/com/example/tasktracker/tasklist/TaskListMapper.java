package com.example.tasktracker.tasklist;

import com.example.tasktracker.tasklist.dto.CreateTaskListRequest;
import com.example.tasktracker.tasklist.dto.TaskListResponse;
import com.example.tasktracker.tasklist.dto.UpdateTaskListRequest;
import org.springframework.stereotype.Component;

/**
 * Converts between TaskList and its DTOs. Hand-written for now; a @Component so
 * services get it by injection, which keeps a later switch to MapStruct
 * (also a generated @Component) a drop-in change.
 * Must be called inside a transaction when it reads lazy data (it doesn't today).
 */
@Component
public class TaskListMapper {

    public TaskList toEntity(CreateTaskListRequest request) {
        return new TaskList(request.title(), request.description());
    }

    /** Copies the editable fields onto an existing, managed entity. */
    public void updateEntity(UpdateTaskListRequest request, TaskList taskList) {
        taskList.setTitle(request.title());
        taskList.setDescription(request.description());
    }

    public TaskListResponse toResponse(TaskList taskList) {
        return new TaskListResponse(
                taskList.getId(),
                taskList.getTitle(),
                taskList.getDescription(),
                taskList.getCreatedAt(),
                taskList.getUpdatedAt()
        );
    }
}
