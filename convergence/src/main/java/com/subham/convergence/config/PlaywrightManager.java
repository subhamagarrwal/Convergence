package com.subham.convergence.config;

import com.microsoft.playwright.*;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;

@Component
public class PlaywrightManager {

    private Playwright playwright;
    private BrowserContext browserContext;

    public PlaywrightManager() {
        initPlaywright();
    }

    private void initPlaywright() {
        try {
            playwright = Playwright.create();
            // persistent context for session management
            browserContext = playwright.chromium().launchPersistentContext(
                    Paths.get("pw-data"),
                    new BrowserType.LaunchPersistentContextOptions()
                            .setHeadless(false)
                            .setArgs(List.of(
                                    "--no-sandbox",
                                    "--disable-blink-features=AutomationControlled",
                                    "--start-maximized",
                                    "--disable-dev-shm-usage"
                            ))
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            );
            log.info("[PlaywrightManager] Browser context created at pw-data");
        } catch (Exception e) {
            log.error("[PlaywrightManager] Failed to initialize Playwright: {}", e.getMessage());
            throw new RuntimeException("Playwright initialization failed", e);
        }
    }

    public Playwright getPlaywright() {
        if (playwright == null) {
            throw new RuntimeException("Playwright not initialized");
        }
        return playwright;
    }

    public BrowserContext getBrowserContext() {
        if (browserContext == null) {
            throw new RuntimeException("Browser context not initialized");
        }
        return browserContext;
    }

    public Page newPage() {
        return browserContext.newPage();
    }

    public void shutdown() {
        try {
            if (browserContext != null) {
                browserContext.close();
                log.info("[PlaywrightManager] Browser context closed");
            }
            if (playwright != null) {
                playwright.close();
                log.info("[PlaywrightManager] Playwright closed");
            }
        } catch (Exception e) {
            log.error("[PlaywrightManager] Error during shutdown: {}", e.getMessage());
        }
    }
}
