package io.reliefmesh.crdt.clock;

/**
 * The source of real (wall-clock) time for a {@link HybridLogicalClock}.
 *
 * <p>Time is behind an interface so tests can control it. Production code uses
 * {@link #system()}; tests and the simulation harness pass a clock they can move forwards,
 * backwards or freeze, which makes clock bugs reproducible.
 */
@FunctionalInterface
public interface PhysicalClock {

    /** Returns the current time in milliseconds since the epoch. */
    long currentTimeMillis();

    /** Returns a clock backed by the operating system's clock. */
    static PhysicalClock system() {
        return System::currentTimeMillis;
    }
}
