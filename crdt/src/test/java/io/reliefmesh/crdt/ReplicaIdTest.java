package io.reliefmesh.crdt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReplicaIdTest {

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> ReplicaId.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> ReplicaId.of("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalWhenValuesAreEqual() {
        assertThat(ReplicaId.of("hub-colombo")).isEqualTo(ReplicaId.of("hub-colombo"));
    }

    @Test
    void ordersByValue() {
        assertThat(ReplicaId.of("a")).isLessThan(ReplicaId.of("b"));
    }
}
