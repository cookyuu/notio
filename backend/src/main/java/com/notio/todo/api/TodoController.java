package com.notio.todo.api;

import com.notio.common.api.ApiResponse;
import com.notio.todo.application.TodoService;
import com.notio.todo.domain.TodoStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(final TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping
    public ApiResponse<TodoResponse> create(@Valid @RequestBody final CreateTodoRequest request) {
        return ApiResponse.success(TodoResponse.from(todoService.createFromNotification(request)));
    }

    @GetMapping
    public ApiResponse<List<TodoResponse>> findAll(
            @RequestParam(required = false) final TodoStatus status
    ) {
        return ApiResponse.success(
                todoService.findAll(status).stream().map(TodoResponse::from).toList()
        );
    }

    @PatchMapping("/{id}")
    public ApiResponse<TodoResponse> update(
            @PathVariable("id") final long id,
            @Valid @RequestBody final UpdateTodoRequest request
    ) {
        return ApiResponse.success(TodoResponse.from(todoService.update(id, request)));
    }
}
