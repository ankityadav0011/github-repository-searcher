package com.freightfox.githubsearcher.controller;

import com.freightfox.githubsearcher.dto.RepositoryDto;
import com.freightfox.githubsearcher.dto.RepositoryListResponse;
import com.freightfox.githubsearcher.dto.SearchRequest;
import com.freightfox.githubsearcher.dto.SearchResponse;
import com.freightfox.githubsearcher.service.GithubSearchService;
import com.freightfox.githubsearcher.service.RepositoryStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/github")
@Tag(name = "GitHub Repositories")
public class GithubController {

    private final GithubSearchService searchService;
    private final RepositoryStoreService storeService;

    public GithubController(GithubSearchService searchService, RepositoryStoreService storeService) {
        this.searchService = searchService;
        this.storeService = storeService;
    }

    @Operation(summary = "Search GitHub and save the results",
            description = "Calls the GitHub search API, upserts the repositories into PostgreSQL and returns them.")
    @PostMapping("/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return searchService.searchAndSave(request);
    }

    @Operation(summary = "Get stored repositories",
            description = "Returns repositories already saved in the database, with optional filters.")
    @GetMapping("/repositories")
    public RepositoryListResponse repositories(
            @Parameter(description = "Filter by language, e.g. Java")
            @RequestParam(required = false) String language,
            @Parameter(description = "Minimum stars count")
            @RequestParam(required = false) Long minStars,
            @Parameter(description = "stars | forks | updated (default stars)")
            @RequestParam(required = false, defaultValue = "stars") String sort) {
        List<RepositoryDto> repos = storeService.findStored(language, minStars, sort);
        return new RepositoryListResponse(repos.size(), repos);
    }
}
