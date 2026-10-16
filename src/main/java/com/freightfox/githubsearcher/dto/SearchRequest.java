package com.freightfox.githubsearcher.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchRequest(
        @Schema(example = "spring boot", description = "Repository name (partial or full)")
        @NotBlank(message = "query must not be blank")
        @Size(max = 256, message = "query must be at most 256 characters")
        String query,

        @Schema(example = "Java", description = "Programming language (optional)")
        @Size(max = 50, message = "language must be at most 50 characters")
        String language,

        @Schema(example = "stars", description = "stars | forks | updated (default: stars)")
        String sort) {
}
