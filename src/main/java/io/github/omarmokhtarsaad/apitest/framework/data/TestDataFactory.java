package io.github.omarmokhtarsaad.apitest.framework.data;

import io.github.omarmokhtarsaad.apitest.framework.config.ConfigProvider;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import net.datafaker.Faker;

/**
 * Test data that is reproducible and collision-free under parallel runs.
 *
 * <ul>
 *   <li>{@link #faker()} is seeded from {@code data.seed}, one instance per thread ({@code Faker} is
 *       not thread-safe). Re-run with the seed printed at start-up to get the same values.
 *   <li>{@link #uniqueEmail()} and {@link #uniqueSuffix()} combine a per-JVM {@link #runId()} with a
 *       counter, so two runs against a shared environment never collide.
 * </ul>
 */
public final class TestDataFactory {

    private static final String RUN_ID = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
            + Integer.toString(ThreadLocalRandom.current().nextInt(36 * 36), 36);

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private static final ThreadLocal<TestDataFactory> CURRENT = ThreadLocal.withInitial(
            () -> new TestDataFactory(ConfigProvider.get().dataSeed()));

    private final Faker faker;

    public TestDataFactory(long seed) {
        this.faker = new Faker(new Random(seed));
    }

    /** The factory for the current thread. */
    public static TestDataFactory current() {
        return CURRENT.get();
    }

    /** Identifies this JVM's run; use it to tag and later clean up created data. */
    public static String runId() {
        return RUN_ID;
    }

    /** {@code <runId>-<n>}, unique within and across runs. */
    public static String uniqueSuffix() {
        return RUN_ID + "-" + SEQUENCE.incrementAndGet();
    }

    /** An address on the reserved {@code .test} TLD, so no real mailbox is ever used. */
    public static String uniqueEmail() {
        return "user." + RUN_ID + "." + SEQUENCE.incrementAndGet() + "@example.test";
    }

    public Faker faker() {
        return faker;
    }
}
