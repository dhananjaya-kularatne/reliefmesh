package io.reliefmesh.crdt.clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reliefmesh.crdt.ReplicaId;
import org.junit.jupiter.api.Test;

class HlcTimestampTest {

    private static final ReplicaId A = ReplicaId.of("a");
    private static final ReplicaId B = ReplicaId.of("b");

    @Test
    void wallTimeDecidesFirst() {
        assertThat(new HlcTimestamp(100, 9, B)).isLessThan(new HlcTimestamp(101, 0, A));
    }

    @Test
    void logicalDecidesWhenWallTimesAreEqual() {
        assertThat(new HlcTimestamp(100, 1, B)).isLessThan(new HlcTimestamp(100, 2, A));
    }

    @Test
    void replicaBreaksExactTies() {
        assertThat(new HlcTimestamp(100, 1, A)).isLessThan(new HlcTimestamp(100, 1, B));
    }

    @Test
    void isAfterAndIsBeforeAgreeWithCompareTo() {
        HlcTimestamp earlier = new HlcTimestamp(100, 0, A);
        HlcTimestamp later = new HlcTimestamp(100, 1, A);

        assertThat(later.isAfter(earlier)).isTrue();
        assertThat(earlier.isBefore(later)).isTrue();
        assertThat(earlier.isAfter(earlier)).isFalse();
    }

    @Test
    void rejectsNegativeWallTime() {
        assertThatThrownBy(() -> new HlcTimestamp(-1, 0, A))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeLogical() {
        assertThatThrownBy(() -> new HlcTimestamp(0, -1, A))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullReplica() {
        assertThatThrownBy(() -> new HlcTimestamp(0, 0, null))
                .isInstanceOf(NullPointerException.class);
    }
}
