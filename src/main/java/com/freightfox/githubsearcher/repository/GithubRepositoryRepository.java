package com.freightfox.githubsearcher.repository;

import com.freightfox.githubsearcher.entity.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GithubRepositoryRepository
        extends JpaRepository<GithubRepository, Long>, JpaSpecificationExecutor<GithubRepository> {
}
