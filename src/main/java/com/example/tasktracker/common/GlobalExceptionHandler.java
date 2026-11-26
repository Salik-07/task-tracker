package com.example.tasktracker.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns exceptions thrown anywhere in a controller (or the services it calls) into
 * error responses, in one place. Every error body is a ProblemDetail (RFC 9457):
 * <pre>
 * { "title": "Not Found", "status": 404, "detail": "TaskList not found: 0193..." }
 * </pre>
 * Content-Type is application/problem+json. "type" is left out when it's the
 * default "about:blank" (= "nothing more than the HTTP status").
 * @RestControllerAdvice = a bean whose @ExceptionHandler methods apply to all controllers.
 * <p>
 * Extending ResponseEntityExceptionHandler gives ProblemDetail responses for Spring
 * MVC's own errors for free: a bad UUID or enum in the path or query (400), an
 * unknown URL (404), a wrong HTTP method (405)... We override only what we want to change.
 * <p>
 * Rule for every handler: the response says what the CLIENT did wrong, never how the
 * server works inside (no SQL, class names or stack traces). Those go to the log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Named "log" because the base class already has a field called "logger".
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** One invalid field in a 400 response, e.g. {"field": "title", "message": "Title is required"}. */
    public record FieldViolation(String field, String message) {
    }

    // Our own exception -> 404. Returning ProblemDetail is enough: Spring sets the
    // response status from it.
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // @Valid failed -> 400. The default body says nothing about which field failed,
    // so add an "errors" list, sorted by field so the output is stable.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();

        ProblemDetail body = ex.getBody();          // the ProblemDetail Spring already built
        body.setTitle("Validation failed");
        body.setDetail("The request has invalid fields.");
        body.setProperty("errors", errors);         // extra members are allowed by RFC 9457
        return super.handleMethodArgumentNotValid(ex, headers, status, request);
    }

    // The body couldn't be turned into the record -> 400. The default detail is just
    // "Failed to read request", so say which field and value were wrong when we can:
    //   {"priority": "URGENT"}  -> Invalid value 'URGENT' for 'priority'. Allowed: HIGH, MEDIUM, LOW.
    //   {"dueDate": "2026-13-45"} -> Invalid value '2026-13-45' for 'dueDate'.
    //   {"title":                 -> The request body is not valid JSON.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        String detail = "The request body is not valid JSON.";
        if (ex.getCause() instanceof InvalidFormatException invalid) {   // valid JSON, wrong value
            detail = "Invalid value '" + invalid.getValue() + "' for '" + fieldPath(invalid) + "'.";
            Class<?> type = invalid.getTargetType();
            if (type != null && type.isEnum()) {
                detail += " Allowed: " + Arrays.stream(type.getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(", ")) + ".";
            }
        }
        // Unlike MethodArgumentNotValidException, this exception carries no ProblemDetail
        // of its own to edit, so we build one and pass it to the base class.
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    // ?sort=foo on a paged endpoint: Spring Data can't find a "foo" property -> 400.
    // Without this it was a 500, although the client caused it.
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleUnknownProperty(PropertyReferenceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Unknown property '" + ex.getPropertyName() + "' (check the sort parameter).");
    }

    // A database constraint rejected the write (foreign key, NOT NULL, CHECK) -> 409.
    // Validation catches almost everything first, so this is mostly races, e.g. a task
    // created while its list is being deleted. The SQL error stays in the log only.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The request conflicts with the current state of the data.");
    }

    // Safety net: anything not handled above -> 500 with a generic message.
    // The full stack trace goes to the log, so the bug can be found; the client
    // never sees it. More specific handlers (above and in the base class) win.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.");
    }

    /** "priority", or "items[2].name" for nested fields. */
    private static String fieldPath(JacksonException ex) {
        return ex.getPath().stream()
                .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."))
                .replace(".[", "[");
    }
}
