package com.kycdocs.infrastructure.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitingConfig {

    @Value("${app.rate-limiting.requests-per-minute}")
    private int requestsPerMinute;

    @Bean
    public Bucket globalRateLimitBucket() {
        var refill = Refill.intervally(requestsPerMinute, Duration.ofMinutes(1));
        var limit = Bandwidth.classic(requestsPerMinute, refill);
        return Bucket.builder().addLimit(limit).build();
    }
}
