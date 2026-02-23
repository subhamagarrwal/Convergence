package com.subham.convergence.ingestion.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.ingestion.dto.IngestRequest;
import com.subham.convergence.config.PlaywrightManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java,

@Service
@RequiredArgsConstructor
@Slf4j
public class YoutubeIngestionService {

    private final IngestionService ingestionService;
    private final PlaywrightManager playwrightManager;

    public void login() {
        log.info("[Youtube] logging into Youtube");

        try {
            Page page = playwrightManager.newPage();
            page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
            page.navigate("https://accounts.google.com/signin");

            log.info("[Chrome login] login manually then press any key in console");
            System.in.read();

            page.navigate("https://www.youtube.com");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            if(page.title().contains("YouTube")) {
                log.info("[Youtube] login successful");
            } else {
                log.warn("[Youtube] login may have failed, check browser");
            }

            page.close();
        } catch (Exception e) {
            log.error("[Youtube] error logging in: {}", e.getMessage());
        }
    }

    public void scrape() {
        log.info("Scraping starts----->");
        try {
            Page page = playwrightManager.newPage();
            page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
            page.navigate("https://www.youtube.com/playlist?list=WL");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // session expired, need to login again
            if(page.url().contains("accounts.google.com")) {
                log.warn("[Youtube] session expired, need to login again");
                page.close();
                return;
            }
            
            scrolltoBottom(page);

            List<IngestRequest> videos = scrapeVideos(page);
            log.info("[Youtube] scraped {} videos", videos.size());

            //saving the bookmarks to the jsons folder
            saveBookmarksToFile(videos, "src/main/java/com/subham/convergence/ingestion/jsons/youtube_videos.json");
            int saved = ingestionService.ingestAll(videos);
            log.info("[Youtube] saved {} videos to DB", saved);
            
            page.close();

        } catch (Exception e) {
            log.error("[Youtube] error scraping videos: {}", e.getMessage());
        }
    }

    private List<IngestRequest> scrapeVideos(Page page) {
        List<IngestRequest> items = new ArrayList<>();

        Locator videoElements = page.locator("ytd-playlist-video-renderer");
        int count = videoElements.count();
        log.info("[YouTube] Found {} video elements", count);

        for (int i = 0; i < count; i++) {
            try {
                Locator el = videoElements.nth(i);

                // title + url
                Locator titleEl = el.locator("#video-title");
                if (!titleEl.isVisible()) continue;

                String title = titleEl.textContent().trim();
                String href  = titleEl.getAttribute("href");
                if (href == null || href.isBlank()) continue;

                String url     = "https://www.youtube.com" + href.split("&")[0];
                String videoId = extractVideoId(url);

                // channel
                Locator channelEl = el.locator("#channel-name a");
                String channel = channelEl.isVisible() ? channelEl.textContent().trim() : "";

                // duration
                Locator durationEl = el.locator("span.ytd-thumbnail-overlay-time-status-renderer");
                String duration = durationEl.isVisible() ? durationEl.textContent().trim() : "";

                // thumbnail
                Locator thumbEl = el.locator("img#img");
                String thumbnail = thumbEl.isVisible() ? thumbEl.getAttribute("src") : null;

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
                log.warn("[YouTube] Skipped video element: {}", e.getMessage());
            }
        }

        return items;
    }

    private String extractVideoId(String url) {
        try {
            String[] parts = url.split("v=");
            if (parts.length > 1) {
                return parts[1].split("&")[0];
            }
        } catch (Exception e) {
            log.warn("[YouTube] Failed to extract video ID from URL: {}", url);
        }
        return null;
    }

    private void scrolltoBottom(Page page) {
        int previousHeight = 0;
        while (true) {
            page.evaluate("window.scrollTo(0, document.documentElement.scrollHeight)");
            page.waitForTimeout(2000);
            int currentHeight = (int) page.evaluate("document.documentElement.scrollHeight");
            if (currentHeight == previousHeight) break;
            previousHeight = currentHeight;
        }
    }

    private void saveBookmarksToFile(List<IngestRequest> videos, String filename){
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), videos);
            log.info("[YouTube] Saved scraped videos to file: {}", filename);
        } catch (Exception e) {
            log.error("[YouTube] Failed to save videos to file: {}", e.getMessage());
        }
    }
}

