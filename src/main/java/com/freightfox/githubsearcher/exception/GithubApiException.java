package com.freightfox.githubsearcher.exception;

/** GitHub is down, unreachable or sent something unusable -> HTTP 502. */
public class GithubApiException extends RuntimeException {
    public GithubApiException(String message) {
        super(message);
    }

    public GithubApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
