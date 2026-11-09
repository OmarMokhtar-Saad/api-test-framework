package io.github.omarmokhtarsaad.apitest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.omarmokhtarsaad.apitest.framework.auth.AccessToken;
import io.github.omarmokhtarsaad.apitest.framework.auth.CachingTokenProvider;
import io.github.omarmokhtarsaad.apitest.framework.auth.TokenProvider;
import io.github.omarmokhtarsaad.apitest.framework.config.Secret;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CachingTokenProviderTest {

    /** A clock the test moves by hand. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /** Issues token-1, token-2, ... each valid for {@code ttl} from the clock's current time. */
    private static final class CountingProvider implements TokenProvider {
        private final AtomicInteger fetches = new AtomicInteger();
        private final Clock clock;
        private final Duration ttl;

        CountingProvider(Clock clock, Duration ttl) {
            this.clock = clock;
            this.ttl = ttl;
        }

        @Override
        public AccessToken token() {
            return new AccessToken(
                    Secret.of("token-" + fetches.incrementAndGet()),
                    clock.instant().plus(ttl));
        }
    }

    public void reusesTokenUntilSkewBeforeExpiry() {
        MutableClock clock = new MutableClock();
        CountingProvider delegate = new CountingProvider(clock, Duration.ofMinutes(5));
        CachingTokenProvider cache = new CachingTokenProvider(delegate, clock, Duration.ofSeconds(30));

        assertThat(cache.token().value().reveal()).isEqualTo("token-1");
        clock.advance(Duration.ofMinutes(4));
        assertThat(cache.token().value().reveal()).isEqualTo("token-1");
        clock.advance(Duration.ofSeconds(31)); // now inside the 30 s skew window
        assertThat(cache.token().value().reveal()).isEqualTo("token-2");
        assertThat(delegate.fetches).hasValue(2);
    }

    public void invalidateForcesAFreshFetch() {
        MutableClock clock = new MutableClock();
        CountingProvider delegate = new CountingProvider(clock, Duration.ofHours(1));
        CachingTokenProvider cache = new CachingTokenProvider(delegate, clock, Duration.ZERO);

        cache.token();
        cache.invalidate();

        assertThat(cache.token().value().reveal()).isEqualTo("token-2");
    }

    public void concurrentCallersShareOneFetch() throws Exception {
        AtomicInteger fetches = new AtomicInteger();
        TokenProvider slow = () -> {
            fetches.incrementAndGet();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return AccessToken.nonExpiring(Secret.of("shared"));
        };
        CachingTokenProvider cache = new CachingTokenProvider(slow);
        int callers = 16;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < callers; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return cache.token().value().reveal();
                }));
            }
            start.countDown();
            for (Future<String> result : results) {
                assertThat(result.get()).isEqualTo("shared");
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(fetches).hasValue(1);
    }
}
