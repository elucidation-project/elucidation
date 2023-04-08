package org.kiwiproject.elucidation.common.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DirectionTest {

    @Test
    void testOpposite() {
        assertThat(Direction.OUTBOUND.opposite()).isEqualTo(Direction.INBOUND);
        assertThat(Direction.INBOUND.opposite()).isEqualTo(Direction.OUTBOUND);
    }

}
