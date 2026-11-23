package com.example.tasktracker.task;

import com.example.tasktracker.common.ResourceNotFoundException;
import com.example.tasktracker.task.dto.CreateTaskRequest;
import com.example.tasktracker.task.dto.TaskResponse;
import com.example.tasktracker.task.dto.UpdateTaskRequest;
import com.example.tasktracker.task.dto.UpdateTaskStatusRequest;
import com.example.tasktracker.tasklist.TaskList;
import com.example.tasktracker.tasklist.TaskListRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.example.tasktracker.task.TaskSpecifications.hasPriority;
import static com.example.tasktracker.task.TaskSpecifications.hasStatus;
import static com.example.tasktracker.task.TaskSpecifications.inList;

/**
 * Business logic for tasks. Every method takes the list id from the URL, and a
 * task is only found through its own list: asking for it through another list
 * gives the same 404 as a missing task.
 * Same transaction setup as TaskListService: readOnly by default, writes override.
 */
@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskListRepository taskListRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository, TaskListRepository taskListRepository,
                       TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskListRepository = taskListRepository;
        this.taskMapper = taskMapper;
    }

    @Transactional
    public TaskResponse create(UUID taskListId, CreateTaskRequest request) {
        TaskList taskList = taskListRepository.findById(taskListId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskList", taskListId));
        Task task = taskMapper.toEntity(request);
        taskList.addTask(task);                     // sets task.taskList, keeps both sides in sync
        // saveAndFlush so the INSERT runs now and the timestamps are set (see TaskListService).
        return taskMapper.toResponse(taskRepository.saveAndFlush(task));
    }

    /** One page of a list's tasks. status and priority are optional filters (null = any). */
    public Page<TaskResponse> findAll(UUID taskListId, TaskStatus status, TaskPriority priority,
                                      Pageable pageable) {
        // Without this check, an unknown list would return an empty page instead of a 404.
        if (!taskListRepository.existsById(taskListId)) {
            throw new ResourceNotFoundException("TaskList", taskListId);
        }
        return taskRepository
                .findAll(inList(taskListId).and(hasStatus(status)).and(hasPriority(priority)), pageable)
                .map(taskMapper::toResponse);
    }

    public TaskResponse findById(UUID taskListId, UUID taskId) {
        return taskMapper.toResponse(getOrThrow(taskListId, taskId));
    }

    @Transactional
    public TaskResponse update(UUID taskListId, UUID taskId, UpdateTaskRequest request) {
        Task task = getOrThrow(taskListId, taskId);
        taskMapper.updateEntity(request, task);
        taskRepository.flush();                     // dirty checking; flush so updatedAt is set
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse updateStatus(UUID taskListId, UUID taskId, UpdateTaskStatusRequest request) {
        Task task = getOrThrow(taskListId, taskId);
        task.setStatus(request.status());
        taskRepository.flush();
        return taskMapper.toResponse(task);
    }

    @Transactional
    public void delete(UUID taskListId, UUID taskId) {
        // Delete the task directly. taskList.removeTask(task) would also work
        // (orphanRemoval), but it loads ALL of the list's tasks first just to
        // remove one from the collection.
        taskRepository.delete(getOrThrow(taskListId, taskId));
    }

    private Task getOrThrow(UUID taskListId, UUID taskId) {
        return taskRepository.findByIdAndTaskListId(taskId, taskListId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
    }
}
