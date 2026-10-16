package com.freightfox.githubsearcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Values come from the "github:" block in application.yml (or environment variables). */
@ConfigurationProperties(prefix = "github")
public record GithubProperties(
        @DefaultValue("https://api.github.com") String apiUrl,
        @DefaultValue("") String token,
        @DefaultValue("30") int perPage,
        @DefaultValue("5000") int connectTimeoutMs,
        @DefaultValue("10000") int readTimeoutMs) {
}
