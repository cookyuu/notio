package com.notio.notification.infrastructure;

import com.notio.notification.application.NotificationEvent;
import com.notio.notification.application.NotificationIngestionService;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class InMemoryNotificationIngestionService implements NotificationIngestionService {

    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public long saveFromEvent(final NotificationEvent event) {
        return sequence.incrementAndGet();
    }
}
