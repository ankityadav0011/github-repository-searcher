package com.freightfox.githubsearcher.repository;

import com.freightfox.githubsearcher.entity.GithubRepository;
import org.springframework.data.jpa.domain.Specification;

/** Building blocks for the optional filters. A null/blank filter simply adds no condition. */
public final class RepositorySpecifications {

    private RepositorySpecifications() {
    }

    public static Specification<GithubRepository> hasLanguage(String language) {
        return (root, query, cb) -> {
            if (language == null || language.isBlank()) {
                return null;
            }
            return cb.equal(cb.lower(root.get("language")), language.trim().toLowerCase());
        };
    }

    public static Specification<GithubRepository> hasMinStars(Long minStars) {
        return (root, query, cb) -> {
            if (minStars == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("stargazersCount"), minStars.intValue());
        };
    }
}
