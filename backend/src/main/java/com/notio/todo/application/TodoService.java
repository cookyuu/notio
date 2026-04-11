package com.notio.todo.application;

import com.notio.common.error.ErrorCode;
import com.notio.common.error.NotioException;
import com.notio.notification.application.NotificationService;
import com.notio.notification.domain.Notification;
import com.notio.todo.api.CreateTodoRequest;
import com.notio.todo.api.UpdateTodoRequest;
import com.notio.todo.domain.Todo;
import com.notio.todo.domain.TodoStatus;
import com.notio.todo.infrastructure.TodoRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class TodoService {

    private final TodoRepository todoRepository;
    private final NotificationService notificationService;

    public TodoService(
            final TodoRepository todoRepository,
            final NotificationService notificationService
    ) {
        this.todoRepository = todoRepository;
        this.notificationService = notificationService;
    }

    public Todo createFromNotification(final CreateTodoRequest request) {
        final Notification notification = request.notificationId() == null
                ? null
                : notificationService.findById(request.notificationId());
        final String title = buildTitle(request.title(), notification);
        final Todo todo = new Todo(
                title,
                request.description(),
                TodoStatus.PENDING,
                request.notificationId()
        );
        return todoRepository.save(todo);
    }

    public List<Todo> findAll(final TodoStatus status) {
        if (status == null) {
            return todoRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc();
        }
        return todoRepository.findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status);
    }

    public Todo update(final long id, final UpdateTodoRequest request) {
        final Todo todo = todoRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotioException(ErrorCode.TODO_NOT_FOUND));
        todo.update(request.title(), request.description(), request.status());
        return todo;
    }

    private String buildTitle(final String requestedTitle, final Notification notification) {
        if (requestedTitle != null && !requestedTitle.isBlank()) {
            return requestedTitle;
        }
        if (notification != null) {
            return "Follow up: " + notification.getTitle();
        }
        return "Untitled task";
    }
}

