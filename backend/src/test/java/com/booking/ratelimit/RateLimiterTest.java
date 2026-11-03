package com.booking.ratelimit;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.booking.support.IntegrationTest;
import com.booking.support.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private final MutableClock clock = new MutableClock(IntegrationTest.DEFAULT_NOW);
    private final RateLimiter limiter = new RateLimiter(2, Duration.ofMinutes(10), clock);

    @Test
    void allowsUpToTheLimitPerKeyAndWindow() {
        assertThat(limiter.tryAcquire("1.1.1.1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1").allowed()).isTrue();

        RateLimiter.Decision third = limiter.tryAcquire("1.1.1.1");
        assertThat(third.allowed()).isFalse();
        assertThat(third.retryAfter()).isEqualTo(Duration.ofMinutes(10));

        assertThat(limiter.tryAcquire("2.2.2.2").allowed()).isTrue();

        clock.advance(Duration.ofMinutes(4));
        assertThat(limiter.tryAcquire("1.1.1.1").retryAfter()).isEqualTo(Duration.ofMinutes(6));

        clock.advance(Duration.ofMinutes(6));
        assertThat(limiter.tryAcquire("1.1.1.1").allowed()).isTrue();
    }
}
