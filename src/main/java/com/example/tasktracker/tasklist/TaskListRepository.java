package com.example.tasktracker.tasklist;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Data access for TaskList. JpaRepository<Entity, IdType> gives save, findById,
 * findAll (also paged and sorted), deleteById, count and more. Spring Data
 * creates the implementation at startup, so there's no class to write.
 */
public interface TaskListRepository extends JpaRepository<TaskList, UUID> {

    // Loads every list AND its tasks in one query (a LEFT JOIN), avoiding N+1.
    // tasks is LAZY: normally it's loaded only when you first use it (one query per list).
    // @EntityGraph overrides that for this method only: it fetches tasks eagerly,
    // together with the lists, in the same query. All other methods stay LAZY. "WithTasks" between find and By is
    // just a label: Spring Data ignores it, so this is findAll() plus the graph.
    // Don't combine with Pageable: a collection join returns one row per task,
    // so Hibernate would have to page in memory.
    @EntityGraph(attributePaths = "tasks")
    List<TaskList> findAllWithTasksBy();
}
