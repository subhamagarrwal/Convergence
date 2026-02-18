package com.subham.convergence.ingestion.service;

import com.subham.convergence.enums.ContentType;
import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.ingestion.dto.ChromeBookmarkPayload;
import com.subham.convergence.ingestion.dto.ChromeBookmarkPayload.ChromeBookmarkItem;
import com.subham.convergence.ingestion.dto.XSyncResponse;
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
public class ChromeBookmarkProcessor {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository     userRepository;

    @Transactional
    public XSyncResponse processAndSave(ChromeBookmarkPayload payload) {
        Optional<User> userOpt = userRepository.findById(UUID.fromString(payload.getUserId()));
        if (userOpt.isEmpty()) {
            return XSyncResponse.builder()
                    .status("error")
                    .message("User not found: " + payload.getUserId())
                    .build();
        }

        User user = userOpt.get();
        int newCount     = 0;
        int updatedCount = 0;

        for (ChromeBookmarkItem item : payload.getBookmarks()) {
            if (item.getUrl() == null || item.getUrl().isBlank()) continue;

            // Skip chrome:// internal pages
            if (item.getUrl().startsWith("chrome://")) continue;

            try {
                boolean exists = bookmarkRepository
                        .existsByUrlAndUserId(item.getUrl(), user.getId());

                if (!exists) {
                    Bookmark b = new Bookmark();
                    b.setUser(user);
                    b.setPlatform(PlatformType.CHROME);
                    b.setExternalId(item.getChromeId());
                    b.setUrl(item.getUrl());
                    b.setTitle(item.getTitle() != null ? item.getTitle() : item.getUrl());
                    b.setDescription("Chrome bookmark" +
                            (item.getParentTitle() != null
                                    ? " — folder: " + item.getParentTitle()
                                    : ""));
                    b.setContentType(guessContentType(item.getUrl()));
                    b.setCreatedAt(LocalDateTime.now());
                    b.setUpdatedAt(LocalDateTime.now());
                    bookmarkRepository.save(b);
                    newCount++;
                }
                // Chrome bookmarks don't have metrics to update, so skip updatedCount
            } catch (Exception e) {
                log.warn("[ChromeProcessor] Skipped {}: {}", item.getUrl(), e.getMessage());
            }
        }

        long total = bookmarkRepository.countByUserIdAndPlatform(user.getId(), PlatformType.CHROME);

        log.info("[ChromeProcessor] user={} new={} total={}", payload.getUserId(), newCount, total);

        return XSyncResponse.builder()
                .status("success")
                .newBookmarks(newCount)
                .updatedBookmarks(updatedCount)
                .totalBookmarks(total)
                .lastSyncTime(LocalDateTime.now().toString())
                .message(String.format("Chrome sync: %d new bookmark(s)", newCount))
                .build();
    }

    private ContentType guessContentType(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) return ContentType.VIDEO;
        if (lower.endsWith(".pdf"))                                        return ContentType.ARTICLE;
        if (lower.contains("github.com"))                                  return ContentType.ARTICLE;
        return ContentType.ARTICLE;
    }
}