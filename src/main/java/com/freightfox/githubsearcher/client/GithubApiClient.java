package com.freightfox.githubsearcher.client;

import com.freightfox.githubsearcher.dto.GithubSearchResponse;
import com.freightfox.githubsearcher.entity.SortField;

/** Port to the outside world: lets tests replace the real GitHub with a mock. */
public interface GithubApiClient {

    GithubSearchResponse searchRepositories(String query, String language, SortField sort);
}
