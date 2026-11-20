package com.example.tasktracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// TEMPORARY: runs against the local dev database from compose.yaml.
// Step 8 replaces this with Testcontainers, a throwaway Postgres per test run.
@ActiveProfiles("dev")
@SpringBootTest
class TaskTrackerApplicationTests {

	@Test
	void contextLoads() {
	}

}
