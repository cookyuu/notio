package com.notio.connection.repository;

import com.notio.connection.domain.ConnectionCredential;
import com.notio.connection.domain.ConnectionAuthType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectionCredentialRepository extends JpaRepository<ConnectionCredential, Long> {

    @Query("SELECT cc FROM ConnectionCredential cc WHERE cc.deletedAt IS NULL AND cc.connectionId = :connectionId")
    List<ConnectionCredential> findAllActiveByConnectionId(@Param("connectionId") Long connectionId);

    @Query("SELECT cc FROM ConnectionCredential cc " +
        "WHERE cc.deletedAt IS NULL AND cc.revokedAt IS NULL " +
        "AND cc.connectionId = :connectionId AND cc.authType = :authType")
    Optional<ConnectionCredential> findActiveByConnectionIdAndAuthType(
        @Param("connectionId") Long connectionId,
        @Param("authType") ConnectionAuthType authType
    );

    @Query("SELECT cc FROM ConnectionCredential cc " +
        "WHERE cc.deletedAt IS NULL AND cc.revokedAt IS NULL " +
        "AND cc.keyPrefix = :keyPrefix AND cc.authType = :authType")
    Optional<ConnectionCredential> findActiveByKeyPrefixAndAuthType(
        @Param("keyPrefix") String keyPrefix,
        @Param("authType") ConnectionAuthType authType
    );
}
