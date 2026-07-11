package com.alin.lin.enums;

import lombok.Getter;

@Getter
public enum PolicyRideKey {
    // 主約列
    MAIN("000");

    private final String rideOrder;

    PolicyRideKey(String rideOrder) {
        this.rideOrder = rideOrder;
    }
}
