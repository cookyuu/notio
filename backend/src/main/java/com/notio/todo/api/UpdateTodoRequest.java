package com.notio.todo.api;

import com.notio.todo.domain.TodoStatus;
import jakarta.validation.constraints.Size;

public record UpdateTodoRequest(
        @Size(max = 500) String title,
        String description,
        TodoStatus status
) {
}

