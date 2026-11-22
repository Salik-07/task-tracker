package com.example.tasktracker;

import com.example.tasktracker.task.Task;
import com.example.tasktracker.tasklist.TaskList;
import com.example.tasktracker.tasklist.TaskListRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Learning demo for phase 4.4: shows the N+1 problem and its fix by counting
 * the SQL statements Hibernate sends. Watch the log while it runs.
 * Each test runs in a transaction that is rolled back, so no data is left behind.
 */
@ActiveProfiles("dev")
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class NPlusOneDemoTests {

    @Autowired TaskListRepository taskLists;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;

    Statistics stats;
    long listsInDb;

    @BeforeEach
    void setUp() {
        // 3 lists with 2 tasks each, written to the DB.
        for (int i = 1; i <= 3; i++) {
            TaskList list = new TaskList("List " + i, null);
            list.addTask(new Task("Task A", null, null, null));
            list.addTask(new Task("Task B", null, null, null));
            taskLists.save(list);
        }
        em.flush();  // send the INSERTs now
        em.clear();  // forget the objects, so the next reads must hit the DB
        listsInDb = taskLists.count();
        em.clear();

        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();  // count only what the test does from here on
    }

    @Test
    void lazyTasks_causeNPlusOne() {
        for (TaskList list : taskLists.findAll()) {   // 1 query for the lists
            list.getTasks().size();                   // +1 query per list
        }
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1 + listsInDb);
    }

    @Test
    void entityGraph_loadsEverythingInOneQuery() {
        for (TaskList list : taskLists.findAllWithTasksBy()) {  // 1 query with a JOIN
            list.getTasks().size();                              // already loaded
        }
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }
}
