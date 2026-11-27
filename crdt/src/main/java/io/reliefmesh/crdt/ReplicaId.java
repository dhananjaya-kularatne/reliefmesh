package io.reliefmesh.crdt;

import java.util.Objects;

/**
 * Identifies one replica: a hub, or a device when it creates data.
 *
 * <p>Replica ids are used to break ties. When two replicas produce values that are otherwise
 * equal, they are ordered by replica id, so every replica picks the same winner without
 * having to talk to the others.
 */
public record ReplicaId(String value) implements Comparable<ReplicaId> {

    public ReplicaId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("replica id must not be blank");
        }
    }

    public static ReplicaId of(String value) {
        return new ReplicaId(value);
    }

    @Override
    public int compareTo(ReplicaId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
