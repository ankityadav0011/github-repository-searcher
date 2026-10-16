package com.freightfox.githubsearcher.client;

import com.freightfox.githubsearcher.config.GithubProperties;
import com.freightfox.githubsearcher.dto.GithubSearchResponse;
import com.freightfox.githubsearcher.entity.SortField;
import com.freightfox.githubsearcher.exception.GithubApiException;
import com.freightfox.githubsearcher.exception.InvalidRequestException;
import com.freightfox.githubsearcher.exception.RateLimitExceededException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.Map;

@Component
public class GithubApiClientImpl implements GithubApiClient {

    private final RestClient restClient;
    private final GithubProperties properties;

    public GithubApiClientImpl(RestClient githubRestClient, GithubProperties properties) {
        this.restClient = githubRestClient;
        this.properties = properties;
    }

    @Override
    public GithubSearchResponse searchRepositories(String query, String language, SortField sort) {
        String q = buildQuery(query, language);
        try {
            GithubSearchResponse body = restClient.get()
                    .uri("/search/repositories?q={q}&sort={sort}&order=desc&per_page={perPage}",
                            Map.of("q", q, "sort", sort.getApiValue(), "perPage", properties.perPage()))
                    .retrieve()
                    .body(GithubSearchResponse.class);
            if (body == null) {
                throw new GithubApiException("GitHub returned an empty response");
            }
            return body;
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (ResourceAccessException ex) {
            throw new GithubApiException("Unable to reach the GitHub API (network error or timeout)", ex);
        } catch (RestClientException ex) {
            throw new GithubApiException("GitHub returned a response that could not be read", ex);
        }
    }

    /** "spring boot" + "Java" -> "spring boot in:name language:Java" */
    static String buildQuery(String query, String language) {
        StringBuilder q = new StringBuilder(query.trim()).append(" in:name");
        if (language != null && !language.isBlank()) {
            String lang = language.trim();
            q.append(" language:").append(lang.contains(" ") ? "\"" + lang + "\"" : lang);
        }
        return q.toString();
    }

    private RuntimeException translate(RestClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        HttpHeaders headers = ex.getResponseHeaders();

        if (isRateLimited(status, headers, ex.getResponseBodyAsString())) {
            return new RateLimitExceededException(
                    "GitHub API rate limit exceeded. Please retry later.", retryAfterSeconds(headers));
        }
        if (status.value() == 422) {
            return new InvalidRequestException("GitHub rejected the search query. Please check 'query' and 'language'.");
        }
        if (status.value() == 401) {
            return new GithubApiException("GitHub authentication failed. Check the GITHUB_TOKEN value.");
        }
        return new GithubApiException("GitHub API responded with HTTP " + status.value());
    }

    private boolean isRateLimited(HttpStatusCode status, HttpHeaders headers, String body) {
        if (status.value() == 429) {
            return true;
        }
        if (status.value() != 403) {
            return false;
        }
        boolean remainingIsZero = headers != null && "0".equals(headers.getFirst("X-RateLimit-Remaining"));
        boolean hasRetryAfter = headers != null && headers.getFirst(HttpHeaders.RETRY_AFTER) != null;
        boolean bodyMentionsLimit = body != null && body.toLowerCase().contains("rate limit");
        return remainingIsZero || hasRetryAfter || bodyMentionsLimit;
    }

    private Long retryAfterSeconds(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        try {
            String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
            if (retryAfter != null) {
                return Long.parseLong(retryAfter.trim());
            }
            String reset = headers.getFirst("X-RateLimit-Reset");
            if (reset != null) {
                return Math.max(0, Long.parseLong(reset.trim()) - Instant.now().getEpochSecond());
            }
        } catch (NumberFormatException ignored) {
            // fall through: header was not a number
        }
        return null;
    }
}
