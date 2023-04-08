package org.kiwiproject.elucidation.common.model;

public enum Direction {
    INBOUND, OUTBOUND;

    public Direction opposite() {
        if (this == INBOUND) {
            return OUTBOUND;
        }

        return INBOUND;
    }

}
