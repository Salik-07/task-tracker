# Task Tracker: learning Spring Boot step by step

## Context

A senior MERN/MEAN engineer is learning the Java ecosystem. The goal is a production-style Spring Boot REST backend for task lists and their tasks, backed by PostgreSQL running in Docker. The main point is **learning**, so we build in small steps. In each step, Claude explains the concept and why we chose it, writes the code, then **stops** so the code can be run, reviewed and questioned before we move on.

**Decisions so far:**
- Claude writes the code and explains it; the owner runs and reviews each step.
- Build tool: **Maven**, through the wrapper (`mvnw`), so there's no global install.
- JDK: **21 LTS** (Eclipse Temurin 21.0.13).
- Docker 24 and Compose v2 are already installed.

## Architecture we're aiming for

- **Stack:** Spring Boot **4.2.0**, Maven 3.10.1 (wrapper), Spring Web MVC, Spring Data JPA (Hibernate), Bean Validation, PostgreSQL driver, Flyway and Actuator.
- **Package by feature**, with layers inside each feature:
  ```
  com.example.tasktracker
  ├── tasklist/   TaskListController, TaskListService(+Impl), TaskListRepository, TaskList (entity), dto/, TaskListMapper
  ├── task/       same shape, plus the TaskStatus and TaskPriority enums
  ├── common/     exception/ (GlobalExceptionHandler, ResourceNotFoundException), shared DTOs (PageResponse)
  └── config/     typed @ConfigurationProperties, Jackson and other config
  ```
- **Domain:**
  - `TaskList`: UUID id, title, description, createdAt, updatedAt, and `@OneToMany` tasks.
  - `Task`: UUID id, title, description, dueDate, `status` (OPEN/COMPLETED), `priority` (HIGH/MEDIUM/LOW), a lazy `@ManyToOne` taskList, and timestamps.
  - Enums are stored as STRING.
- **API (versioned, nested resources):**
  - `GET|POST /api/v1/task-lists` and `GET|PUT|DELETE /api/v1/task-lists/{id}`
  - `GET|POST /api/v1/task-lists/{listId}/tasks` and `GET|PUT|DELETE …/tasks/{taskId}`
  - `PATCH …/tasks/{taskId}/status` marks a task complete or incomplete.
  - Collections are paginated and can be filtered by status and priority.
- **Conventions:**
  - DTOs are Java `record`s. Entities never leave the service layer.
  - Mapping is hand-written at first. MapStruct comes later, once the cost of writing it by hand is clear.
  - Constructor injection only. `@Transactional` lives on the service layer.
  - Errors are handled by `@RestControllerAdvice` and returned as `ProblemDetail` (RFC 9457).
  - The schema is owned by Flyway, and Hibernate runs with `ddl-auto=validate`.
  - Config lives in `application.yaml` plus a `dev` profile. Secrets come from environment variables or `.env`, which is git-ignored.
  - Lombok is used from phase 3.3 on, at the owner's choice, to cut getter and setter boilerplate. Only `@Getter`, `@Setter` and constructor annotations go on entities, never `@Data`.

## Roadmap (each step ends with a checkpoint)

