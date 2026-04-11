package com.notio.notification.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.notio.common.api.ApiResponse;
import com.notio.common.api.PageMeta;
import com.notio.notification.application.NotificationService;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void findAllReturnsPagedNotifications() {
        final NotificationController controller = new NotificationController(notificationService);
        final Notification notification = new Notification(
                NotificationSource.GITHUB,
                "PR opened",
                "A new PR is ready",
                NotificationPriority.HIGH,
                "gh-1",
                "https://github.com",
                Map.of("repo", "notio")
        );
        ReflectionTestUtils.setField(notification, "id", 1L);
        when(notificationService.findAll(any(), any())).thenReturn(new PageImpl<>(
                List.of(notification),
                PageRequest.of(0, 20),
                1
        ));

        final ApiResponse<List<NotificationResponse>> response = controller.findAll(
                0,
                20,
                null,
                null,
                null,
                null
        );

        assertThat(response.success()).isTrue();
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().source()).isEqualTo(NotificationSource.GITHUB);
        assertThat(response.meta()).isEqualTo(new PageMeta(0, 20, 1, 1));
    }
}
