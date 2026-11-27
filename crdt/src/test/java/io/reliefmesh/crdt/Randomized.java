package io.reliefmesh.crdt;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Runs a randomized check many times, each time with fresh random inputs.
 *
 * <p>Every case gets its own seed. The same seed always produces the same random inputs, so a
 * failure can be reproduced exactly: the error names the seed of the failing case, and running
 * the build with {@code -Dreliefmesh.seed=<seed>} replays only that case.
 */
public final class Randomized {

    public static final String SEED_PROPERTY = "reliefmesh.seed";
    public static final int DEFAULT_CASES = 1_000;

    private Randomized() {}

    /** Runs {@code property} {@value #DEFAULT_CASES} times. */
    public static void check(Consumer<Random> property) {
        check(DEFAULT_CASES, property);
    }

    /** Runs {@code property} {@code cases} times, or once if a replay seed is set. */
    public static void check(int cases, Consumer<Random> property) {
        if (cases < 1) {
            throw new IllegalArgumentException("cases must be at least 1: " + cases);
        }
        Long replaySeed = replaySeed();
        if (replaySeed != null) {
            runCase(replaySeed, property);
            return;
        }
        Random seeds = new Random();
        for (int i = 0; i < cases; i++) {
            runCase(seeds.nextLong(), property);
        }
    }

    static void runCase(long seed, Consumer<Random> property) {
        try {
            property.accept(new Random(seed));
        } catch (AssertionError | RuntimeException failure) {
            throw new AssertionError(
                    "Failed with seed " + seed + ". Replay this case with -D" + SEED_PROPERTY + "=" + seed,
                    failure);
        }
    }

    private static Long replaySeed() {
        String value = System.getProperty(SEED_PROPERTY);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    SEED_PROPERTY + " must be a whole number, got: " + value, e);
        }
    }
}
