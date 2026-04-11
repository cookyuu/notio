package com.notio.notification.infrastructure;

import com.notio.notification.domain.Notification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationRepository
        extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    Optional<Notification> findByIdAndDeletedAtIsNull(Long id);

    long countByIsReadFalseAndDeletedAtIsNull();
}

