package com.example.tasktracker.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * The JSON shape of every paged collection in the API:
 * <pre>
 * { "content": [...], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3 }
 * </pre>
 * Why not return Spring's Page directly? Its JSON is an internal detail (pageable,
 * sort, numberOfElements, ...) that can change between versions, and Spring Data
 * logs a warning when it's serialized as-is. Our own record is a stable contract.
 *
 * @param <T> the item type, e.g. TaskResponse
 */
public record PageResponse<T>(
        List<T> content,
        int page,             // zero-based, same as the ?page= query parameter
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
