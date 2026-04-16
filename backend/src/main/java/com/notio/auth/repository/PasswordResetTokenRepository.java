package com.notio.auth.repository;

import com.notio.auth.domain.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE PasswordResetToken prt
            SET prt.usedAt = CURRENT_TIMESTAMP
            WHERE prt.authIdentity.id = :authIdentityId
              AND prt.usedAt IS NULL
            """)
    void invalidateUnusedByAuthIdentityId(@Param("authIdentityId") Long authIdentityId);
}
