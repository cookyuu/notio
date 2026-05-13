package com.notio.channel.repository;

import com.notio.channel.domain.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutingRuleRepository extends JpaRepository<RoutingRule, Long> {

    List<RoutingRule> findByUserIdOrderByPriorityOrder(Long userId);

    List<RoutingRule> findAllByUserIdOrderByPriorityOrder(Long userId);
}
