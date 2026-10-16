package com.freightfox.githubsearcher.exception;

/** Client sent something invalid -> HTTP 400. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
