package com.atlasshard.storage.service;

import com.atlasshard.storage.repo.ShareLinkRepository;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceService {
    private final ShareLinkRepository shares;

    public MaintenanceService(ShareLinkRepository shares) {
        this.shares = shares;
    }

    @Scheduled(cron = "0 45 3 * * *")
    @Transactional
    public void deleteExpiredShares() {
        shares.deleteByExpiresAtBefore(Instant.now());
    }
}
