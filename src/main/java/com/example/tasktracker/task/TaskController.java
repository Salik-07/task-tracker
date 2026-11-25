package com.example.tasktracker.task;

import com.example.tasktracker.common.PageResponse;
import com.example.tasktracker.task.dto.CreateTaskRequest;
import com.example.tasktracker.task.dto.TaskResponse;
import com.example.tasktracker.task.dto.UpdateTaskRequest;
import com.example.tasktracker.task.dto.UpdateTaskStatusRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * HTTP layer for tasks. Tasks are nested under their list in the URL, so every
 * endpoint gets {taskListId} from the base path; the service uses it to scope
 * every query to that list.
 * Same conventions as TaskListController. Validation and 404s come in step 7.
 */
@RestController
@RequestMapping("/api/v1/task-lists/{taskListId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // POST .../tasks -> 201 Created, Location: .../tasks/{taskId}, body = the new task
    @PostMapping
    public ResponseEntity<TaskResponse> create(@PathVariable UUID taskListId,
                                               @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.create(taskListId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()  // .../task-lists/{taskListId}/tasks
                .path("/{taskId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // GET .../tasks?status=OPEN&priority=HIGH&page=0&size=20&sort=dueDate,asc -> 200
    // status and priority are optional (required = false): missing means "any".
    // Spring converts "OPEN" to TaskStatus.OPEN by name; an unknown value gives 400.
    @GetMapping
    public PageResponse<TaskResponse> findAll(
            @PathVariable UUID taskListId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(taskService.findAll(taskListId, status, priority, pageable));
    }

    // GET .../tasks/{taskId} -> 200
    @GetMapping("/{taskId}")
    public TaskResponse findById(@PathVariable UUID taskListId, @PathVariable UUID taskId) {
        return taskService.findById(taskListId, taskId);
    }

    // PUT .../tasks/{taskId} -> 200. Replaces title, description, dueDate and priority.
    @PutMapping("/{taskId}")
    public TaskResponse update(@PathVariable UUID taskListId, @PathVariable UUID taskId,
                               @RequestBody UpdateTaskRequest request) {
        return taskService.update(taskListId, taskId, request);
    }

    // PATCH .../tasks/{taskId}/status -> 200. PATCH = change part of a resource,
    // here only the status: {"status": "COMPLETED"}.
    @PatchMapping("/{taskId}/status")
    public TaskResponse updateStatus(@PathVariable UUID taskListId, @PathVariable UUID taskId,
                                     @RequestBody UpdateTaskStatusRequest request) {
        return taskService.updateStatus(taskListId, taskId, request);
    }

    // DELETE .../tasks/{taskId} -> 204 No Content
    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID taskListId, @PathVariable UUID taskId) {
        taskService.delete(taskListId, taskId);
    }
}
