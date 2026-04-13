package com.notio.todo.dto;

import com.notio.todo.domain.TodoStatus;
import jakarta.validation.constraints.Size;

public record UpdateTodoRequest(
        @Size(max = 500) String title,
        String description,
        TodoStatus status
) {
}

