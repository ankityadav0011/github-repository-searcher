package com.freightfox.githubsearcher.client;

import com.freightfox.githubsearcher.config.GithubProperties;
import com.freightfox.githubsearcher.dto.GithubSearchResponse;
import com.freightfox.githubsearcher.entity.SortField;
import com.freightfox.githubsearcher.exception.GithubApiException;
import com.freightfox.githubsearcher.exception.InvalidRequestException;
import com.freightfox.githubsearcher.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubApiClientImplTest {

    private static final String SAMPLE = """
            {"total_count": 1, "incomplete_results": false, "items": [
              {"id": 123456, "name": "spring-boot-example", "full_name": "user123/spring-boot-example",
               "description": "An example repository for Spring Boot",
               "html_url": "https://github.com/user123/spring-boot-example",
               "owner": {"login": "user123", "id": 1}, "language": "Java",
               "stargazers_count": 450, "forks_count": 120, "watchers_count": 450,
               "open_issues_count": 7, "size": 900,
               "created_at": "2020-01-01T00:00:00Z", "updated_at": "2024-01-01T12:00:00Z",
               "pushed_at": "2024-01-02T12:00:00Z", "some_unknown_field": true}
            ]}
            """;

    private MockRestServiceServer server;
    private GithubApiClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GithubApiClientImpl(builder.build(),
                new GithubProperties("https://api.github.com", "", 30, 5000, 10000));
    }

    @Test
    void parsesSuccessfulResponse() {
        server.expect(requestTo(containsString("/search/repositories")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(SAMPLE, MediaType.APPLICATION_JSON));

        GithubSearchResponse response = client.searchRepositories("spring boot", "Java", SortField.STARS);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).id()).isEqualTo(123456L);
        assertThat(response.items().get(0).owner().login()).isEqualTo("user123");
        assertThat(response.items().get(0).stars()).isEqualTo(450);
        server.verify();
    }

    @Test
    void handlesEmptyResults() {
        server.expect(requestTo(containsString("/search/repositories")))
                .andRespond(withSuccess("{\"total_count\":0,\"items\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.searchRepositories("zzzz", null, SortField.STARS).items()).isEmpty();
    }

    @Test
    void rateLimitWithZeroRemainingThrowsRateLimitException() {
        server.expect(requestTo(containsString("/search/repositories")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .header("X-RateLimit-Remaining", "0")
                        .header("Retry-After", "42"));

        assertThatThrownBy(() -> client.searchRepositories("spring", null, SortField.STARS))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> assertThat(((RateLimitExceededException) ex).getRetryAfterSeconds()).isEqualTo(42L));
    }

    @Test
    void http429ThrowsRateLimitException() {
        server.expect(requestTo(containsString("/search/repositories")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.searchRepositories("spring", null, SortField.STARS))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void http422ThrowsInvalidRequest() {
        server.expect(requestTo(containsString("/search/repositories")))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> client.searchRepositories("spring", null, SortField.STARS))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void serverErrorThrowsGithubApiException() {
        server.expect(requestTo(containsString("/search/repositories")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.searchRepositories("spring", null, SortField.STARS))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("500");
    }

    @Test
    void malformedJsonThrowsGithubApiException() {
        server.expect(requestTo(containsString("/search/repositories")))
                .andRespond(withSuccess("this is not json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.searchRepositories("spring", null, SortField.STARS))
                .isInstanceOf(GithubApiException.class);
    }

    @Test
    void buildQueryAddsNameAndLanguageQualifiers() {
        assertThat(GithubApiClientImpl.buildQuery(" spring boot ", "Java"))
                .isEqualTo("spring boot in:name language:Java");
        assertThat(GithubApiClientImpl.buildQuery("spring", null)).isEqualTo("spring in:name");
        assertThat(GithubApiClientImpl.buildQuery("x", "Jupyter Notebook"))
                .isEqualTo("x in:name language:\"Jupyter Notebook\"");
    }
}
