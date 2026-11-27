package io.reliefmesh.crdt.clock;

import io.reliefmesh.crdt.ReplicaId;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A hybrid logical clock (Kulkarni et al., 2014).
 *
 * <p>It issues timestamps that stay close to real time but, unlike the real clock, have two
 * guarantees:
 *
 * <ul>
 *   <li>Every timestamp it issues is greater than every timestamp it issued before, even if
 *       the physical clock jumps backwards.
 *   <li>After {@link #update receiving} a timestamp from another replica, every timestamp it
 *       issues is greater than the received one. So a change made after seeing another change
 *       always sorts after it, whatever the two machines' clocks say.
 * </ul>
 *
 * <p>Received timestamps more than {@code maxDrift} ahead of the local physical clock are
 * rejected with a {@link ClockDriftException}. Without that limit, one device with its clock
 * set years ahead would push every replica's clock years ahead, and its writes would win every
 * last-writer-wins conflict.
 *
 * <p>This class is thread-safe. It uses a {@link ReentrantLock} rather than
 * {@code synchronized}, because on Java 21 a virtual thread blocked inside
 * {@code synchronized} pins its carrier thread.
 */
public final class HybridLogicalClock {

    private final ReplicaId replica;
    private final PhysicalClock physicalClock;
    private final long maxDriftMillis;
    private final ReentrantLock lock = new ReentrantLock();

    private long wallTime;
    private int logical;

    /** Creates a clock that has issued no timestamps yet. */
    public HybridLogicalClock(ReplicaId replica, PhysicalClock physicalClock, Duration maxDrift) {
        this(replica, physicalClock, maxDrift, 0, 0);
    }

    private HybridLogicalClock(
            ReplicaId replica,
            PhysicalClock physicalClock,
            Duration maxDrift,
            long wallTime,
            int logical) {
        this.replica = Objects.requireNonNull(replica, "replica");
        this.physicalClock = Objects.requireNonNull(physicalClock, "physicalClock");
        Objects.requireNonNull(maxDrift, "maxDrift");
        if (maxDrift.isNegative()) {
            throw new IllegalArgumentException("maxDrift must not be negative: " + maxDrift);
        }
        this.maxDriftMillis = maxDrift.toMillis();
        this.wallTime = wallTime;
        this.logical = logical;
    }

    /**
     * Creates a clock that continues after {@code lastIssued}, the last timestamp this replica
     * issued before it restarted.
     *
     * <p>Without this, a device whose physical clock was reset during a restart would issue
     * timestamps smaller than its own earlier ones, and its newer writes would lose to its
     * older writes.
     */
    public static HybridLogicalClock resume(
            ReplicaId replica, PhysicalClock physicalClock, Duration maxDrift, HlcTimestamp lastIssued) {
        Objects.requireNonNull(lastIssued, "lastIssued");
        if (!lastIssued.replica().equals(replica)) {
            throw new IllegalArgumentException(
                    "lastIssued belongs to " + lastIssued.replica() + ", not " + replica);
        }
        return new HybridLogicalClock(
                replica, physicalClock, maxDrift, lastIssued.wallTime(), lastIssued.logical());
    }

    public ReplicaId replica() {
        return replica;
    }

    /** Issues a timestamp for a local event, such as a write or sending a message. */
    public HlcTimestamp now() {
        lock.lock();
        try {
            long physical = physicalClock.currentTimeMillis();
            if (physical > wallTime) {
                wallTime = physical;
                logical = 0;
            } else {
                logical = increment(logical);
            }
            return current();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Moves this clock past a timestamp received from another replica and issues a timestamp
     * for the receive event.
     *
     * @throws ClockDriftException if {@code remote} is more than {@code maxDrift} ahead of the
     *     local physical clock. The clock is left unchanged.
     */
    public HlcTimestamp update(HlcTimestamp remote) {
        Objects.requireNonNull(remote, "remote");
        lock.lock();
        try {
            long physical = physicalClock.currentTimeMillis();
            if (remote.wallTime() - physical > maxDriftMillis) {
                throw new ClockDriftException(remote.wallTime(), physical, maxDriftMillis);
            }

            long newWallTime = Math.max(Math.max(wallTime, remote.wallTime()), physical);
            int newLogical;
            if (newWallTime == wallTime && newWallTime == remote.wallTime()) {
                newLogical = increment(Math.max(logical, remote.logical()));
            } else if (newWallTime == wallTime) {
                newLogical = increment(logical);
            } else if (newWallTime == remote.wallTime()) {
                newLogical = increment(remote.logical());
            } else {
                newLogical = 0;
            }

            wallTime = newWallTime;
            logical = newLogical;
            return current();
        } finally {
            lock.unlock();
        }
    }

    private HlcTimestamp current() {
        return new HlcTimestamp(wallTime, logical, replica);
    }

    private static int increment(int value) {
        if (value == Integer.MAX_VALUE) {
            throw new IllegalStateException("logical counter overflow");
        }
        return value + 1;
    }
}
