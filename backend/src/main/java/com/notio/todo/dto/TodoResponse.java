package com.notio.todo.dto;

import com.notio.todo.domain.Todo;
import com.notio.todo.domain.TodoStatus;
import java.time.OffsetDateTime;

public record TodoResponse(
        long id,
        String title,
        String description,
        TodoStatus status,
        Long notificationId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static TodoResponse from(final Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.getStatus(),
                todo.getNotificationId(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }
}

