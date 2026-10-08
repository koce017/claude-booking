package com.booking.support;


import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(IntegrationTest.DEFAULT_NOW);
    }

    @Bean
    public CapturingMagicLinkSender capturingMagicLinkSender() {
        return new CapturingMagicLinkSender();
    }
}
