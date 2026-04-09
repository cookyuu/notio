package com.notio.notification.application;

public interface NotificationIngestionService {

    long saveFromEvent(NotificationEvent event);
}
