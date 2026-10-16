package com.freightfox.githubsearcher;

import com.freightfox.githubsearcher.client.GithubApiClient;
import com.freightfox.githubsearcher.dto.GithubSearchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.freightfox.githubsearcher.TestData.item;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Whole app (controller -> service -> H2 database) with only GitHub itself mocked. */
@SpringBootTest
@AutoConfigureMockMvc
class GithubSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GithubApiClient githubApiClient;

    @Test
    void searchTwiceUpdatesInsteadOfDuplicatingThenFiltersWork() throws Exception {
        // 1st search
        when(githubApiClient.searchRepositories(anyString(), any(), any())).thenReturn(
                new GithubSearchResponse(2, List.of(
                        item(9001L, "alpha", "Java", 100, 5),
                        item(9002L, "beta", "Kotlin", 300, 50))));
        search();

        // 2nd search: alpha now has more stars
        when(githubApiClient.searchRepositories(anyString(), any(), any())).thenReturn(
                new GithubSearchResponse(1, List.of(item(9001L, "alpha", "Java", 999, 5))));
        search();

        mockMvc.perform(get("/api/github/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))                       // no duplicate row
                .andExpect(jsonPath("$.repositories[0].id").value(9001))       // sorted by stars desc
                .andExpect(jsonPath("$.repositories[0].stars").value(999));    // updated value

        mockMvc.perform(get("/api/github/repositories").param("language", "kotlin").param("minStars", "200"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.repositories[0].name").value("beta"));

        mockMvc.perform(get("/api/github/repositories").param("minStars", "5000"))
                .andExpect(jsonPath("$.count").value(0));
    }

    private void search() throws Exception {
        mockMvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"anything\",\"sort\":\"stars\"}"))
                .andExpect(status().isOk());
    }
}
