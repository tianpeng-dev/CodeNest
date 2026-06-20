package com.codenest.backend.admin.dto;

import java.util.List;
import java.util.Map;

public record AdminAnalyticsDto(
    List<AdminMetricDto> metrics,
    Map<String, Long> postsByStatus,
    Map<String, Long> usersByStatus,
    Map<String, Long> sensitiveHitsByLevel) {}
