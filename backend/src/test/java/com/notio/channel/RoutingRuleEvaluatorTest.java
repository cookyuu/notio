package com.notio.channel;

import static org.assertj.core.api.Assertions.assertThat;

import com.notio.channel.domain.RoutingCondition;
import com.notio.channel.domain.RoutingRule;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoutingRuleEvaluatorTest {

    private final RoutingRuleEvaluator evaluator = new RoutingRuleEvaluator();

    @Test
    void matchesReturnsTrueWhenConditionsIsNull() {
        RoutingRule rule = RoutingRule.builder()
            .userId(1L).ruleName("test").conditions(null).build();
        Notification notification = notification(NotificationSource.GITHUB, NotificationPriority.HIGH);

        assertThat(evaluator.matches(rule, notification)).isTrue();
    }

    @Test
    void matchesReturnsTrueWhenBothSourcesAndPrioritiesAreNull() {
        RoutingRule rule = RoutingRule.builder()
            .userId(1L).ruleName("test")
            .conditions(new RoutingCondition(null, null)).build();
        Notification notification = notification(NotificationSource.GITHUB, NotificationPriority.HIGH);

        assertThat(evaluator.matches(rule, notification)).isTrue();
    }

    @Test
    void matchesReturnsTrueWhenBothSourcesAndPrioritiesAreEmpty() {
        RoutingRule rule = RoutingRule.builder()
            .userId(1L).ruleName("test")
            .conditions(new RoutingCondition(List.of(), List.of())).build();
        Notification notification = notification(NotificationSource.GITHUB, NotificationPriority.HIGH);

        assertThat(evaluator.matches(rule, notification)).isTrue();
    }

    @Test
    void matchesReturnsTrueWhenSourceMatchesAndPriorityIsNull() {
        RoutingRule rule = RoutingRule.builder()
            .userId(1L).ruleName("test")
            .conditions(new RoutingCondition(List.of("GITHUB"), null)).build();
        Notification notification = notification(NotificationSource.GITHUB, NotificationPriority.HIGH);

        assertThat(evaluator.matches(rule, notification)).isTrue();
    }

    @Test
    void matchesReturnsFalseWhenSourceDoesNotMatch() {
        RoutingRule rule = RoutingRule.builder()
            .userId(1L).ruleName("test")
            .conditions(new RoutingCondition(List.of("GITHUB"), null)).build();
        Notification notification = notification(NotificationSource.SLACK, NotificationPriority.HIGH);

        assertThat(evaluator.matches(rule, notification)).isFalse();
    }

    @Test
    void matchesReturnsTrueWhenPriorityMatchesAndSourceIsNull() {
        RoutingRule rule = RoutingRule.builder()
            .userId(1L).ruleName("test")
            .conditions(new RoutingCondition(null, List.of("HIGH"))).build();
        Notification notification = notification(NotificationSource.GITHUB, NotificationPriority.HIGH);

        assertThat(evaluator.matches(rule, notification)).isTrue();
    }

    @Test
    void matchesReturnsFalseWhenPriorityDoesNotMatch() {
        RoutingRule rule = RoutingRule.builder()
            .userId(1L).ruleName("test")
            .conditions(new RoutingCondition(null, List.of("URGENT"))).build();
        Notification notification = notification(NotificationSource.GITHUB, NotificationPriority.LOW);

        assertThat(evaluator.matches(rule, notification)).isFalse();
    }

    @Test
    void matchesReturnsTrueWhenBothSourceAndPriorityMatch() {
        RoutingRule rule = RoutingRule.builder()
            .userId(1L).ruleName("test")
            .conditions(new RoutingCondition(List.of("GITHUB", "SLACK"), List.of("HIGH", "URGENT"))).build();
        Notification notification = notification(NotificationSource.SLACK, NotificationPriority.URGENT);

        assertThat(evaluator.matches(rule, notification)).isTrue();
    }

    @Test
    void matchesReturnsFalseWhenSourceMatchesButPriorityDoesNot() {
        RoutingRule rule = RoutingRule.builder()
            .userId(1L).ruleName("test")
            .conditions(new RoutingCondition(List.of("GITHUB"), List.of("URGENT"))).build();
        Notification notification = notification(NotificationSource.GITHUB, NotificationPriority.LOW);

        assertThat(evaluator.matches(rule, notification)).isFalse();
    }

    private Notification notification(NotificationSource source, NotificationPriority priority) {
        return Notification.builder()
            .id(1L).userId(1L).source(source)
            .title("Test").body("Body").priority(priority)
            .build();
    }
}
