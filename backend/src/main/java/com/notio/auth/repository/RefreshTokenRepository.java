package com.notio.auth.repository;

import com.notio.auth.domain.RefreshToken;
import com.notio.auth.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰으로 유효한 RefreshToken 조회 (무효화되지 않은 토큰)
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revokedAt IS NULL")
    Optional<RefreshToken> findByTokenAndRevokedAtIsNull(@Param("token") String token);

    /**
     * 사용자의 모든 RefreshToken을 무효화 (로그아웃 시)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = CURRENT_TIMESTAMP WHERE rt.user = :user AND rt.revokedAt IS NULL")
    void revokeAllByUser(@Param("user") User user);

    /**
     * 만료된 토큰 삭제 (배치 작업용)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();
}
