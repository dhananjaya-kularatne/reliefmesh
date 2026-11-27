package io.reliefmesh.crdt.clock;

/** A physical clock that tests move by hand. */
final class ManualClock implements PhysicalClock {

    private long now;

    ManualClock(long start) {
        this.now = start;
    }

    void set(long millis) {
        this.now = millis;
    }

    void advance(long millis) {
        this.now += millis;
    }

    @Override
    public long currentTimeMillis() {
        return now;
    }
}
