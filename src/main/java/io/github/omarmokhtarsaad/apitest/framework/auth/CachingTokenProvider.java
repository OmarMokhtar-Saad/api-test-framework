package io.github.omarmokhtarsaad.apitest.framework.auth;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * Caches a delegate's token until shortly before it expires. Thread-safe: when many parallel tests
 * need a token at once, exactly one fetch happens and the rest wait for it.
 */
public final class CachingTokenProvider implements TokenProvider {

    /** Refresh this long before expiry so a token never expires mid-request. */
    public static final Duration DEFAULT_SKEW = Duration.ofSeconds(30);

    private final TokenProvider delegate;
    private final Clock clock;
    private final Duration skew;
    private AccessToken cached;

    public CachingTokenProvider(TokenProvider delegate, Clock clock, Duration skew) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.skew = Objects.requireNonNull(skew, "skew");
    }

    public CachingTokenProvider(TokenProvider delegate) {
        this(delegate, Clock.systemUTC(), DEFAULT_SKEW);
    }

    @Override
    public synchronized AccessToken token() {
        if (cached == null || !cached.isUsableAt(clock.instant(), skew)) {
            cached = delegate.token();
        }
        return cached;
    }

    @Override
    public synchronized void invalidate() {
        cached = null;
        delegate.invalidate();
    }
}
