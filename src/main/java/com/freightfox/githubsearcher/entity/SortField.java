package com.freightfox.githubsearcher.entity;

import com.freightfox.githubsearcher.exception.InvalidRequestException;

/** Allowed sort options. Maps the public name to the GitHub API value and our entity property. */
public enum SortField {
    STARS("stars", "stargazersCount"),
    FORKS("forks", "forksCount"),
    UPDATED("updated", "lastUpdated");

    private final String apiValue;
    private final String entityProperty;

    SortField(String apiValue, String entityProperty) {
        this.apiValue = apiValue;
        this.entityProperty = entityProperty;
    }

    public String getApiValue() {
        return apiValue;
    }

    public String getEntityProperty() {
        return entityProperty;
    }

    /** null or blank -> STARS (default). Anything unknown -> 400. */
    public static SortField from(String value) {
        if (value == null || value.isBlank()) {
            return STARS;
        }
        for (SortField field : values()) {
            if (field.apiValue.equalsIgnoreCase(value.trim())) {
                return field;
            }
        }
        throw new InvalidRequestException("Invalid sort '" + value + "'. Allowed values: stars, forks, updated");
    }
}
