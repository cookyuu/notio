package com.notio.connection.repository;

import com.notio.connection.domain.ConnectionEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectionEventRepository extends JpaRepository<ConnectionEvent, Long> {

    @Query("SELECT ce FROM ConnectionEvent ce " +
        "WHERE ce.deletedAt IS NULL AND ce.connectionId = :connectionId " +
        "ORDER BY ce.createdAt DESC")
    List<ConnectionEvent> findRecentByConnectionId(@Param("connectionId") Long connectionId);
}
