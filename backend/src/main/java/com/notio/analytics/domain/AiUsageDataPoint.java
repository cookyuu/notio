package com.notio.analytics.domain;

/**
 * 기간별 AI 토큰 사용량 트렌드 집계 Projection.
 * Native SQL 쿼리의 결과를 Spring Data JPA가 매핑하기 위한 인터페이스.
 */
public interface AiUsageDataPoint {

    String getPeriodLabel();

    long getTotalInput();

    long getTotalOutput();

    long getSessionCount();
}
