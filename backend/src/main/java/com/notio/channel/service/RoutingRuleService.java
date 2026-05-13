package com.notio.channel.service;

import com.notio.channel.domain.DeliveryMode;
import com.notio.channel.domain.RoutingCondition;
import com.notio.channel.domain.RoutingRule;
import com.notio.channel.repository.RoutingRuleRepository;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutingRuleService {

    private final RoutingRuleRepository routingRuleRepository;

    public List<RoutingRule> findAll(Long userId) {
        return routingRuleRepository.findAllByUserIdOrderByPriorityOrder(userId);
    }

    public RoutingRule findById(Long userId, Long ruleId) {
        RoutingRule rule = routingRuleRepository.findById(ruleId)
            .orElseThrow(() -> new NotioException(ErrorCode.ROUTING_RULE_NOT_FOUND));
        if (!rule.getUserId().equals(userId)) {
            throw new NotioException(ErrorCode.FORBIDDEN);
        }
        return rule;
    }

    @Transactional
    public RoutingRule create(
        Long userId,
        String ruleName,
        int priorityOrder,
        RoutingCondition conditions,
        List<Long> channelIds,
        boolean stopOnMatch,
        DeliveryMode deliveryMode,
        Integer digestIntervalMin
    ) {
        validateDigestInterval(deliveryMode, digestIntervalMin);

        RoutingRule rule = RoutingRule.builder()
            .userId(userId)
            .ruleName(ruleName)
            .priorityOrder(priorityOrder)
            .conditions(conditions != null ? conditions : RoutingCondition.empty())
            .channelIds(channelIds != null ? channelIds : List.of())
            .stopOnMatch(stopOnMatch)
            .deliveryMode(deliveryMode)
            .digestIntervalMin(digestIntervalMin)
            .build();

        return routingRuleRepository.save(rule);
    }

    @Transactional
    public RoutingRule update(
        Long userId,
        Long ruleId,
        String ruleName,
        int priorityOrder,
        RoutingCondition conditions,
        List<Long> channelIds,
        boolean stopOnMatch,
        boolean isEnabled,
        DeliveryMode deliveryMode,
        Integer digestIntervalMin
    ) {
        validateDigestInterval(deliveryMode, digestIntervalMin);
        RoutingRule rule = findById(userId, ruleId);
        rule.update(ruleName, priorityOrder, conditions, channelIds, stopOnMatch, isEnabled, deliveryMode, digestIntervalMin);
        return routingRuleRepository.save(rule);
    }

    @Transactional
    public void delete(Long userId, Long ruleId) {
        RoutingRule rule = findById(userId, ruleId);
        routingRuleRepository.delete(rule);
    }

    @Transactional
    public void reorder(Long userId, List<Long> orderedIds) {
        List<RoutingRule> rules = routingRuleRepository.findAllByUserIdOrderByPriorityOrder(userId);
        for (int i = 0; i < orderedIds.size(); i++) {
            final int order = i;
            rules.stream()
                .filter(r -> r.getId().equals(orderedIds.get(order)))
                .findFirst()
                .ifPresent(r -> r.updatePriorityOrder(order));
        }
        routingRuleRepository.saveAll(rules);
    }

    private void validateDigestInterval(DeliveryMode deliveryMode, Integer digestIntervalMin) {
        if (deliveryMode == DeliveryMode.DIGEST && digestIntervalMin == null) {
            throw new NotioException(ErrorCode.DIGEST_INTERVAL_REQUIRED);
        }
    }
}
