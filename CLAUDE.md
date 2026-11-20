# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose and working style

This is a **learning project**. The owner is a senior Node/React/Angular engineer learning Spring Boot by building a production-grade REST backend. Work **one roadmap step at a time**:
- Explain the concepts and the reasoning behind each decision. Compare with Node/Express equivalents where that helps.
- Write the code for that step only, then stop for the owner to run and review it.
- Don't generate later steps ahead of time. This includes config: add a setting only in the step that uses it, and never restate a Spring Boot default. `plan.md` notes which step brings each deferred setting back.

The full roadmap (steps 1–12: Docker Postgres → schema/entities → repositories → services → controllers/DTOs → validation/errors → tests → docs/logging → security/JWT → containerization/CI) lives in `plan.md`. Update its status table when a step is completed.

## Stack

Java 21 (Temurin LTS), Spring Boot 4.2, Maven via the wrapper (no global Maven install), PostgreSQL (Docker Compose), Spring Data JPA/Hibernate, Flyway, Bean Validation, Actuator.

## Commands

Use `./mvnw` in Git Bash or `mvnw.cmd` in cmd/PowerShell.

```bash
./mvnw clean verify                          # build + all tests
./mvnw test -Dtest=ClassName#methodName      # single test
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
docker compose up -d --wait                  # local Postgres; needs .env (copy .env.example)
curl localhost:8080/actuator/health          # db status (details shown in dev profile)
```

In IntelliJ, the owner runs the app with ▶ on `main()`, using the shared run config `.run/TaskTrackerApplication.run.xml` (Active profiles: `dev`). Without the dev profile, `.env` isn't loaded and login fails with user `"${POSTGRES_USER}"`.

The app needs a reachable PostgreSQL to start or to pass `@SpringBootTest`: DataSource, JPA and Flyway auto-configuration all require it. Until step 8 (Testcontainers), tests use the `dev` profile, so they need `docker compose up` first. `.env` is the single source of DB credentials for both Compose and Spring: the dev profile imports it with `spring.config.import`.

## Architecture conventions (target design)

- **Package by feature** under `com.example.tasktracker`: `tasklist/`, `task/` (each with its controller, service, repository, entity, `dto/` and mapper), `common/` (exceptions, shared DTOs), `config/`.
- Domain: `TaskList` 1→N `Task`. A task has `status` (OPEN/COMPLETED) and `priority` (HIGH/MEDIUM/LOW), with enums stored as STRING. IDs are UUIDs.
- REST: `/api/v1/task-lists[/{id}]`, nested `/api/v1/task-lists/{listId}/tasks[/{taskId}]`, and `PATCH …/tasks/{taskId}/status`. Collections are paginated.
- DTOs are Java `record`s. Entities never leave the service layer. Mappers are hand-written (MapStruct may come later).
- Use constructor injection only. `@Transactional` lives on the service layer.
- Errors are handled by a global `@RestControllerAdvice` that returns `ProblemDetail` (RFC 9457).
- The schema is owned by **Flyway** migrations in `src/main/resources/db/migration`, and Hibernate runs with `ddl-auto: validate`. Never let Hibernate generate the schema.
- Config goes in `application.yaml` plus profile files (`application-dev.yaml`). Secrets come from environment variables or `.env`, which is git-ignored.
- No Lombok for now. It's a deliberate later addition.
