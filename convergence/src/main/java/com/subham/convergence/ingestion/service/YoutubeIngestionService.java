package com.subham.convergence.ingestion.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.subham.convergence.config.PlaywrightManager;
import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.ingestion.common.IngestionState;
import com.subham.convergence.ingestion.dto.IngestRequest;
import com.subham.convergence.ingestion.repository.IngestionStateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeIngestionService {

    private final IngestionService ingestionService;
    private final PlaywrightManager playwrightManager;
    private final IngestionStateRepository ingestionStateRepository; // ← injected (not static)
    private final ObjectMapper objectMapper;                          // ← injected (not new)

    public void login() {
        log.info("[YouTube] Logging in...");
        try {
            Page page = playwrightManager.newPage();
            page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
            page.navigate("https://accounts.google.com/signin");

            log.info("[YouTube] Log in manually then press ENTER in console...");
            System.in.read();

            page.navigate("https://www.youtube.com");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            if (page.title().contains("YouTube")) {
                log.info("[YouTube] ✓ Login successful");
            } else {
                log.warn("[YouTube] Login may have failed — title: {}", page.title());
            }

            page.close();
        } catch (Exception e) {
            log.error("[YouTube] Login failed: {}", e.getMessage());
        }
    }

    public void scrape() {
        log.info("[YouTube] Scraping starts...");
        try {
            Page page = playwrightManager.newPage();
            page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
            page.navigate("https://www.youtube.com/playlist?list=WL");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            if (page.url().contains("accounts.google.com")) {
                log.warn("[YouTube] Session expired — re-login required");
                page.close();
                return;
            }

            // 1. get checkpoint
            IngestionState state = ingestionStateRepository.findByPlatform(PlatformType.YOUTUBE)
                    .orElse(new IngestionState(PlatformType.YOUTUBE, null, 0L));
            String lastProcessedId = state.getLastProcessedId(); // ← String, not long
            log.info("[YouTube] Last processed ID: {}", lastProcessedId);

            // 2. scroll until lastProcessedId is visible (or end of list)
            scrollToBottom(page, lastProcessedId);

            // 3. scrape only new videos
            List<IngestRequest> newVideos = scrapeVideos(page, lastProcessedId);
            log.info("[YouTube] {} new videos to ingest", newVideos.size());

            if (newVideos.isEmpty()) {
                log.info("[YouTube] No new videos — already up to date");
                page.close();
                return;
            }

            // 4. save to JSON file
            saveBookmarksToFile(newVideos, "src/main/java/com/subham/convergence/ingestion/jsons/youtube_videos.json");

            // 5. ingest to DB
            int saved = ingestionService.ingestAll(newVideos);
            log.info("[YouTube] ✓ Saved {} new videos to DB", saved);

            // 6. update checkpoint — newest video is first in list
            state.setLastProcessedId(newVideos.get(0).getExternalId());
            ingestionStateRepository.save(state);

            page.close();
        } catch (Exception e) {
            log.error("[YouTube] Scrape failed: {}", e.getMessage());
        }
    }

    private void scrollToBottom(Page page, String lastProcessedId) {
        int previousHeight = 0;
        while (true) {
            // stop if last processed video is already visible
            if (lastProcessedId != null) {
                Locator knownVideo = page.locator("ytd-playlist-video-renderer #video-title[href*='v=" + lastProcessedId + "']");
                if (knownVideo.count() > 0) {
                    log.info("[YouTube] Found last processed video — stopping scroll");
                    break;
                }
            }

            page.evaluate("window.scrollTo(0, document.documentElement.scrollHeight)");
            page.waitForTimeout(2000);

            int currentHeight = (int) (long) page.evaluate("document.documentElement.scrollHeight");
            if (currentHeight == previousHeight) break; // end of list
            previousHeight = currentHeight;
        }
        log.info("[YouTube] Finished scrolling");
    }

    private List<IngestRequest> scrapeVideos(Page page, String lastProcessedId) {
        List<IngestRequest> items = new ArrayList<>();

        Locator videoElements = page.locator("ytd-playlist-video-renderer");
        int count = videoElements.count();
        log.info("[YouTube] Found {} total video elements", count);

        for (int i = 0; i < count; i++) {
            try {
                Locator el = videoElements.nth(i);

                Locator titleEl = el.locator("#video-title");
                if (!titleEl.isVisible()) continue;

                String title = titleEl.textContent().trim();
                String href  = titleEl.getAttribute("href");
                if (href == null || href.isBlank()) continue;

                String url     = "https://www.youtube.com" + href.split("&")[0];
                String videoId = extractVideoId(url);

                // stop collecting when we hit the last known video
                if (videoId != null && videoId.equals(lastProcessedId)) {
                    log.info("[YouTube] Hit lastProcessedId at index {} — stopping", i);
                    break;
                }

                Locator channelEl  = el.locator("#channel-name a");
                String channel     = channelEl.isVisible() ? channelEl.textContent().trim() : "";

                Locator durationEl = el.locator("span.ytd-thumbnail-overlay-time-status-renderer");
                String duration    = durationEl.isVisible() ? durationEl.textContent().trim() : "";

                Locator thumbEl    = el.locator("img#img");
                String thumbnail   = thumbEl.isVisible() ? thumbEl.getAttribute("src") : null;

                items.add(IngestRequest.builder()
                        .url(url)
                        .title(title)
                        .externalId(videoId)
                        .thumbnailUrl(thumbnail)
                        .platform(PlatformType.YOUTUBE)
                        .platformMetadata(Map.of(
                                "channel",        channel,
                                "duration",       duration,
                                "playlistSource", "WatchLater"
                        ))
                        .build());

            } catch (Exception e) {
                log.warn("[YouTube] Skipped video element {}: {}", i, e.getMessage());
            }
        }

        return items;
    }

    private String extractVideoId(String url) {
        try {
            if (url.contains("v=")) {
                return url.split("v=")[1].split("&")[0];
            }
        } catch (Exception e) {
            log.warn("[YouTube] Failed to extract video ID from: {}", url);
        }
        return null;
    }

    private void saveBookmarksToFile(List<IngestRequest> videos, String filename) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), videos);
            log.info("[YouTube] ✓ Saved {} videos to {}", videos.size(), filename);
        } catch (Exception e) {
            log.error("[YouTube] Failed to save to file: {}", e.getMessage());
        }
    }
}

