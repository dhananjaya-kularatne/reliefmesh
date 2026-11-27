package io.reliefmesh.crdt.clock;

import io.reliefmesh.crdt.ReplicaId;
import java.util.Comparator;
import java.util.Objects;

/**
 * A timestamp issued by a {@link HybridLogicalClock}.
 *
 * <p>It has three parts, compared in this order:
 *
 * <ol>
 *   <li>{@code wallTime}: milliseconds since the epoch, close to real time.
 *   <li>{@code logical}: a counter that orders events with the same {@code wallTime}.
 *   <li>{@code replica}: the replica that issued it, used only to break exact ties.
 * </ol>
 *
 * <p>Including the replica makes the order total: two different replicas can never issue
 * equal timestamps, so "the latest write wins" always has exactly one answer.
 */
public record HlcTimestamp(long wallTime, int logical, ReplicaId replica)
        implements Comparable<HlcTimestamp> {

    private static final Comparator<HlcTimestamp> ORDER =
            Comparator.comparingLong(HlcTimestamp::wallTime)
                    .thenComparingInt(HlcTimestamp::logical)
                    .thenComparing(HlcTimestamp::replica);

    public HlcTimestamp {
        Objects.requireNonNull(replica, "replica");
        if (wallTime < 0) {
            throw new IllegalArgumentException("wallTime must not be negative: " + wallTime);
        }
        if (logical < 0) {
            throw new IllegalArgumentException("logical must not be negative: " + logical);
        }
    }

    @Override
    public int compareTo(HlcTimestamp other) {
        return ORDER.compare(this, other);
    }

    public boolean isAfter(HlcTimestamp other) {
        return compareTo(other) > 0;
    }

    public boolean isBefore(HlcTimestamp other) {
        return compareTo(other) < 0;
    }
}
