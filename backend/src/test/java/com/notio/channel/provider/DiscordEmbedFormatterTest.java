package com.notio.channel.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.notio.notification.domain.NotificationPriority;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

class DiscordEmbedFormatterTest {

    @Test
    @SuppressWarnings("unchecked")
    void urgentPriorityMapsToDecimalRed() {
        Map<NotificationPriority, Integer> colors = getColors();
        assertThat(colors.get(NotificationPriority.URGENT)).isEqualTo(16711680);
    }

    @Test
    @SuppressWarnings("unchecked")
    void highPriorityMapsToDecimalOrange() {
        Map<NotificationPriority, Integer> colors = getColors();
        assertThat(colors.get(NotificationPriority.HIGH)).isEqualTo(16744448);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mediumPriorityMapsToDecimalBlue() {
        Map<NotificationPriority, Integer> colors = getColors();
        assertThat(colors.get(NotificationPriority.MEDIUM)).isEqualTo(4886754);
    }

    @Test
    @SuppressWarnings("unchecked")
    void lowPriorityMapsToDecimalGrey() {
        Map<NotificationPriority, Integer> colors = getColors();
        assertThat(colors.get(NotificationPriority.LOW)).isEqualTo(10197915);
    }

    @Test
    @SuppressWarnings("unchecked")
    void allPrioritiesHaveColorMappings() {
        Map<NotificationPriority, Integer> colors = getColors();
        assertThat(colors).containsKeys(
            NotificationPriority.URGENT,
            NotificationPriority.HIGH,
            NotificationPriority.MEDIUM,
            NotificationPriority.LOW
        );
    }

    @SuppressWarnings("unchecked")
    private Map<NotificationPriority, Integer> getColors() {
        Field field = ReflectionUtils.findField(DiscordChannelProvider.class, "COLORS");
        assertThat(field).isNotNull();
        ReflectionUtils.makeAccessible(field);
        return (Map<NotificationPriority, Integer>) ReflectionUtils.getField(field, null);
    }
}
