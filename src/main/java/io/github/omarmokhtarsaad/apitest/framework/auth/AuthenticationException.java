package io.github.omarmokhtarsaad.apitest.framework.auth;

/** Credentials could not be obtained, e.g. the token endpoint rejected the client. */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
