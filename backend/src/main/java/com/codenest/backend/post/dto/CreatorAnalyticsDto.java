package com.codenest.backend.post.dto;

import java.util.List;

public record CreatorAnalyticsDto(
    int postCount,
    int publishedCount,
    int draftCount,
    int totalViews,
    int totalLikes,
    int totalFavorites,
    List<TrendPointDto> trend,
    List<PiePointDto> pie) {}
