package com.freightfox.githubsearcher.service;

import com.freightfox.githubsearcher.client.GithubApiClient;
import com.freightfox.githubsearcher.dto.GithubRepoItem;
import com.freightfox.githubsearcher.dto.GithubSearchResponse;
import com.freightfox.githubsearcher.dto.RepositoryDto;
import com.freightfox.githubsearcher.dto.SearchRequest;
import com.freightfox.githubsearcher.dto.SearchResponse;
import com.freightfox.githubsearcher.entity.SortField;
import com.freightfox.githubsearcher.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/** Orchestrates: validate -> call GitHub -> save (upsert) -> build response. */
@Service
public class GithubSearchService {

    private final GithubApiClient githubApiClient;
    private final RepositoryStoreService storeService;

    public GithubSearchService(GithubApiClient githubApiClient, RepositoryStoreService storeService) {
        this.githubApiClient = githubApiClient;
        this.storeService = storeService;
    }

    public SearchResponse searchAndSave(SearchRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            throw new InvalidRequestException("query must not be blank");
        }
        SortField sort = SortField.from(request.sort());

        GithubSearchResponse result = githubApiClient.searchRepositories(
                request.query().trim(), request.language(), sort);

        List<GithubRepoItem> items = result.items() == null ? List.of()
                : result.items().stream().filter(i -> i != null && Objects.nonNull(i.id())).toList();

        if (items.isEmpty()) {
            return new SearchResponse("No repositories found for the given criteria", List.of());
        }

        List<RepositoryDto> saved = storeService.upsertAll(items).stream()
                .map(RepositoryMapper::toDto)
                .toList();
        return new SearchResponse("Repositories fetched and saved successfully", saved);
    }
}
