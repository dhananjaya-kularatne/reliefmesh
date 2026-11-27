package io.reliefmesh.crdt.clock;

/**
 * Thrown when a received timestamp is further ahead of the local physical clock than the
 * allowed maximum drift.
 *
 * <p>The clock refuses to adopt such a timestamp, because doing so would drag every later
 * local timestamp into the future. The caller decides what to do with the message that
 * carried it, for example quarantining it for review.
 */
public final class ClockDriftException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long remoteWallTime;
    private final long localPhysicalTime;
    private final long maxDriftMillis;

    public ClockDriftException(long remoteWallTime, long localPhysicalTime, long maxDriftMillis) {
        super("remote timestamp " + remoteWallTime + " is "
                + (remoteWallTime - localPhysicalTime) + " ms ahead of local time "
                + localPhysicalTime + ", more than the allowed " + maxDriftMillis + " ms");
        this.remoteWallTime = remoteWallTime;
        this.localPhysicalTime = localPhysicalTime;
        this.maxDriftMillis = maxDriftMillis;
    }

    public long remoteWallTime() {
        return remoteWallTime;
    }

    public long localPhysicalTime() {
        return localPhysicalTime;
    }

    public long maxDriftMillis() {
        return maxDriftMillis;
    }
}
