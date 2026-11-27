package io.reliefmesh.crdt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RandomizedTest {

    @Test
    void runsTheRequestedNumberOfCases() {
        assumeTrue(System.getProperty(Randomized.SEED_PROPERTY) == null, "replay seed is set");
        AtomicInteger runs = new AtomicInteger();

        Randomized.check(25, random -> runs.incrementAndGet());

        assertThat(runs).hasValue(25);
    }

    @Test
    void theSameSeedGivesTheSameInputs() {
        List<Long> first = new ArrayList<>();
        List<Long> second = new ArrayList<>();

        Randomized.runCase(42, random -> first.add(random.nextLong()));
        Randomized.runCase(42, random -> second.add(random.nextLong()));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void reportsTheSeedOfAFailedCheck() {
        assertThatThrownBy(() -> Randomized.runCase(42, random -> {
                    throw new AssertionError("boom");
                }))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Failed with seed 42")
                .hasMessageContaining("-Dreliefmesh.seed=42")
                .cause()
                .hasMessage("boom");
    }

    @Test
    void reportsTheSeedOfAnUnexpectedException() {
        assertThatThrownBy(() -> Randomized.runCase(7, random -> {
                    throw new IllegalStateException("broken");
                }))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Failed with seed 7")
                .cause()
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsFewerThanOneCase() {
        assertThatThrownBy(() -> Randomized.check(0, (Random random) -> {}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
