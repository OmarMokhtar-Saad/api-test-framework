package io.github.omarmokhtarsaad.apitest.framework.auth;

/** Supplies bearer tokens. Implementations must be thread-safe. */
@FunctionalInterface
public interface TokenProvider {

    /** A token valid for at least the next request. May block while a new one is fetched. */
    AccessToken token();

    /** Discards any cached token so the next call fetches a fresh one. No-op by default. */
    default void invalidate() {}
}
