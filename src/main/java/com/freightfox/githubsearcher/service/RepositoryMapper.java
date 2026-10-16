package com.freightfox.githubsearcher.service;

import com.freightfox.githubsearcher.dto.RepositoryDto;
import com.freightfox.githubsearcher.entity.GithubRepository;

public final class RepositoryMapper {

    private RepositoryMapper() {
    }

    public static RepositoryDto toDto(GithubRepository r) {
        return new RepositoryDto(
                r.getId(),
                r.getName(),
                r.getFullName(),
                r.getDescription(),
                r.getOwnerName(),
                r.getLanguage(),
                r.getStargazersCount(),
                r.getForksCount(),
                r.getWatchersCount(),
                r.getOpenIssuesCount(),
                r.getSizeKb(),
                r.getHtmlUrl(),
                r.getCreatedAt(),
                r.getLastUpdated(),
                r.getPushedAt());
    }
}
