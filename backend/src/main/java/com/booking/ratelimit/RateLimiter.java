package com.booking.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window request counter kept in memory. Good enough for a single backend
 * instance; several instances would each count separately.
 */
public class RateLimiter {

    private record Window(Instant start, int count) {
    }

    /** Result of an attempt; {@code retryAfter} is set only when the request was refused. */
    public record Decision(boolean allowed, Duration retryAfter) {
    }

    private static final int PURGE_THRESHOLD = 10_000;

    private final int maxRequests;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, Duration window, Clock clock) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
    }

    public Decision tryAcquire(String key) {
        Instant now = clock.instant();
        if (windows.size() > PURGE_THRESHOLD) {
            windows.values().removeIf(w -> !w.start().plus(window).isAfter(now));
        }
        Window updated = windows.compute(key, (k, w) -> {
            if (w == null || !w.start().plus(window).isAfter(now)) {
                return new Window(now, 1);
            }
            return new Window(w.start(), w.count() + 1);
        });
        if (updated.count() <= maxRequests) {
            return new Decision(true, null);
        }
        return new Decision(false, Duration.between(now, updated.start().plus(window)));
    }
}
