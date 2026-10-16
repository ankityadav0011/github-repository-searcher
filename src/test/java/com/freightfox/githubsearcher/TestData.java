package com.freightfox.githubsearcher;

import com.freightfox.githubsearcher.dto.GithubRepoItem;

import java.time.Instant;

public final class TestData {

    private TestData() {
    }

    public static GithubRepoItem item(long id, String name, String language, int stars, int forks) {
        return new GithubRepoItem(id, name, "owner/" + name, "desc of " + name,
                "https://github.com/owner/" + name, new GithubRepoItem.Owner("owner"),
                language, stars, forks, stars, 3, 120L,
                Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T12:00:00Z"),
                Instant.parse("2024-01-02T12:00:00Z"));
    }
}
