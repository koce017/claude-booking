package com.booking.ratelimit;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.booking.common.error.GlobalExceptionHandler;
import com.booking.config.BookingProperties;

/**
 * Limits how often one client IP may request admin login links, redeem them, and
 * submit bookings. Excess requests get 429 with a Retry-After header.
 */
@Component
@ConditionalOnProperty(name = "booking.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private record LimitedEndpoint(Pattern path, RateLimiter limiter) {
    }

    private final List<LimitedEndpoint> endpoints;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(BookingProperties properties, Clock clock, ObjectMapper objectMapper) {
        BookingProperties.RateLimit limits = properties.rateLimit();
        List<LimitedEndpoint> list = new ArrayList<>();
        addEndpoint(list, "^/api/admin/auth/request-link$", limits.loginLink(), clock);
        addEndpoint(list, "^/api/admin/auth/verify$", limits.loginVerify(), clock);
        addEndpoint(list, "^/api/companies/[^/]+/appointments$", limits.booking(), clock);
        this.endpoints = List.copyOf(list);
        this.objectMapper = objectMapper;
    }

    private static void addEndpoint(List<LimitedEndpoint> list, String regex, BookingProperties.Rule rule, Clock clock) {
        if (rule != null) {
            list.add(new LimitedEndpoint(Pattern.compile(regex), new RateLimiter(rule.requests(), rule.window(), clock)));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        for (LimitedEndpoint endpoint : endpoints) {
            if (endpoint.path().matcher(path).matches()) {
                RateLimiter.Decision decision = endpoint.limiter().tryAcquire(request.getRemoteAddr());
                if (!decision.allowed()) {
                    reject(response, decision);
                    return;
                }
                break;
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, RateLimiter.Decision decision) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(Math.max(1, decision.retryAfter().toSeconds())));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), GlobalExceptionHandler.problem(HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMITED", "Too many requests. Please try again later."));
    }
}