| # | Step | Status |
|---|------|--------|
| 1 | **Project setup:** install JDK 21, generate the project with Spring Initializr, `git init`, `.gitignore`, a tour of auto-configuration, and CLAUDE.md | ✅ Done |
| 2 | **PostgreSQL in Docker:** `compose.yaml` with `postgres` (pinned version, named volume, healthcheck, `.env` credentials), datasource config, profiles, env-var overrides, and an Actuator health check | ✅ Done |
| 3 | **Schema and entities:** Flyway `V1__init.sql` (tables, foreign key, indexes, check constraints), the JPA entities, entity lifecycle, lazy loading, and auditing (`@CreationTimestamp` vs. Spring Data auditing). Config: `jpa.hibernate.ddl-auto: validate` | ✅ Done |
| 4 | **Repositories:** Spring Data interfaces, derived queries, pagination, and the N+1 problem with how to avoid it | ✅ Done |
| 5 | **Service layer:** business rules, `@Transactional` boundaries, interface vs. implementation, and why the service owns the transaction, plus the DTOs and mappers it returns. Config: `jpa.open-in-view: false` | ✅ Done |
| 6 | **Controllers:** REST endpoints, status codes, `ResponseEntity` and a shared `PageResponse` (DTOs and mappers moved to step 5) | ⏭️ Next |
| 7 | **Validation and exception handling:** `@Valid`, custom messages, the global handler, and a consistent error response | |
| 8 | **Testing:** unit tests (JUnit 5, Mockito, AssertJ), `@WebMvcTest` slices, and `@DataJpaTest`/`@SpringBootTest` with **Testcontainers** against real Postgres | |
| 9 | **API docs and observability:** springdoc-openapi (Swagger UI), structured logging with SLF4J, a correlation-id MDC filter, and Actuator endpoints. Config: `logging.level.*`, `management.endpoints.web.exposure` | |
| 10 | **Security:** Spring Security, user registration and login, JWT (access and refresh tokens), role-based authorization, and per-user ownership of task lists | |
| 11 | **Containerize and CI:** multi-stage Dockerfile (or Buildpacks), the app added to compose, and a GitHub Actions build and test pipeline. Config: `POSTGRES_HOST` (database host on the Docker network) | |
| 12 | **Optional extras:** MapStruct, Lombok, caching, optimistic locking (`@Version`), ArchUnit rules, Spotless/Checkstyle | |

## Phases (steps 3–12)

Each step is split into **phases**. A phase is one small, reviewable change, and it ends with a checkpoint: Claude explains it and writes it, then stops. The owner runs it, reviews it and asks questions, and only then does the next phase start. Mark a phase ✅ when it's reviewed.

The phases for later steps are a **draft**. They get refined at the start of each step, once the earlier code exists.

### Step 3: Schema and entities
- [x] **3.1 Flyway migration:** `V1__init.sql` with the `task_list` and `task` tables, UUID primary keys, the foreign key (`ON DELETE CASCADE`), check constraints for status and priority, and an index on `task.task_list_id`. Run the app, check that Flyway applied it, and look at the tables with `psql`.
- [x] **3.2 Enums:** `TaskStatus` and `TaskPriority`.
- [x] **3.3 `TaskList` entity:** `@Entity`/`@Table`, UUID id generation, columns, timestamps, and the protected no-arg constructor JPA needs. `equals`/`hashCode` were left out on purpose: the default identity check is enough while entities stay inside the service layer. If entities ever go into `Set`s or get compared across transactions, add id-based versions.
- [x] **3.4 `Task` entity:** `@Enumerated(STRING)`, a lazy `@ManyToOne`, the `@OneToMany` back on `TaskList`, and helper methods that keep both sides of the relationship in sync.
- [x] **3.5 Schema validation:** turn on `ddl-auto: validate`, break a mapping on purpose to see the error, then the entity lifecycle (transient, managed, detached, removed) and `@CreationTimestamp` vs. Spring Data auditing.

### Step 4: Repositories
- [x] **4.1 Basic repositories:** `TaskListRepository` and `TaskRepository` extending `JpaRepository`, and what Spring generates behind the interface.
- [x] **4.2 Queries scoped to a list:** derived queries such as `findByIdAndTaskListId` and `findByTaskListId(Pageable)`, plus `Page` vs. `Slice`. (`findByTaskListId` was later replaced by the `inList` specification in 4.3.)
- [x] **4.3 Filtering:** optional status and priority filters (derived queries vs. `@Query` vs. Specifications, and which one to pick).
- [x] **4.4 N+1:** turn on SQL logging in the dev profile, reproduce N+1, then fix it with `@EntityGraph` or `JOIN FETCH`.

