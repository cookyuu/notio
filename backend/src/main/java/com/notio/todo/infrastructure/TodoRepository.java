package com.notio.todo.infrastructure;

import com.notio.todo.domain.Todo;
import com.notio.todo.domain.TodoStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Todo> findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(TodoStatus status);

    Optional<Todo> findByIdAndDeletedAtIsNull(Long id);
}

