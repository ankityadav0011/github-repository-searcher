package com.freightfox.githubsearcher.dto;

import java.time.Instant;

public record RepositoryDto(
        Long id,
        String name,
        String fullName,
        String description,
        String owner,
        String language,
        int stars,
        int forks,
        int watchers,
        int openIssues,
        long sizeKb,
        String url,
        Instant createdAt,
        Instant lastUpdated,
        Instant pushedAt) {
}
