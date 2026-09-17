package com.cerbo.ratelimiter;

import com.cerbo.ratelimiter.config.RateLimitProperties;
import com.cerbo.ratelimiter.model.ClientTier;
import com.cerbo.ratelimiter.model.EndpointType;
import com.cerbo.ratelimiter.model.RateLimitResult;
import com.cerbo.ratelimiter.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitService service;

    @BeforeEach
    void setUp() {

        RateLimitProperties properties =
                new RateLimitProperties();

        RateLimitProperties.ClientLimit standard =
                new RateLimitProperties.ClientLimit();

        RateLimitProperties.Limit read =
                new RateLimitProperties.Limit();

        read.setCapacity(3);
        read.setRefillPerMinute(3);

        RateLimitProperties.Limit write =
                new RateLimitProperties.Limit();

        write.setCapacity(2);
        write.setRefillPerMinute(2);

        standard.setRead(read);
        standard.setWrite(write);

        properties.getClients()
                .put("standard", standard);

        RateLimitProperties.Cache cache =
                new RateLimitProperties.Cache();

        cache.setMaxClients(100);

        properties.setCache(cache);

        service =
                new RateLimitService(properties);
    }

    @Test
    void shouldAllowRequestsWithinLimit() {

        RateLimitResult first =
                service.checkLimit(
                        "client-1",
                        ClientTier.STANDARD,
                        EndpointType.READ
                );

        assertTrue(first.allowed());
        assertEquals(3, first.limit());
        assertEquals(2, first.remaining());
    }

    @Test
    void shouldRejectRequestsAfterLimit() {

        for (int i = 0; i < 3; i++) {

            service.checkLimit(
                    "client-1",
                    ClientTier.STANDARD,
                    EndpointType.READ
            );
        }

        RateLimitResult result =
                service.checkLimit(
                        "client-1",
                        ClientTier.STANDARD,
                        EndpointType.READ
                );

        assertFalse(result.allowed());
        assertEquals(3, result.limit());
        assertTrue(
                result.retryAfterSeconds() >= 1
        );
    }

    @Test
    void readAndWriteShouldHaveSeparateLimits() {

        service.checkLimit(
                "client-1",
                ClientTier.STANDARD,
                EndpointType.READ
        );

        service.checkLimit(
                "client-1",
                ClientTier.STANDARD,
                EndpointType.WRITE
        );

        RateLimitResult read =
                service.checkLimit(
                        "client-1",
                        ClientTier.STANDARD,
                        EndpointType.READ
                );

        RateLimitResult write =
                service.checkLimit(
                        "client-1",
                        ClientTier.STANDARD,
                        EndpointType.WRITE
                );

        assertEquals(3, read.limit());
        assertEquals(2, write.limit());
    }

    @Test
    void differentClientsShouldHaveIndependentBuckets() {

        for (int i = 0; i < 3; i++) {

            service.checkLimit(
                    "client-1",
                    ClientTier.STANDARD,
                    EndpointType.READ
            );
        }

        RateLimitResult client1 =
                service.checkLimit(
                        "client-1",
                        ClientTier.STANDARD,
                        EndpointType.READ
                );

        RateLimitResult client2 =
                service.checkLimit(
                        "client-2",
                        ClientTier.STANDARD,
                        EndpointType.READ
                );

        assertFalse(client1.allowed());
        assertTrue(client2.allowed());
    }
}