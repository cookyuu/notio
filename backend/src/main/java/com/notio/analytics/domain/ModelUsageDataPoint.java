package com.notio.analytics.domain;

/**
 * 모델별 AI 토큰 사용량 분포 집계 Projection.
 * Native SQL 쿼리의 결과를 Spring Data JPA가 매핑하기 위한 인터페이스.
 */
public interface ModelUsageDataPoint {

    String getModel();

    long getTotalTokens();

    long getSessionCount();
}
