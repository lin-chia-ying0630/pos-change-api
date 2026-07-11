package com.alin.lin.enums;

import lombok.Getter;

@Getter
public enum RideChangeField {
    // 主附約保額
    INSURED_AMOUNT(".insured_amount"),

    // 主附約保費
    PREMIUM(".premium");

    private static final String PREFIX = "main_policy_ride.";

    private final String suffix;

    RideChangeField(String suffix) {
        this.suffix = suffix;
    }

    public String fieldName(String rideOrder) {
        return PREFIX + rideOrder + suffix;
    }

    public boolean matches(String fieldName) {
        return fieldName != null && fieldName.startsWith(PREFIX) && fieldName.endsWith(suffix);
    }

    public String resolveRideOrder(String fieldName) {
        return fieldName.substring(PREFIX.length(), fieldName.length() - suffix.length());
    }
}
