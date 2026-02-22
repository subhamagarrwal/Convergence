package com.subham.convergence.ingestion.service;

import com.subham.convergence.ingestion.common.YouTubeSession;
import com.subham.convergence.ingestion.repository.YouTubeSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnifiedSyncScheduler {

    private final YouTubeScraperService  ytScraper;
    private final YouTubeSessionRepository ytSessionRepo;

    /**
     * Runs every 2 hours.
     *
     * Extension handles real-time sync for all 3 platforms.
     * This scheduler is the FALLBACK when extension is offline:
     *   - X:       dead man's switch (has its own check inside)
     *   - YouTube: re-scrape using stored Playwright cookies
     *   - Chrome:  no fallback needed (only changes when browser is open)
     */
    @Scheduled(fixedRate = 7_200_000, initialDelay = 120_000) // 2 hours, 2 min initial delay
    public void runFallbackSyncs() {
        log.info("[Scheduler] ═══ Running fallback sync checks ═══");

        // 1) X Dead Man's Switch (checks heartbeat internally)
        // try {
        //     xDeadManSwitch.runFallbackCheck();
        // } catch (Exception e) {
        //     log.error("[Scheduler] X fallback failed: {}", e.getMessage());
        // }

        // 2) YouTube — re-scrape for users with valid sessions
        try {
            runYouTubeFallback();
        } catch (Exception e) {
            log.error("[Scheduler] YouTube fallback failed: {}", e.getMessage());
        }

        log.info("[Scheduler] ═══ Fallback sync checks complete ═══");
    }

    private void runYouTubeFallback() {
        List<YouTubeSession> sessions = ytSessionRepo.findAll();

        for (YouTubeSession session : sessions) {
            if (!Boolean.TRUE.equals(session.getIsValid())) continue;

            if (session.getLastSyncAt() != null) {
                long ageMins = java.time.Duration.between(
                        session.getLastSyncAt(),
                        java.time.LocalDateTime.now()
                ).toMinutes();

                if (ageMins < 120) {
                    log.debug("[Scheduler] YouTube user {} synced {}min ago — skipping",
                            session.getUserId(), ageMins);
                    continue;
                }
            }

            log.info("[Scheduler] YouTube fallback scrape for user {}", session.getUserId());
            try {
                ytScraper.scrapeWatchLater(session.getUserId());
            } catch (Exception e) {
                log.warn("[Scheduler] YouTube scrape failed for {}: {}",
                        session.getUserId(), e.getMessage());
            }
        }
    }
}