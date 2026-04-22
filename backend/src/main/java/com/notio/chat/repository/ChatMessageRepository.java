package com.notio.chat.repository;

import com.notio.chat.domain.ChatMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.deletedAt IS NULL
              AND m.userId = :userId
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    List<ChatMessage> findRecentByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
