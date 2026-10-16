package com.freightfox.githubsearcher.dto;

import java.util.List;

public record RepositoryListResponse(int count, List<RepositoryDto> repositories) {
}
