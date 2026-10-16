package com.freightfox.githubsearcher.entity;

import com.freightfox.githubsearcher.dto.GithubRepoItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

/**
 * One row per GitHub repository. The primary key is GitHub's own repository id, so saving the
 * same repository twice updates the row instead of creating a duplicate (upsert).
 */
@Entity
@Table(name = "github_repositories", indexes = {
        @Index(name = "idx_repo_language", columnList = "language"),
        @Index(name = "idx_repo_stars", columnList = "stargazers_count")
})
public class GithubRepository implements Persistable<Long> {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "language")
    private String language;

    @Column(name = "stargazers_count", nullable = false)
    private int stargazersCount;

    @Column(name = "forks_count", nullable = false)
    private int forksCount;

    @Column(name = "watchers_count", nullable = false)
    private int watchersCount;

    @Column(name = "open_issues_count", nullable = false)
    private int openIssuesCount;

    @Column(name = "size_kb", nullable = false)
    private long sizeKb;

    @Column(name = "html_url")
    private String htmlUrl;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Column(name = "pushed_at")
    private Instant pushedAt;

    @Column(name = "fetched_at")
    private Instant fetchedAt;

    /** Tells Spring Data whether to INSERT (true) or UPDATE (false) without an extra SELECT. */
    @Transient
    private boolean isNew = true;

    /** Factory method: create a new entity from a GitHub API item. */
    public static GithubRepository from(GithubRepoItem item) {
        GithubRepository repo = new GithubRepository();
        repo.id = item.id();
        repo.applyFrom(item);
        return repo;
    }

    /** Copy the latest values from GitHub onto this entity. */
    public void applyFrom(GithubRepoItem item) {
        this.name = item.name();
        this.fullName = item.fullName();
        this.description = truncate(item.description(), 2000);
        this.ownerName = item.owner() != null ? item.owner().login() : null;
        this.language = item.language();
        this.stargazersCount = item.stars();
        this.forksCount = item.forks();
        this.watchersCount = item.watchers();
        this.openIssuesCount = item.openIssues();
        this.sizeKb = item.size();
        this.htmlUrl = item.htmlUrl();
        this.createdAt = item.createdAt();
        this.lastUpdated = item.updatedAt();
        this.pushedAt = item.pushedAt();
        this.fetchedAt = Instant.now();
    }

    private static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getName() { return name; }
    public String getFullName() { return fullName; }
    public String getDescription() { return description; }
    public String getOwnerName() { return ownerName; }
    public String getLanguage() { return language; }
    public int getStargazersCount() { return stargazersCount; }
    public int getForksCount() { return forksCount; }
    public int getWatchersCount() { return watchersCount; }
    public int getOpenIssuesCount() { return openIssuesCount; }
    public long getSizeKb() { return sizeKb; }
    public String getHtmlUrl() { return htmlUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUpdated() { return lastUpdated; }
    public Instant getPushedAt() { return pushedAt; }
    public Instant getFetchedAt() { return fetchedAt; }
}
