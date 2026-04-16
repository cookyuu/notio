package com.notio.connection.repository;

import com.notio.connection.domain.Connection;
import com.notio.connection.domain.ConnectionProvider;
import com.notio.connection.domain.ConnectionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    @Query("SELECT c FROM Connection c WHERE c.deletedAt IS NULL AND c.userId = :userId")
    List<Connection> findAllByUserIdAndNotDeleted(@Param("userId") Long userId);

    @Query("SELECT c FROM Connection c WHERE c.deletedAt IS NULL AND c.userId = :userId AND c.id = :id")
    Optional<Connection> findByUserIdAndIdAndNotDeleted(
        @Param("userId") Long userId,
        @Param("id") Long id
    );

    boolean existsByUserIdAndProviderAndStatusAndDeletedAtIsNull(
        Long userId,
        ConnectionProvider provider,
        ConnectionStatus status
    );
}
