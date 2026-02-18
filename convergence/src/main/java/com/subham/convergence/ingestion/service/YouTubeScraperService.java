package com.subham.convergence.ingestion.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.subham.convergence.enums.ContentType;
import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.ingestion.common.YouTubeSession;
import com.subham.convergence.ingestion.dto.YouTubeSyncResponse;
import com.subham.convergence.ingestion.dto.YouTubeSyncResponse.YouTubeVideoItem;
import com.subham.convergence.ingestion.repository.YouTubeSessionRepository;
import com.subham.convergence.model.Bookmark;
import com.subham.convergence.model.User;
import com.subham.convergence.repository.BookmarkRepository;
import com.subham.convergence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class YouTubeScraperService {

    private final YouTubeSessionRepository sessionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final String WATCH_LATER_URL = "https://www.youtube.com/playlist?list=WL";
    
    // Define standard User Agent to look like a real PC
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // ════════════════════════════════════════════════════════════
    // STEP 1: Open browser for user to login manually
    // ════════════════════════════════════════════════════════════
    public String openLoginBrowser(UUID userId) {
        log.info("[YouTube] Opening login browser for user {}", userId);

        try (Playwright playwright = Playwright.create()) {
            BrowserType chromium = playwright.chromium();
            
            // Stealth args
            List<String> stealthArgs = List.of(
                "--disable-blink-features=AutomationControlled", // hides automation flag
                "--no-sandbox",
                "--disable-infobars"
            ); // <-- FIXED: Added semicolon

            // VISIBLE browser — user must see it to log in
            Browser browser = chromium.launch(new BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setSlowMo(100)
                    .setArgs(stealthArgs)
                    .setIgnoreDefaultArgs(List.of("--enable-automation"))
            );

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1200, 800)
                    .setUserAgent(USER_AGENT)
            );    
            
            Page page = context.newPage();

            // Go to YouTube login
            page.navigate("https://accounts.google.com/signin/v2/identifier?service=youtube");

            log.info("[YouTube] Waiting for user to complete login...");

            // Wait until user reaches YouTube homepage (max 5 min)
            page.waitForURL("https://www.youtube.com/**",
                    new Page.WaitForURLOptions().setTimeout(300_000));

            log.info("[YouTube] Login detected — capturing cookies");

            // Capture all cookies
            List<Cookie> cookies = context.cookies();
            String cookiesJson = objectMapper.writeValueAsString(cookies);

            // Try to get email from page
            String email = extractEmail(page);

            // Save session
            YouTubeSession session = sessionRepository.findByUserId(userId)
                    .orElse(new YouTubeSession());
            session.setUserId(userId);
            session.setCookiesJson(cookiesJson);
            session.setGoogleEmail(email);
            session.setIsValid(true);
            session.setLoggedInAt(LocalDateTime.now());
            sessionRepository.save(session);

            browser.close();
            log.info("[YouTube] Session saved for user {}", userId);
            return "Login successful. Session stored.";

        } catch (Exception e) {
            log.error("[YouTube] Login failed: {}", e.getMessage());
            throw new RuntimeException("YouTube login failed: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    // STEP 2: Scrape Watch Later using stored cookies
    // ════════════════════════════════════════════════════════════
    @Transactional
    public YouTubeSyncResponse scrapeWatchLater(UUID userId) {
        YouTubeSession session = sessionRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException(
                        "No YouTube session. Call /api/youtube/login first."));

        if (!Boolean.TRUE.equals(session.getIsValid())) {
            throw new RuntimeException("YouTube session expired. Re-login at /api/youtube/login");
        }

        log.info("[YouTube] Scraping Watch Later for user {}", userId);

        try (Playwright playwright = Playwright.create()) {
            
            // UPDATED: Must use stealth args here too!
            List<String> stealthArgs = List.of(
                "--disable-blink-features=AutomationControlled",
                "--no-sandbox"
            );

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(stealthArgs) // <-- Added stealth args
            );

            // UPDATED: Must use same User Agent
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(USER_AGENT)
            );

            // Restore saved cookies
            List<Cookie> cookies = objectMapper.readValue(
                    session.getCookiesJson(),
                    new TypeReference<List<Cookie>>() {});
            context.addCookies(cookies);

            Page page = context.newPage();

            // Navigate to Watch Later
            page.navigate(WATCH_LATER_URL);

            // Check if redirected to login → session expired
            if (page.url().contains("accounts.google.com")) {
                session.setIsValid(false);
                sessionRepository.save(session);
                browser.close();
                throw new RuntimeException("YouTube session expired. Re-login required.");
            }

            // Wait for playlist to load
            page.waitForSelector("ytd-playlist-video-renderer",
                    new Page.WaitForSelectorOptions().setTimeout(15_000));

            // Scroll to load all videos
            List<YouTubeVideoItem> videos = scrollAndScrape(page);

            browser.close();

            // Save to bookmarks
            int newCount = 0;
            for (YouTubeVideoItem video : videos) {
                if (saveAsBookmark(userId, video)) newCount++;
            }

            session.setLastSyncAt(LocalDateTime.now());
            sessionRepository.save(session);

            long total = bookmarkRepository.countByUserIdAndPlatform(userId, PlatformType.YOUTUBE);

            log.info("[YouTube] user={} scraped={} new={} total={}",
                    userId, videos.size(), newCount, total);

            return YouTubeSyncResponse.builder()
                    .status("success")
                    .newVideos(newCount)
                    .totalVideos((int) total)
                    .lastSyncTime(LocalDateTime.now().toString())
                    .message(String.format("Scraped %d videos, %d new", videos.size(), newCount))
                    .videos(videos)
                    .build();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[YouTube] Scrape failed: {}", e.getMessage());
            throw new RuntimeException("YouTube scrape failed: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    // SCROLL + SCRAPE full playlist
    // ════════════════════════════════════════════════════════════
    private List<YouTubeVideoItem> scrollAndScrape(Page page) {
        List<YouTubeVideoItem> videos = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        int maxScrolls = 50;  // 50 x ~10 videos = ~500 videos max
        int scrollCount = 0;
        int previousCount = 0;
        int noChangeCount = 0;

        while (scrollCount < maxScrolls) {
            // Extract all currently visible videos
            List<ElementHandle> items = page.querySelectorAll(
                    "ytd-playlist-video-renderer");

            for (ElementHandle item : items) {
                try {
                    YouTubeVideoItem video = extractVideoData(item);
                    if (video != null && !seen.contains(video.getVideoId())) {
                        seen.add(video.getVideoId());
                        videos.add(video);
                    }
                } catch (Exception e) {
                    log.debug("[YouTube] Skipping item: {}", e.getMessage());
                }
            }

            // Scroll down
            page.evaluate("window.scrollTo(0, document.documentElement.scrollHeight)");
            page.waitForTimeout(1500);

            // Stop if no new items loaded after 3 consecutive scrolls
            if (videos.size() == previousCount) {
                noChangeCount++;
                if (noChangeCount >= 3) {
                    log.info("[YouTube] No new videos after {} scrolls — done", scrollCount);
                    break;
                }
            } else {
                noChangeCount = 0;
                previousCount = videos.size();
            }

            scrollCount++;
        }

        log.info("[YouTube] Total scraped: {}", videos.size());
        return videos;
    }

    // ════════════════════════════════════════════════════════════
    // EXTRACT DATA FROM ONE VIDEO ELEMENT
    // ════════════════════════════════════════════════════════════
    private YouTubeVideoItem extractVideoData(ElementHandle item) {
        // Title + URL from the <a> tag
        ElementHandle titleEl = item.querySelector(
                "a#video-title, h3 a, #meta a#video-title");
        if (titleEl == null) return null;

        String href  = titleEl.getAttribute("href");
        String title = titleEl.getAttribute("title");
        if (title == null) title = titleEl.innerText();
        if (href  == null || href.isEmpty()) return null;

        // Extract videoId from href (?v=VIDEO_ID)
        String videoId = null;
        if (href.contains("v=")) {
            videoId = href.split("v=")[1].split("&")[0];
        } else if (href.contains("/shorts/")) {
            videoId = href.split("/shorts/")[1].split("\\?")[0];
        }
        if (videoId == null || videoId.isEmpty()) return null;

        // Channel name
        String channel = "";
        ElementHandle channelEl = item.querySelector(
                "ytd-channel-name a, #channel-name a, .ytd-channel-name");
        if (channelEl != null) channel = channelEl.innerText().trim();

        // Thumbnail
        String thumbnail = "";
        ElementHandle thumbEl = item.querySelector("img#img, img.yt-core-image");
        if (thumbEl != null) {
            thumbnail = thumbEl.getAttribute("src");
            if (thumbnail == null) thumbnail = thumbEl.getAttribute("data-thumb");
        }
        if (thumbnail == null) thumbnail = "https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg";

        return YouTubeVideoItem.builder()
                .videoId(videoId)
                .title(title.trim())
                .channelName(channel)
                .thumbnailUrl(thumbnail)
                .url("https://www.youtube.com/watch?v=" + videoId)
                .publishedAt(LocalDateTime.now().toString())
                .build();
    }

    // ════════════════════════════════════════════════════════════
    // SAVE TO UNIFIED BOOKMARK TABLE
    // ════════════════════════════════════════════════════════════
    private boolean saveAsBookmark(UUID userId, YouTubeVideoItem video) {
        try {
            if (bookmarkRepository.existsByUrlAndUserId(video.getUrl(), userId)) return false;

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) return false;

            Bookmark b = new Bookmark();
            b.setUser(userOpt.get());
            b.setPlatform(PlatformType.YOUTUBE);
            b.setExternalId(video.getVideoId());
            b.setUrl(video.getUrl());
            b.setTitle(video.getTitle());
            b.setDescription("Channel: " + video.getChannelName());
            b.setThumbnailUrl(video.getThumbnailUrl());
            b.setContentType(ContentType.VIDEO);
            b.setCreatedAt(LocalDateTime.now());
            b.setUpdatedAt(LocalDateTime.now());
            bookmarkRepository.save(b);
            return true;

        } catch (Exception e) {
            log.warn("[YouTube] Save failed for {}: {}", video.getVideoId(), e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════
    private String extractEmail(Page page) {
        try {
            ElementHandle el = page.querySelector(
                    "yt-formatted-string#account-name, #email, [data-email]");
            return el != null ? el.innerText().trim() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    public Map<String, Object> getStatus(UUID userId) {
        Map<String, Object> status = new HashMap<>();
        sessionRepository.findByUserId(userId).ifPresentOrElse(
                s -> {
                    status.put("connected", Boolean.TRUE.equals(s.getIsValid()));
                    status.put("email", s.getGoogleEmail());
                    status.put("loggedInAt", s.getLoggedInAt());
                    status.put("lastSync", s.getLastSyncAt());
                },
                () -> status.put("connected", false)
        );
        return status;
    }

    @Transactional
    public void invalidateSession(UUID userId) {
        sessionRepository.findByUserId(userId).ifPresent(s -> {
            s.setIsValid(false);
            s.setCookiesJson(null);
            sessionRepository.save(s);
        });
    }
}