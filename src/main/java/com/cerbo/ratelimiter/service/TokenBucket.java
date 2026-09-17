package com.cerbo.ratelimiter.service;

public class TokenBucket {

    private final long capacity;

    private final double refillTokensPerNano;

    private double tokens;

    private long lastRefillTime;

    public TokenBucket(
            long capacity,
            long refillPerMinute) {

        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be greater than zero"
            );
        }

        if (refillPerMinute <= 0) {
            throw new IllegalArgumentException(
                    "Refill rate must be greater than zero"
            );
        }

        this.capacity = capacity;

        this.tokens = capacity;

        this.refillTokensPerNano =
                (double) refillPerMinute
                        / (60.0 * 1_000_000_000.0);

        this.lastRefillTime =
                System.nanoTime();
    }

    public synchronized boolean tryConsume() {

        refill();

        if (tokens < 1.0) {
            return false;
        }

        tokens -= 1.0;

        return true;
    }

    public synchronized long remainingTokens() {

        refill();

        return Math.max(
                0,
                (long) Math.floor(tokens)
        );
    }

    public synchronized long secondsUntilNextToken() {

        refill();

        if (tokens >= 1.0) {
            return 0;
        }

        double tokensNeeded =
                1.0 - tokens;

        double nanoseconds =
                tokensNeeded / refillTokensPerNano;

        return Math.max(
                1,
                (long) Math.ceil(
                        nanoseconds
                                / 1_000_000_000.0
                )
        );
    }

    private void refill() {

        long now = System.nanoTime();

        long elapsed =
                now - lastRefillTime;

        if (elapsed <= 0) {
            return;
        }

        double newTokens =
                elapsed * refillTokensPerNano;

        tokens = Math.min(
                capacity,
                tokens + newTokens
        );

        lastRefillTime = now;
    }
}