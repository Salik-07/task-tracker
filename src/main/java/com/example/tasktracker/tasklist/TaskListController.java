package com.example.tasktracker.tasklist;

import com.example.tasktracker.common.PageResponse;
import com.example.tasktracker.tasklist.dto.CreateTaskListRequest;
import com.example.tasktracker.tasklist.dto.TaskListResponse;
import com.example.tasktracker.tasklist.dto.UpdateTaskListRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * HTTP layer for task lists. Only translates HTTP to service calls and back:
 * no business logic, no transactions, no entities.
 * <p>
 * @RestController = @Controller + @ResponseBody: return values are written as JSON.
 * Validation (@Valid) and error responses (404 instead of 500) come in step 7.
 */
@RestController
@RequestMapping("/api/v1/task-lists")
public class TaskListController {

    private final TaskListService taskListService;

    public TaskListController(TaskListService taskListService) {
        this.taskListService = taskListService;
    }

    // POST /api/v1/task-lists -> 201 Created, Location: /api/v1/task-lists/{id}, body = the new list
    // REST convention: a POST tells the client where the new resource lives (the
    // Location header), so the client can GET it there.
    @PostMapping
    public ResponseEntity<TaskListResponse> create(@RequestBody CreateTaskListRequest request) {
        TaskListResponse created = taskListService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()  // the URL just called, e.g. http://localhost:8080/api/v1/task-lists (no hard-coded host or port)
                .path("/{id}")                                           // add "/{id}" to the end
                .buildAndExpand(created.id())                            // fill in {id} -> /api/v1/task-lists/01a2...
                .toUri();                                                // make it a URI object
        // created(location): status 201 + the Location header. body(created): the new list as JSON.
        return ResponseEntity.created(location).body(created);
    }

    // GET /api/v1/task-lists?page=0&size=20&sort=title,asc -> 200
    // Spring builds the Pageable from those query parameters; the defaults apply when they're missing.
    @GetMapping
    public PageResponse<TaskListResponse> findAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(taskListService.findAll(pageable));
    }

    // GET /api/v1/task-lists/{id} -> 200. A plain return value means 200 OK.
    @GetMapping("/{id}")
    public TaskListResponse findById(@PathVariable UUID id) {
        return taskListService.findById(id);
    }

    // PUT /api/v1/task-lists/{id} -> 200 with the updated list
    @PutMapping("/{id}")
    public TaskListResponse update(@PathVariable UUID id, @RequestBody UpdateTaskListRequest request) {
        return taskListService.update(id, request);
    }

    // DELETE /api/v1/task-lists/{id} -> 204 No Content (success, empty body)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        taskListService.delete(id);
    }
}
