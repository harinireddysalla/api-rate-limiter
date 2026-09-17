package com.cerbo.ratelimiter.service;

import com.cerbo.ratelimiter.config.RateLimitProperties;
import com.cerbo.ratelimiter.model.ClientTier;
import com.cerbo.ratelimiter.model.EndpointType;
import com.cerbo.ratelimiter.model.RateLimitResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RateLimitService {

    private final RateLimitProperties properties;

    private final ConcurrentMap<String, TokenBucket> buckets =
            new ConcurrentHashMap<>();

    public RateLimitService(
            RateLimitProperties properties) {

        this.properties = properties;
    }

    public RateLimitResult checkLimit(
            String clientId,
            ClientTier clientTier,
            EndpointType endpointType) {

        RateLimitProperties.ClientLimit clientLimit =
                properties.getClients()
                        .get(clientTier.name().toLowerCase());

        if (clientLimit == null) {
            throw new IllegalArgumentException(
                    "No configuration for tier: "
                            + clientTier
            );
        }

        RateLimitProperties.Limit limit =
                endpointType == EndpointType.WRITE
                        ? clientLimit.getWrite()
                        : clientLimit.getRead();

        String bucketKey =
                clientId
                        + ":"
                        + clientTier
                        + ":"
                        + endpointType;

        TokenBucket bucket =
                buckets.computeIfAbsent(
                        bucketKey,
                        key -> createBucket(
                                limit
                        )
                );

        if (!bucket.tryConsume()) {

            return RateLimitResult.rejected(
                    limit.getCapacity(),
                    bucket.secondsUntilNextToken()
            );
        }

        return RateLimitResult.allowed(
                limit.getCapacity(),
                bucket.remainingTokens()
        );
    }

    private TokenBucket createBucket(
            RateLimitProperties.Limit limit) {

        enforceCacheLimit();

        return new TokenBucket(
                limit.getCapacity(),
                limit.getRefillPerMinute()
        );
    }

    private void enforceCacheLimit() {

        int maxClients =
                properties.getCache()
                        .getMaxClients();

        if (buckets.size() >= maxClients) {

            String firstKey =
                    buckets.keySet()
                            .stream()
                            .findFirst()
                            .orElse(null);

            if (firstKey != null) {
                buckets.remove(firstKey);
            }
        }
    }
}