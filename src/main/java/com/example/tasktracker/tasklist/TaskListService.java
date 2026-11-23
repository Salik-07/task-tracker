package com.example.tasktracker.tasklist;

import com.example.tasktracker.common.ResourceNotFoundException;
import com.example.tasktracker.tasklist.dto.CreateTaskListRequest;
import com.example.tasktracker.tasklist.dto.TaskListResponse;
import com.example.tasktracker.tasklist.dto.UpdateTaskListRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic for task lists. Takes and returns DTOs; entities stay inside.
 * <p>
 * Transactions: the class default is readOnly (no dirty checking, and Postgres
 * can skip some work). Methods that write override it with a plain @Transactional.
 * Each public method is one transaction: it all commits, or all rolls back on an
 * unchecked exception.
 */
@Service
@Transactional(readOnly = true)
public class TaskListService {

    private final TaskListRepository taskListRepository;
    private final TaskListMapper taskListMapper;

    // Constructor injection: Spring passes the beans in. With one constructor,
    // no @Autowired is needed.
    public TaskListService(TaskListRepository taskListRepository, TaskListMapper taskListMapper) {
        this.taskListRepository = taskListRepository;
        this.taskListMapper = taskListMapper;
    }

    @Transactional
    public TaskListResponse create(CreateTaskListRequest request) {
        // saveAndFlush, not save: save() only queues the INSERT until commit, and
        // @CreationTimestamp/@UpdateTimestamp are filled when the INSERT is sent.
        // Flushing now means the response has its timestamps.
        TaskList saved = taskListRepository.saveAndFlush(taskListMapper.toEntity(request));
        return taskListMapper.toResponse(saved);
    }

    public Page<TaskListResponse> findAll(Pageable pageable) {
        // Page.map converts each entity while the transaction is still open.
        return taskListRepository.findAll(pageable).map(taskListMapper::toResponse);
    }

    public TaskListResponse findById(UUID id) {
        return taskListMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public TaskListResponse update(UUID id, UpdateTaskListRequest request) {
        TaskList taskList = getOrThrow(id);           // managed: Hibernate tracks it
        taskListMapper.updateEntity(request, taskList);
        // No save() needed: dirty checking sends the UPDATE at commit.
        // flush() sends it now instead, so @UpdateTimestamp has set updatedAt
        // before we build the response.
        taskListRepository.flush();
        return taskListMapper.toResponse(taskList);
    }

    @Transactional
    public void delete(UUID id) {
        // Load first, so a missing id gives a 404. (deleteById silently ignores
        // missing ids.) cascade = ALL then deletes the list's tasks too.
        taskListRepository.delete(getOrThrow(id));
    }

    private TaskList getOrThrow(UUID id) {
        return taskListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaskList", id));
    }
}
