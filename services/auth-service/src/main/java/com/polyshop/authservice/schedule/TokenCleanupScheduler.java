package com.polyshop.authservice.schedule;

import com.polyshop.authservice.repository.AuthTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import jakarta.transaction.Transactional;

@Component
public class TokenCleanupScheduler {
    private final AuthTokenRepository repo;
    private final long retentionSeconds;
    public TokenCleanupScheduler(AuthTokenRepository repo, @Value("${auth.token.cleanup.retention-seconds:604800}") long retentionSeconds) {
        this.repo = repo;
        this.retentionSeconds = retentionSeconds;
    }
    @Scheduled(cron = "${auth.token.cleanup.cron:0 0 * * * *}")
    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(retentionSeconds);
        repo.deleteExpiredOrRevokedBefore(now, cutoff);
    }
}
