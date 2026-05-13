package com.notio.channel;

import com.notio.channel.domain.RoutingCondition;
import com.notio.channel.domain.RoutingRule;
import com.notio.notification.domain.Notification;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoutingRuleEvaluator {

    public boolean matches(RoutingRule rule, Notification notification) {
        RoutingCondition cond = rule.getConditions();
        if (cond == null) {
            return true;
        }

        boolean sourceMatch = isNullOrEmpty(cond.sources()) ||
            cond.sources().contains(notification.getSource().name());
        boolean priorityMatch = isNullOrEmpty(cond.priorities()) ||
            cond.priorities().contains(notification.getPriority().name());

        return sourceMatch && priorityMatch;
    }

    private boolean isNullOrEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
