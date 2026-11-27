package io.reliefmesh.crdt.clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reliefmesh.crdt.ReplicaId;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class HybridLogicalClockTest {

    private static final ReplicaId LOCAL = ReplicaId.of("local");
    private static final ReplicaId REMOTE = ReplicaId.of("remote");
    private static final Duration MAX_DRIFT = Duration.ofSeconds(60);

    private final ManualClock physical = new ManualClock(1_000_000);
    private final HybridLogicalClock clock = new HybridLogicalClock(LOCAL, physical, MAX_DRIFT);

    @Test
    void followsPhysicalTimeWhenItMovesForward() {
        assertThat(clock.now()).isEqualTo(new HlcTimestamp(1_000_000, 0, LOCAL));

        physical.advance(5);

        assertThat(clock.now()).isEqualTo(new HlcTimestamp(1_000_005, 0, LOCAL));
    }

    @Test
    void countsUpWhenPhysicalTimeStandsStill() {
        HlcTimestamp first = clock.now();
        HlcTimestamp second = clock.now();

        assertThat(second).isEqualTo(new HlcTimestamp(1_000_000, 1, LOCAL));
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void neverGoesBackwardsWhenPhysicalTimeDoes() {
        HlcTimestamp before = clock.now();

        physical.set(500_000);
        HlcTimestamp after = clock.now();

        assertThat(after).isGreaterThan(before);
        assertThat(after.wallTime()).isEqualTo(before.wallTime());
    }

    @Test
    void movesPastARemoteTimestampThatIsAhead() {
        HlcTimestamp remote = new HlcTimestamp(1_030_000, 7, REMOTE);

        HlcTimestamp received = clock.update(remote);

        assertThat(received).isEqualTo(new HlcTimestamp(1_030_000, 8, LOCAL));
        assertThat(clock.now()).isGreaterThan(remote);
    }

    @Test
    void keepsItsOwnTimeWhenARemoteTimestampIsBehind() {
        clock.now();

        HlcTimestamp received = clock.update(new HlcTimestamp(900_000, 3, REMOTE));

        assertThat(received).isEqualTo(new HlcTimestamp(1_000_000, 1, LOCAL));
    }

    @Test
    void usesTheLargerCounterWhenWallTimesAreEqual() {
        clock.now();

        HlcTimestamp received = clock.update(new HlcTimestamp(1_000_000, 5, REMOTE));

        assertThat(received).isEqualTo(new HlcTimestamp(1_000_000, 6, LOCAL));
    }

    @Test
    void resetsTheCounterWhenPhysicalTimeIsAheadOfBoth() {
        clock.update(new HlcTimestamp(1_000_000, 5, REMOTE));

        physical.advance(10);
        HlcTimestamp received = clock.update(new HlcTimestamp(1_000_002, 9, REMOTE));

        assertThat(received).isEqualTo(new HlcTimestamp(1_000_010, 0, LOCAL));
    }

    @Test
    void acceptsARemoteTimestampExactlyAtTheDriftLimit() {
        HlcTimestamp remote = new HlcTimestamp(1_060_000, 0, REMOTE);

        assertThat(clock.update(remote)).isGreaterThan(remote);
    }

    @Test
    void rejectsARemoteTimestampBeyondTheDriftLimitAndStaysUnchanged() {
        HlcTimestamp before = clock.now();

        assertThatThrownBy(() -> clock.update(new HlcTimestamp(1_060_001, 0, REMOTE)))
                .isInstanceOf(ClockDriftException.class)
                .hasMessageContaining("60001 ms ahead");

        assertThat(clock.now()).isEqualTo(new HlcTimestamp(before.wallTime(), 1, LOCAL));
    }

    @Test
    void resumesAfterTheLastIssuedTimestamp() {
        HlcTimestamp lastIssued = new HlcTimestamp(2_000_000, 4, LOCAL);

        HybridLogicalClock resumed =
                HybridLogicalClock.resume(LOCAL, physical, MAX_DRIFT, lastIssued);

        assertThat(resumed.now()).isEqualTo(new HlcTimestamp(2_000_000, 5, LOCAL));
    }

    @Test
    void refusesToResumeFromAnotherReplicasTimestamp() {
        HlcTimestamp otherReplicas = new HlcTimestamp(2_000_000, 4, REMOTE);

        assertThatThrownBy(() -> HybridLogicalClock.resume(LOCAL, physical, MAX_DRIFT, otherReplicas))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeMaxDrift() {
        assertThatThrownBy(() -> new HybridLogicalClock(LOCAL, physical, Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
