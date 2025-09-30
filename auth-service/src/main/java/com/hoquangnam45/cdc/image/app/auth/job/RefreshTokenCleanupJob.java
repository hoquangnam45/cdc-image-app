package com.hoquangnam45.cdc.image.app.auth.job;

import com.hoquangnam45.cdc.image.app.auth.repository.AuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RefreshTokenCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final AuthRepository authRepository;

    public RefreshTokenCleanupJob(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Scheduled(cron = "0 */15 * * * ?")
    public void cleanupStaleRefreshTokens() {
        logger.info("Starting stale refresh token cleanup job.");
        int deletedCount = authRepository.deleteExpiredRefreshTokens(Instant.now());
        logger.info("Completed stale refresh token cleanup job. Deleted {} tokens.", deletedCount);
    }
}