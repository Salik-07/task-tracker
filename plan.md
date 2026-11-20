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
  - Lombok stays out at first so the plain Java is visible, then comes in as a deliberate choice.

## Roadmap (each step ends with a checkpoint)

| # | Step | Status |
|---|------|--------|
| 1 | **Project setup:** install JDK 21, generate the project with Spring Initializr, `git init`, `.gitignore`, a tour of auto-configuration, and CLAUDE.md | ✅ Done |
| 2 | **PostgreSQL in Docker:** `compose.yaml` with `postgres` (pinned version, named volume, healthcheck, `.env` credentials), datasource config, profiles, env-var overrides, and an Actuator health check | ✅ Done |
| 3 | **Schema and entities:** Flyway `V1__init.sql` (tables, foreign key, indexes, check constraints), the JPA entities, entity lifecycle, lazy loading, and auditing (`@CreationTimestamp` vs. Spring Data auditing). Config: `jpa.hibernate.ddl-auto: validate` | ⏭️ Next |
| 4 | **Repositories:** Spring Data interfaces, derived queries, pagination, and the N+1 problem with how to avoid it | |
| 5 | **Service layer:** business rules, `@Transactional` boundaries, interface vs. implementation, and why the service owns the transaction. Config: `jpa.open-in-view: false` | |
| 6 | **DTOs, mappers and controllers:** request and response records, REST endpoints, status codes and `ResponseEntity` | |
| 7 | **Validation and exception handling:** `@Valid`, custom messages, the global handler, and a consistent error response | |
| 8 | **Testing:** unit tests (JUnit 5, Mockito, AssertJ), `@WebMvcTest` slices, and `@DataJpaTest`/`@SpringBootTest` with **Testcontainers** against real Postgres | |
| 9 | **API docs and observability:** springdoc-openapi (Swagger UI), structured logging with SLF4J, a correlation-id MDC filter, and Actuator endpoints. Config: `logging.level.*`, `management.endpoints.web.exposure` | |
| 10 | **Security:** Spring Security, user registration and login, JWT (access and refresh tokens), role-based authorization, and per-user ownership of task lists | |
| 11 | **Containerize and CI:** multi-stage Dockerfile (or Buildpacks), the app added to compose, and a GitHub Actions build and test pipeline. Config: `POSTGRES_HOST` (database host on the Docker network) | |
| 12 | **Optional extras:** MapStruct, Lombok, caching, optimistic locking (`@Version`), ArchUnit rules, Spotless/Checkstyle | |

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

## Verification (each step)

- `./mvnw clean verify` builds the project and runs all tests.
- `docker compose up -d` and `docker compose ps` start and check the database (from step 2).
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` starts the app.
- `curl http://localhost:8080/actuator/health` returns `UP`. Later, the API endpoints are exercised with curl or an `.http` file.
- From step 8, running one test looks like `./mvnw test -Dtest=TaskServiceTest#methodName`.
