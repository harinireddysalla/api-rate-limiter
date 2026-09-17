package com.cerbo.ratelimiter.model;

public enum ClientTier {

    STANDARD,
    PREMIUM;

    public static ClientTier fromHeader(String value) {

        if (value == null || value.isBlank()) {
            return STANDARD;
        }

        try {
            return valueOf(
                    value.trim().toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
            return STANDARD;
        }
    }
}