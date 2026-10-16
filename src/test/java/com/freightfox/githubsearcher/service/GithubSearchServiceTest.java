package com.freightfox.githubsearcher.service;

import com.freightfox.githubsearcher.client.GithubApiClient;
import com.freightfox.githubsearcher.dto.GithubRepoItem;
import com.freightfox.githubsearcher.dto.GithubSearchResponse;
import com.freightfox.githubsearcher.dto.SearchRequest;
import com.freightfox.githubsearcher.dto.SearchResponse;
import com.freightfox.githubsearcher.entity.GithubRepository;
import com.freightfox.githubsearcher.entity.SortField;
import com.freightfox.githubsearcher.exception.InvalidRequestException;
import com.freightfox.githubsearcher.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.freightfox.githubsearcher.TestData.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GithubSearchServiceTest {

    @Mock
    private GithubApiClient client;

    @Mock
    private RepositoryStoreService storeService;

    @InjectMocks
    private GithubSearchService service;

    @Test
    void fetchesSavesAndReturnsRepositories() {
        GithubRepoItem item = item(123456L, "spring-boot-example", "Java", 450, 120);
        when(client.searchRepositories("spring boot", "Java", SortField.STARS))
                .thenReturn(new GithubSearchResponse(1, List.of(item)));
        when(storeService.upsertAll(List.of(item))).thenReturn(List.of(GithubRepository.from(item)));

        SearchResponse response = service.searchAndSave(new SearchRequest(" spring boot ", "Java", "stars"));

        assertThat(response.message()).isEqualTo("Repositories fetched and saved successfully");
        assertThat(response.repositories()).hasSize(1);
        assertThat(response.repositories().get(0).id()).isEqualTo(123456L);
    }

    @Test
    void emptyResultsAreNotSavedAndReturnFriendlyMessage() {
        when(client.searchRepositories(anyString(), any(), any()))
                .thenReturn(new GithubSearchResponse(0, List.of()));

        SearchResponse response = service.searchAndSave(new SearchRequest("nothing-here", null, null));

        assertThat(response.repositories()).isEmpty();
        assertThat(response.message()).contains("No repositories found");
        verifyNoInteractions(storeService);
    }

    @Test
    void nullItemsListIsTreatedAsEmpty() {
        when(client.searchRepositories(anyString(), any(), any()))
                .thenReturn(new GithubSearchResponse(0, null));

        assertThat(service.searchAndSave(new SearchRequest("x", null, null)).repositories()).isEmpty();
    }

    @Test
    void defaultSortIsStars() {
        when(client.searchRepositories(anyString(), any(), eq(SortField.STARS)))
                .thenReturn(new GithubSearchResponse(0, List.of()));

        service.searchAndSave(new SearchRequest("x", null, null));

        verify(client).searchRepositories("x", null, SortField.STARS);
    }

    @Test
    void invalidSortNeverCallsGithub() {
        assertThatThrownBy(() -> service.searchAndSave(new SearchRequest("x", null, "bogus")))
                .isInstanceOf(InvalidRequestException.class);
        verify(client, never()).searchRepositories(anyString(), any(), any());
    }

    @Test
    void blankQueryIsRejected() {
        assertThatThrownBy(() -> service.searchAndSave(new SearchRequest("  ", null, null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rateLimitErrorsPropagate() {
        when(client.searchRepositories(anyString(), any(), any()))
                .thenThrow(new RateLimitExceededException("limit", 30L));

        assertThatThrownBy(() -> service.searchAndSave(new SearchRequest("x", null, null)))
                .isInstanceOf(RateLimitExceededException.class);
    }
}
