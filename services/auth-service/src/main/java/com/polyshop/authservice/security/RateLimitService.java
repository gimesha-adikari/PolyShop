package com.polyshop.authservice.security;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitService {
    private record Bucket(AtomicInteger count, long resetAt){}
    private final ConcurrentHashMap<String, Bucket> ipMap = new ConcurrentHashMap<>();
    public boolean allow(String key, int max, int windowSeconds) {
        long now = Instant.now().getEpochSecond();
        var b = ipMap.compute(key, (k, old) -> {
            if (old == null || old.resetAt() <= now) return new Bucket(new AtomicInteger(1), now + windowSeconds);
            old.count().incrementAndGet();
            return old;
        });
        return b.count().get() <= max;
    }
}
