package com.polyshop.authservice.security;

import com.polyshop.authservice.domain.Ban;
import com.polyshop.authservice.repository.BanRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class BruteForceService {
    private final BanRepository repo;
    public BruteForceService(BanRepository repo) { this.repo = repo; }

    public boolean isBanned(String key) {
        var o = repo.findByKey(key);
        if (o.isEmpty()) return false;
        Ban b = o.get();
        if (b.getUntil().isBefore(Instant.now())) {
            repo.delete(b);
            return false;
        }
        return true;
    }

    public void ban(String key, long seconds, String reason) {
        Ban b = repo.findByKey(key).orElse(new Ban());
        b.setKey(key);
        b.setUntil(Instant.now().plusSeconds(seconds));
        b.setReason(reason);
        repo.save(b);
    }

    public void unban(String key) { repo.deleteByKey(key); }
}
