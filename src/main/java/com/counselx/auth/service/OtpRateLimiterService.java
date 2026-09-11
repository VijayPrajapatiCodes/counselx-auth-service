package com.counselx.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

/**
 * Redis-backed OTP sending rate limiter.
 *
 * The limiter is intentionally independent from the OTP itself:
 * OTPs remain hashed in MySQL, while Redis stores only short-lived
 * rate-limit/cooldown keys.
 */
@Service
@RequiredArgsConstructor
public class OtpRateLimiterService {

    private static final String COOLDOWN_PREFIX = "counselx:otp:cooldown:";
    private static final String HOURLY_PREFIX = "counselx:otp:hourly:";

    private final StringRedisTemplate redisTemplate;

    @Value("${auth.email-otp.cooldown-seconds:60}")
    private long cooldownSeconds;

    @Value("${auth.email-otp.max-per-hour:5}")
    private long maxPerHour;

    private final DefaultRedisScript<Long> rateLimitScript = new DefaultRedisScript<>(
            """
            local cooldownKey = KEYS[1]
            local hourlyKey = KEYS[2]

            local cooldown = redis.call('EXISTS', cooldownKey)
            if cooldown == 1 then
                return 1
            end

            local current = redis.call('GET', hourlyKey)
            if current and tonumber(current) >= tonumber(ARGV[2]) then
                return 2
            end

            redis.call('SET', cooldownKey, '1', 'EX', ARGV[1], 'NX')

            local count = redis.call('INCR', hourlyKey)
            if count == 1 then
                redis.call('EXPIRE', hourlyKey, 3600)
            end

            return 0
            """,
            Long.class
    );

    public boolean tryAcquire(String email) {

        String normalizedEmail = email.trim().toLowerCase();
        String emailKey = hashEmail(normalizedEmail);

        long hourBucket = System.currentTimeMillis() / 3_600_000L;

        String cooldownKey =
                COOLDOWN_PREFIX + "{" + emailKey + "}";

        String hourlyKey =
                HOURLY_PREFIX + "{" + emailKey + "}:" + hourBucket;

        Long result = redisTemplate.execute(
                rateLimitScript,
                List.of(cooldownKey, hourlyKey),
                String.valueOf(cooldownSeconds),
                String.valueOf(maxPerHour)
        );

        return result != null && result == 0L;
    }

    private String hashEmail(String email) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(email.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
