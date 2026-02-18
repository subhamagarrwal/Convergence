package com.subham.convergence.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.subham.convergence.enums.ContentType;
import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.ingestion.common.XBookmark;
import com.subham.convergence.ingestion.dto.XSyncPayload;
import com.subham.convergence.ingestion.dto.XSyncResponse;
import com.subham.convergence.ingestion.repository.XBookmarkRepository;
import com.subham.convergence.model.Bookmark;
import com.subham.convergence.model.User;
import com.subham.convergence.repository.BookmarkRepository;
import com.subham.convergence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class XBookmarkProcessor {

    private final XBookmarkRepository xBookmarkRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    /**
     * Process sync from the Chrome extension (pre-parsed tweets)
     */
    @Transactional
    public XSyncResponse processSync(XSyncPayload payload) {
        String userId = payload.getUserId();
        int newCount = 0;
        int updatedCount = 0;

        if (payload.getBookmarks() == null) {
            return XSyncResponse.builder()
                    .status("success")
                    .message("No bookmarks to sync")
                    .build();
        }

        for (XSyncPayload.XTweetItem tweet : payload.getBookmarks()) {
            try {
                Optional<XBookmark> existing = xBookmarkRepository
                        .findByUserIdAndTweetId(userId, tweet.getTweetId());

                if (existing.isEmpty()) {
                    XBookmark xb = new XBookmark();
                    xb.setUserId(userId);
                    xb.setTweetId(tweet.getTweetId());
                    xb.setTweetUrl(tweet.getTweetUrl());
                    xb.setAuthorUsername(tweet.getAuthorUsername());
                    xb.setAuthorDisplayName(tweet.getAuthorDisplayName());
                    xb.setAuthorProfileImage(tweet.getAuthorProfileImage());
                    xb.setContent(tweet.getContent());
                    xb.setMediaUrls(tweet.getMediaUrls());
                    xb.setLikeCount(tweet.getLikeCount());
                    xb.setRetweetCount(tweet.getRetweetCount());
                    xb.setReplyCount(tweet.getReplyCount());
                    xb.setSource("extension");
                    xBookmarkRepository.save(xb);

                    // Also save to unified bookmarks
                    saveToUnified(userId, xb);
                    newCount++;
                } else {
                    // Update metrics
                    XBookmark xb = existing.get();
                    xb.setLikeCount(tweet.getLikeCount());
                    xb.setRetweetCount(tweet.getRetweetCount());
                    xb.setReplyCount(tweet.getReplyCount());
                    xb.setSyncedAt(LocalDateTime.now());
                    xBookmarkRepository.save(xb);
                    updatedCount++;
                }
            } catch (Exception e) {
                log.warn("[XProcessor] Skipped tweet {}: {}", tweet.getTweetId(), e.getMessage());
            }
        }

        long total = xBookmarkRepository.countByUserId(userId);
        log.info("[XProcessor] user={} new={} updated={} total={}", userId, newCount, updatedCount, total);

        return XSyncResponse.builder()
                .status("success")
                .newBookmarks(newCount)
                .updatedBookmarks(updatedCount)
                .totalBookmarks(total)
                .lastSyncTime(LocalDateTime.now().toString())
                .message(String.format("X sync: %d new, %d updated", newCount, updatedCount))
                .build();
    }

    /**
     * Process from backend fallback (raw X API JSON)
     */
    @Transactional
    public void processAndSave(String userId, JsonNode root, String source) {
        JsonNode entries = root.path("data").path("bookmark_timeline_v2")
                .path("timeline").path("instructions");

        if (!entries.isArray()) {
            log.warn("[XProcessor] No instructions array in response for user {}", userId);
            return;
        }

        int count = 0;
        for (JsonNode instruction : entries) {
            JsonNode timelineEntries = instruction.path("entries");
            if (!timelineEntries.isArray()) continue;

            for (JsonNode entry : timelineEntries) {
                JsonNode tweetResult = entry.path("content").path("itemContent")
                        .path("tweet_results").path("result");

                if (tweetResult.isMissingNode()) continue;

                try {
                    String tweetId = tweetResult.path("rest_id").asText();
                    JsonNode legacy = tweetResult.path("legacy");
                    JsonNode userResult = tweetResult.path("core").path("user_results")
                            .path("result").path("legacy");

                    if (tweetId == null || tweetId.isEmpty()) continue;

                    Optional<XBookmark> existing = xBookmarkRepository.findByUserIdAndTweetId(userId, tweetId);
                    if (existing.isPresent()) continue;

                    XBookmark xb = new XBookmark();
                    xb.setUserId(userId);
                    xb.setTweetId(tweetId);
                    xb.setTweetUrl("https://x.com/i/status/" + tweetId);
                    xb.setContent(legacy.path("full_text").asText(""));
                    xb.setAuthorUsername(userResult.path("screen_name").asText(""));
                    xb.setAuthorDisplayName(userResult.path("name").asText(""));
                    xb.setAuthorProfileImage(userResult.path("profile_image_url_https").asText(""));
                    xb.setLikeCount(legacy.path("favorite_count").asInt(0));
                    xb.setRetweetCount(legacy.path("retweet_count").asInt(0));
                    xb.setReplyCount(legacy.path("reply_count").asInt(0));
                    xb.setSource(source);
                    xBookmarkRepository.save(xb);

                    saveToUnified(userId, xb);
                    count++;

                } catch (Exception e) {
                    log.debug("[XProcessor] Skipped entry: {}", e.getMessage());
                }
            }
        }

        log.info("[XProcessor] Backend fallback saved {} bookmarks for user {}", count, userId);
    }

    private void saveToUnified(String userId, XBookmark xb) {
        try {
            UUID userUuid = UUID.fromString(userId);
            String url = xb.getTweetUrl();

            if (bookmarkRepository.existsByUrlAndUserId(url, userUuid)) return;

            Optional<User> userOpt = userRepository.findById(userUuid);
            if (userOpt.isEmpty()) return;

            Bookmark b = new Bookmark();
            b.setUser(userOpt.get());
            b.setPlatform(PlatformType.X);
            b.setExternalId(xb.getTweetId());
            b.setUrl(url);
            b.setTitle(truncate(xb.getContent(), 200));
            b.setDescription(xb.getContent());
            b.setContentType(ContentType.ARTICLE);
            bookmarkRepository.save(b);

            xb.setConvergenceBookmarkId(b.getId());
            xBookmarkRepository.save(xb);
        } catch (Exception e) {
            log.warn("[XProcessor] Failed to save unified for tweet {}: {}", xb.getTweetId(), e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}