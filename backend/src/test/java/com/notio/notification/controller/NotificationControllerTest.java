package com.notio.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.common.response.ApiResponse;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.dto.NotificationResponse;
import com.notio.notification.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void getNotificationsReturnsPagedNotifications() {
        final NotificationController controller = new NotificationController(notificationService);
        final Notification notification = Notification.builder()
                .source(NotificationSource.GITHUB)
                .title("PR opened")
                .body("A new PR is ready")
                .priority(NotificationPriority.HIGH)
                .externalId("gh-1")
                .externalUrl("https://github.com")
                .metadata("{\"repo\":\"notio\"}")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(notificationService.findAll(any(), any(), any(), any())).thenReturn(new PageImpl<>(
                List.of(notification),
                PageRequest.of(0, 20),
                1
        ));

        final ApiResponse<Page<NotificationResponse>> response = controller.getNotifications(
                null,
                null,
                0,
                20,
                new UsernamePasswordAuthenticationToken("10", null)
        );

        assertThat(response.success()).isTrue();
        assertThat(response.data().getContent()).hasSize(1);
        assertThat(response.data().getContent().getFirst().source()).isEqualTo(NotificationSource.GITHUB.name());
        verify(notificationService).findAll(eq(10L), any(), any(), any());
    }
}
