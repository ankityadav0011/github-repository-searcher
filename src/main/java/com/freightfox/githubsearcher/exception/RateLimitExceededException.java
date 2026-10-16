package com.freightfox.githubsearcher.exception;

/** GitHub rate limit hit -> HTTP 429. */
public class RateLimitExceededException extends RuntimeException {

    private final Long retryAfterSeconds;

    public RateLimitExceededException(String message, Long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
