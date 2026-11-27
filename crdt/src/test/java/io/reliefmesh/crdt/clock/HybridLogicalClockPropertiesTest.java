package io.reliefmesh.crdt.clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reliefmesh.crdt.Randomized;
import io.reliefmesh.crdt.ReplicaId;
import java.time.Duration;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Checks the clock's guarantees against many random histories: a physical clock that moves
 * forwards, backwards or not at all, mixed with timestamps received from another replica.
 */
class HybridLogicalClockPropertiesTest {

    private static final ReplicaId LOCAL = ReplicaId.of("local");
    private static final ReplicaId REMOTE = ReplicaId.of("remote");
    private static final long MAX_DRIFT_MILLIS = 60_000;
    private static final long START = 1_000_000;
    private static final int MAX_STEPS = 200;

    @Test
    void everyTimestampIsGreaterThanTheOneBefore() {
        Randomized.check(random -> {
            History history = new History(random);
            HlcTimestamp previous = history.step();

            for (int i = 0; i < history.length(); i++) {
                HlcTimestamp next = history.step();
                assertThat(next).isGreaterThan(previous);
                previous = next;
            }
        });
    }

    @Test
    void everyTimestampIsGreaterThanEverythingReceivedBeforeIt() {
        Randomized.check(random -> {
            History history = new History(random);
            HlcTimestamp latestReceived = null;

            for (int i = 0; i < history.length(); i++) {
                history.movePhysicalClock();
                HlcTimestamp issued;
                if (random.nextBoolean()) {
                    issued = history.clock.now();
                } else {
                    HlcTimestamp remote = history.remoteWithinDrift();
                    issued = history.clock.update(remote);
                    if (latestReceived == null || remote.isAfter(latestReceived)) {
                        latestReceived = remote;
                    }
                }
                if (latestReceived != null) {
                    assertThat(issued).isGreaterThan(latestReceived);
                }
            }
        });
    }

    @Test
    void wallTimeNeverRunsFurtherAheadThanTheDriftLimitAllows() {
        Randomized.check(random -> {
            History history = new History(random);

            for (int i = 0; i < history.length(); i++) {
                HlcTimestamp issued = history.step();
                assertThat(issued.wallTime())
                        .isLessThanOrEqualTo(history.latestPhysicalTime + MAX_DRIFT_MILLIS);
            }
        });
    }

    @Test
    void aRejectedTimestampChangesNothing() {
        Randomized.check(random -> {
            ManualClock physical = new ManualClock(START);
            Duration maxDrift = Duration.ofMillis(MAX_DRIFT_MILLIS);
            HybridLogicalClock untouched = new HybridLogicalClock(LOCAL, physical, maxDrift);
            HybridLogicalClock rejecting = new HybridLogicalClock(LOCAL, physical, maxDrift);
            int length = random.nextInt(1, MAX_STEPS + 1);

            for (int i = 0; i < length; i++) {
                physical.set(Math.max(0, physical.currentTimeMillis() + random.nextLong(-1_000, 1_001)));

                if (random.nextInt(4) == 0) {
                    HlcTimestamp tooFarAhead = new HlcTimestamp(
                            physical.currentTimeMillis() + MAX_DRIFT_MILLIS + 1 + random.nextLong(0, 1_000_000),
                            random.nextInt(0, 1_000),
                            REMOTE);
                    assertThatThrownBy(() -> rejecting.update(tooFarAhead))
                            .isInstanceOf(ClockDriftException.class);
                }

                assertThat(rejecting.now()).isEqualTo(untouched.now());
            }
        });
    }

    /** One random history: a physical clock that wanders, and a clock that follows it. */
    private static final class History {

        private final Random random;
        private final ManualClock physical = new ManualClock(START);
        private final HybridLogicalClock clock =
                new HybridLogicalClock(LOCAL, physical, Duration.ofMillis(MAX_DRIFT_MILLIS));
        private final int length;
        private long latestPhysicalTime = START;

        History(Random random) {
            this.random = random;
            this.length = random.nextInt(1, MAX_STEPS + 1);
        }

        int length() {
            return length;
        }

        /** Moves the physical clock up to one second forwards or backwards, or leaves it. */
        void movePhysicalClock() {
            long next = Math.max(0, physical.currentTimeMillis() + random.nextLong(-1_000, 1_001));
            physical.set(next);
            latestPhysicalTime = Math.max(latestPhysicalTime, next);
        }

        /** A remote timestamp between 5 seconds behind and exactly the drift limit ahead. */
        HlcTimestamp remoteWithinDrift() {
            long offset = random.nextLong(-5_000, MAX_DRIFT_MILLIS + 1);
            long wallTime = Math.max(0, physical.currentTimeMillis() + offset);
            return new HlcTimestamp(wallTime, random.nextInt(0, 1_000), REMOTE);
        }

        /** Moves the physical clock, then issues a timestamp for a local or a receive event. */
        HlcTimestamp step() {
            movePhysicalClock();
            return random.nextBoolean() ? clock.now() : clock.update(remoteWithinDrift());
        }
    }
}
