package com.notio.auth.repository;

import com.notio.auth.domain.AuthIdentity;
import com.notio.auth.domain.AuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, Long> {

    @Query("""
            SELECT ai
            FROM AuthIdentity ai
            JOIN FETCH ai.user u
            WHERE ai.provider = :provider
              AND ai.email = :email
              AND ai.deletedAt IS NULL
              AND u.deletedAt IS NULL
              AND u.status = com.notio.auth.domain.UserStatus.ACTIVE
            """)
    Optional<AuthIdentity> findActiveByProviderAndEmail(
            @Param("provider") AuthProvider provider,
            @Param("email") String email);
}
