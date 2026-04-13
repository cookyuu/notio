package com.notio.todo.dto;

import jakarta.validation.constraints.Size;

public record CreateTodoRequest(
        @Size(max = 500) String title,
        String description,
        Long notificationId
) {
}

