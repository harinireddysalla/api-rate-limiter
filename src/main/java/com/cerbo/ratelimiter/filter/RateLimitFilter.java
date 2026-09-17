package com.cerbo.ratelimiter.filter;

import com.cerbo.ratelimiter.model.ClientTier;
import com.cerbo.ratelimiter.model.EndpointType;
import com.cerbo.ratelimiter.model.RateLimitResult;
import com.cerbo.ratelimiter.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter
        extends OncePerRequestFilter {

    private static final String CLIENT_ID =
            "X-Client-Id";

    private static final String CLIENT_TIER =
            "X-Client-Tier";

    private final RateLimitService rateLimitService;

    public RateLimitFilter(
            RateLimitService rateLimitService) {

        this.rateLimitService =
                rateLimitService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("RateLimitFilter EXECUTED");

        String clientId =
                request.getHeader(CLIENT_ID);

        if (clientId == null ||
                clientId.isBlank()) {

            clientId = "anonymous";
        }

        ClientTier tier =
                ClientTier.fromHeader(
                        request.getHeader(CLIENT_TIER)
                );

        EndpointType endpointType =
                EndpointType.fromMethod(
                        request.getMethod()
                );

        RateLimitResult result =
                rateLimitService.checkLimit(
                        clientId,
                        tier,
                        endpointType
                );

        System.out.println("Limit = " + result.limit());
        System.out.println("Remaining = " + result.remaining());

        response.setHeader(
                "X-RateLimit-Limit",
                String.valueOf(result.limit())
        );

        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(result.remaining())
        );

        if (!result.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After",
                    String.valueOf(result.retryAfterSeconds()));

            response.setContentType("application/json");
            response.getWriter().write("""
        {
          "error": "Too Many Requests",
          "message": "Rate limit exceeded"
        }
        """);
            return;
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}