### Step 5: Service layer
Refined before starting. Entities must not leave the service layer, so services return DTOs from the start: the DTOs and mappers move here from step 6. Services are concrete classes, with no interface + `Impl` pair: there's one implementation, Spring can proxy a class for `@Transactional`, and Mockito can mock a class.
- [x] **5.1 Groundwork:** `ResourceNotFoundException` in `common/`, and `jpa.open-in-view: false` (what it does and why it's turned off).
- [x] **5.2 TaskList DTOs and mapper:** `CreateTaskListRequest`, `UpdateTaskListRequest` and `TaskListResponse` records, plus a hand-written `TaskListMapper`.
- [x] **5.3 `TaskListService`:** CRUD with `@Transactional` boundaries, `readOnly` for reads, and dirty checking (updates with no `save()` call).
- [x] **5.4 Task DTOs and mapper:** `CreateTaskRequest`, `UpdateTaskRequest`, `UpdateTaskStatusRequest` and `TaskResponse` records, plus `TaskMapper`.
- [x] **5.5 `TaskService`:** CRUD scoped to a list, the filtered and paged listing (specifications from 4.3), and changing the status.

### Step 6: Controllers
- [ ] **6.1 `PageResponse`:** a shared record in `common/`, so the API doesn't expose Spring's `Page` JSON.
- [ ] **6.2 `TaskListController`:** CRUD endpoints, status codes, `ResponseEntity`, and the `Location` header on create.
- [ ] **6.3 `TaskController`:** nested endpoints, `PATCH …/status`, and pagination and filter parameters.
- [ ] **6.4 Exercise the API:** an `.http` file covering every endpoint.

### Step 7: Validation and exception handling
- [ ] **7.1 Bean Validation:** constraints on the request records, `@Valid`, and custom messages.
- [ ] **7.2 Global handler:** `@RestControllerAdvice` returning `ProblemDetail` for not found (404) and validation errors (400, with field errors).
- [ ] **7.3 Edge cases:** malformed JSON, a bad UUID or enum in the path or query, database constraint violations, and a safe catch-all 500.

### Step 8: Testing
- [ ] **8.1 Service unit tests:** JUnit 5, Mockito and AssertJ, with no Spring context.
- [ ] **8.2 Controller slice tests:** `@WebMvcTest`, status codes, JSON and error responses.
- [ ] **8.3 Testcontainers setup:** `@ServiceConnection`, and `contextLoads` stops depending on the `dev` profile.
- [ ] **8.4 Repository tests:** `@DataJpaTest` against real Postgres.
- [ ] **8.5 End-to-end tests:** `@SpringBootTest` through the full HTTP stack.

### Step 9: API docs and observability
- [ ] **9.1 OpenAPI:** springdoc, Swagger UI, and annotations where the generated docs aren't enough.
- [ ] **9.2 Logging:** SLF4J in the services, log levels, and `logging.level.*`.
- [ ] **9.3 Correlation id:** a servlet filter that puts a request id into the MDC and the response header.
- [ ] **9.4 Actuator:** `management.endpoints.web.exposure`, plus the info and metrics endpoints.

### Step 10: Security
- [ ] **10.1 Spring Security basics:** add the starter, see what the defaults do, and write a stateless `SecurityFilterChain`.
- [ ] **10.2 Users and registration:** `V2` migration, `User` entity, password hashing, and a register endpoint.
- [ ] **10.3 Login and access tokens:** issue a JWT on login (pick the library at this point).
- [ ] **10.4 JWT authentication:** validate the token on every request.
- [ ] **10.5 Refresh tokens:** storage, rotation and revocation.
- [ ] **10.6 Roles:** role-based authorization.
- [ ] **10.7 Ownership:** migration that adds an owner to `task_list`, and queries scoped to the current user.
- [ ] **10.8 Security errors and tests:** 401/403 as `ProblemDetail`, and security tests.

### Step 11: Containerize and CI
- [ ] **11.1 Image:** multi-stage Dockerfile vs. Buildpacks.
- [ ] **11.2 Compose:** add the app to `compose.yaml`, with `POSTGRES_HOST`.
- [ ] **11.3 CI:** GitHub Actions that builds and runs the tests.

### Step 12: Optional extras (one phase each, any order)
- [ ] **12.1** MapStruct replaces the hand-written mappers.
- [x] **12.2** Lombok. Moved up and added in phase 3.3.
- [ ] **12.3** Optimistic locking with `@Version`.
- [ ] **12.4** Caching.
- [ ] **12.5** ArchUnit rules for the package structure.
- [ ] **12.6** Spotless/Checkstyle.

## Step 1: what was done

- Installed Temurin JDK 21. `JAVA_HOME` is set system-wide.
- Generated the project from start.spring.io: Spring Boot 4.2.0, Java 21, group `com.example`, artifact `task-tracker`, YAML config.
- Removed the empty metadata placeholders from `pom.xml`, and added `.env` and `.claude/settings.local.json` to `.gitignore`.
- Covered: `pom.xml` (parent POM, BOM, starters, scopes), `mvnw`, `@SpringBootApplication` (configuration, component scan, auto-configuration), and conditional auto-configuration.
- `./mvnw clean compile` succeeds. `contextLoads` and `spring-boot:run` fail **as expected** with `Failed to configure a DataSource` because there's no database yet. Step 2 fixes this.
- IntelliJ's project SDK is set to `temurin-21` with language level 21, and the Maven Runner uses the project JDK. IntelliJ's terminal sets `JAVA_HOME` from the project SDK, so the two must match.

## Step 2: what was done

- `compose.yaml`: `postgres:18.7-alpine` pinned, bound to `127.0.0.1` only, a named volume `postgres-data` mounted at `/var/lib/postgresql` (the Postgres 18+ layout), and a `pg_isready` healthcheck.
- `.env.example` is committed. `.env` is git-ignored and has a randomly generated password. Both Compose and Spring read the same `POSTGRES_*` variables.
- `application.yaml` (shared by all environments) holds **only the datasource**: URL, username and password from `POSTGRES_*` variables. There are no default credentials. If a value is missing, the app doesn't start, but the error is indirect: Spring passes the literal text `${POSTGRES_USER}` to Postgres, which rejects the login (verified).
- `application-dev.yaml` holds `spring.config.import: file:.env[.properties]`, which loads `.env` and fails with a clear error if it's missing, plus `show-details: always` for the health endpoint.
- **Config is kept minimal on purpose.** Settings were trimmed after review and come back in the step that needs them (see the "Config:" notes in the roadmap). The Hikari pool settings were dropped entirely: they just restated the defaults.
- Two **expected** startup warnings remain until later steps: Flyway's `No migrations found` (step 3) and `spring.jpa.open-in-view is enabled by default` (step 5).
- `contextLoads` uses `@ActiveProfiles("dev")` **temporarily**, until Testcontainers replaces it in step 8.
- Verified: the container is healthy, `/actuator/health` shows `db: UP` (PostgreSQL 18.7), Flyway created `flyway_schema_history` (with a "No migrations found" warning, expected until step 3), and `./mvnw clean verify` passes.

## Step 3: what was done

- `V1__init.sql` creates `task_list` and `task`: `uuid` primary keys with `DEFAULT uuidv7()` (built into Postgres 18), `timestamptz` timestamps, `CHECK` constraints for blank titles and for the status and priority values, a foreign key with `ON DELETE CASCADE`, and an index on `task.task_list_id`.
- `TaskStatus` and `TaskPriority` enums. Their names must match the `CHECK` lists in the migration.
- The `TaskList` and `Task` entities:
  - Hibernate generates UUID v7 ids with `@UuidGenerator(style = VERSION_7)`. Timestamps use `@CreationTimestamp` and `@UpdateTimestamp`.
  - No `@Table` or `@Column(name)`: Spring Boot's naming strategy maps camelCase to snake_case.
  - The relationship is bidirectional: `Task.taskList` is the owning side (`@ManyToOne(fetch = LAZY, optional = false)`). `TaskList.tasks` is the inverse side (`mappedBy`, `cascade = ALL`, `orphanRemoval`).
  - `addTask` and `removeTask` keep both sides in sync, and `getTasks()` is read-only.
- No `equals`/`hashCode` on entities, by the owner's choice: the default identity check is enough while entities stay inside the service layer.
- **Lombok was added early, at the owner's request.** Entities use `@Getter`, per-field `@Setter` and `@NoArgsConstructor(access = PROTECTED)`, never `@Data`. Lombok is registered in `annotationProcessorPaths`.
- `ddl-auto: validate` is on. A mapping broken on purpose (`title` renamed to `name`) fails startup with `Schema-validation: missing column`.
- Notes: `docs/notes/04-jpa-relationships.pdf` (owning vs. inverse side, syncing both sides, LAZY proxies, cascade, `orphanRemoval`).
- Verified: Flyway applies V1, the `CHECK` rejects a blank title, and `./mvnw clean verify` passes.
- One expected startup warning remains until step 5: `spring.jpa.open-in-view is enabled by default`.

## Step 4: what was done

- `TaskListRepository` and `TaskRepository` extend `JpaRepository<Entity, UUID>`. Spring Data generates the implementation at startup.
- `TaskRepository.findByIdAndTaskListId`: a derived query, so a task is only found through its own list (later a 404 otherwise).
- Filtering uses **Specifications**: `TaskRepository` also extends `JpaSpecificationExecutor`, and `TaskSpecifications` has `inList`, `hasStatus` and `hasPriority`. A null filter returns `Specification.unrestricted()`. Used as `findAll(inList(id).and(hasStatus(s)).and(hasPriority(p)), pageable)`, which returns a `Page` (with a total count).
- `findByTaskListId(Pageable)` from 4.2 was removed: `inList(id)` with no filters does the same job.
- SQL logging in the dev profile: `org.hibernate.SQL: debug` and `org.hibernate.orm.jdbc.bind: trace`.
- N+1: `TaskListRepository.findAllWithTasksBy()` uses `@EntityGraph(attributePaths = "tasks")` to load lists and tasks in one query. Don't combine it with `Pageable` (in-memory paging).
- `NPlusOneDemoTests` counts statements with Hibernate Statistics: `findAll` + lazy tasks = 1 + N, `findAllWithTasksBy` = 1. Tests roll back, so no data is left behind.
- Notes: `docs/notes/05-repositories.pdf` (repositories, free methods, `save`, transactions, `Optional`).
- Verified: `./mvnw clean verify` passes (3 tests). A temporary check confirmed every filter combination and the list scoping, then was deleted.
- Trade-off noted: specification attribute names are strings; a generated metamodel (`Task_.status`) could make them type-safe later.

## Step 5: what was done

- `common/ResourceNotFoundException` (unchecked, so it also triggers rollback). Step 7 maps it to 404.
- `jpa.open-in-view: false` in `application.yaml`: the session closes with the service transaction, so lazy data must be read inside services. The startup warning is gone.
- DTOs moved here from step 6, so entities never leave services. Records in `tasklist/dto` (`CreateTaskListRequest`, `UpdateTaskListRequest`, `TaskListResponse`) and `task/dto` (`CreateTaskRequest`, `UpdateTaskRequest`, `UpdateTaskStatusRequest`, `TaskResponse`).
  - PUT replaces the editable fields. A missing task priority resets to MEDIUM. Status changes only through `UpdateTaskStatusRequest`.
  - `TaskListResponse` has no tasks (own paged endpoint, avoids N+1). `TaskResponse` has `taskListId`, read from the lazy proxy without a query.
- Hand-written `@Component` mappers: `TaskListMapper` and `TaskMapper` (`toEntity`, `updateEntity`, `toResponse`).
- `TaskListService` and `TaskService` are concrete classes (no interface + `Impl`), with `@Transactional(readOnly = true)` on the class and plain `@Transactional` on writes.
  - Updates use dirty checking (setters, no `save()`).
  - **Gotcha found:** `@CreationTimestamp`/`@UpdateTimestamp` are only set when the SQL is sent, so the response had null timestamps. Fixed with `saveAndFlush` on create and `flush()` on update.
  - Deletes load first, so a missing id is a 404 (`deleteById` ignores it). A task is deleted directly, not with `removeTask`, which would load all of the list's tasks.
  - `TaskService` scopes every call to the list id; listing tasks of an unknown list is a 404, not an empty page.
- Verified: `./mvnw clean verify` passes. Temporary checks against the dev DB covered every service method, the 404 cases and the cascade, then were deleted along with their data.

## Verification (each step)

- `./mvnw clean verify` builds the project and runs all tests.
- `docker compose up -d` and `docker compose ps` start and check the database (from step 2).
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` starts the app.
- `curl http://localhost:8080/actuator/health` returns `UP`. Later, the API endpoints are exercised with curl or an `.http` file.
- From step 8, running one test looks like `./mvnw test -Dtest=TaskServiceTest#methodName`.
