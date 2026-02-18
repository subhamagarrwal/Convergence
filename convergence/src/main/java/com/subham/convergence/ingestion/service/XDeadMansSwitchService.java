package com.subham.convergence.ingestion.service;

import com.subham.convergence.ingestion.common.XCredential;
import com.subham.convergence.ingestion.repository.XCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class XDeadMansSwitchService {

    private final XCredentialRepository credentialRepository;

    private static final long DEAD_THRESHOLD_MS = 10 * 60 * 1000; // 10 minutes

    /**
     * Called by scheduler. If extension hasn't sent heartbeat in threshold time,
     * trigger fallback sync using stored credentials.
     */
    public void runFallbackCheck() {
        List<XCredential> allCreds = credentialRepository.findAll();
        long now = System.currentTimeMillis();

        for (XCredential cred : allCreds) {
            if (cred.getLastActiveTimestamp() == null) continue;

            long elapsed = now - cred.getLastActiveTimestamp();
            if (elapsed > DEAD_THRESHOLD_MS) {
                log.warn("[X DMS] Extension offline for user {} ({}ms). Fallback needed.",
                        cred.getUserId(), elapsed);
                // TODO: implement fallback X API scrape with stored credentials
            } else {
                log.debug("[X DMS] Extension alive for user {} ({}ms ago)",
                        cred.getUserId(), elapsed);
            }
        }
    }
}