package com.freightfox.githubsearcher.controller;

import com.freightfox.githubsearcher.dto.RepositoryDto;
import com.freightfox.githubsearcher.dto.SearchResponse;
import com.freightfox.githubsearcher.exception.GithubApiException;
import com.freightfox.githubsearcher.exception.InvalidRequestException;
import com.freightfox.githubsearcher.exception.RateLimitExceededException;
import com.freightfox.githubsearcher.service.GithubSearchService;
import com.freightfox.githubsearcher.service.RepositoryStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GithubController.class)
class GithubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GithubSearchService searchService;

    @MockBean
    private RepositoryStoreService storeService;

    private RepositoryDto sampleDto() {
        return new RepositoryDto(123456L, "spring-boot-example", "user123/spring-boot-example",
                "An example repository for Spring Boot", "user123", "Java", 450, 120, 450, 3, 100L,
                "https://github.com/user123/spring-boot-example",
                Instant.parse("2020-01-01T00:00:00Z"), Instant.parse("2024-01-01T12:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z"));
    }

    @Test
    void searchReturns200WithRepositories() throws Exception {
        when(searchService.searchAndSave(any()))
                .thenReturn(new SearchResponse("Repositories fetched and saved successfully", List.of(sampleDto())));

        mockMvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"spring boot\",\"language\":\"Java\",\"sort\":\"stars\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Repositories fetched and saved successfully"))
                .andExpect(jsonPath("$.repositories[0].id").value(123456))
                .andExpect(jsonPath("$.repositories[0].owner").value("user123"))
                .andExpect(jsonPath("$.repositories[0].stars").value(450))
                .andExpect(jsonPath("$.repositories[0].lastUpdated").value("2024-01-01T12:00:00Z"));
    }

    @Test
    void searchWithBlankQueryReturns400() throws Exception {
        mockMvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("query: query must not be blank"));
    }

    @Test
    void searchWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void searchWithInvalidSortReturns400() throws Exception {
        when(searchService.searchAndSave(any())).thenThrow(new InvalidRequestException("Invalid sort 'x'"));

        mockMvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"spring\",\"sort\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort 'x'"));
    }

    @Test
    void rateLimitReturns429WithRetryAfterHeader() throws Exception {
        when(searchService.searchAndSave(any())).thenThrow(new RateLimitExceededException("limit", 30L));

        mockMvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"spring\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"));
    }

    @Test
    void githubFailureReturns502() throws Exception {
        when(searchService.searchAndSave(any())).thenThrow(new GithubApiException("GitHub down"));

        mockMvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"spring\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("GitHub down"));
    }

    @Test
    void getRepositoriesPassesFiltersAndReturnsList() throws Exception {
        when(storeService.findStored(eq("Java"), eq(100L), eq("stars"))).thenReturn(List.of(sampleDto()));

        mockMvc.perform(get("/api/github/repositories")
                        .param("language", "Java").param("minStars", "100").param("sort", "stars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.repositories[0].name").value("spring-boot-example"));
    }

    @Test
    void getRepositoriesDefaultsSortToStars() throws Exception {
        when(storeService.findStored(any(), any(), eq("stars"))).thenReturn(List.of());

        mockMvc.perform(get("/api/github/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.repositories").isEmpty());
    }

    @Test
    void getRepositoriesWithNonNumericMinStarsReturns400() throws Exception {
        mockMvc.perform(get("/api/github/repositories").param("minStars", "abc"))
                .andExpect(status().isBadRequest());
    }
}
