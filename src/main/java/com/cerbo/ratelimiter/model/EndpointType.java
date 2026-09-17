package com.cerbo.ratelimiter.model;

public enum EndpointType {

    READ,
    WRITE;

    public static EndpointType fromMethod(
            String method) {

        return switch (method.toUpperCase()) {
            case "POST", "PUT", "PATCH", "DELETE" ->
                    WRITE;

            default ->
                    READ;
        };
    }
}