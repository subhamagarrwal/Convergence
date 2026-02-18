package com.subham.convergence.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.subham.convergence.dto.response.UnifiedBookmarkResponse;
import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.ingestion.common.XBookmark;
import com.subham.convergence.ingestion.repository.XBookmarkRepository;
import com.subham.convergence.model.Bookmark;
import com.subham.convergence.repository.BookmarkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final XBookmarkRepository xBookmarkRepository;

    /**
     * Get ALL bookmarks for a user — unified across Chrome, X, YouTube
     */
    public List<UnifiedBookmarkResponse> getAllBookmarksForUser(UUID userId) {
        List<UnifiedBookmarkResponse> all = new ArrayList<>();

        // Chrome + YouTube bookmarks (from unified bookmarks table)
        List<Bookmark> bookmarks = bookmarkRepository.findByUserId(userId);
        for (Bookmark b : bookmarks) {
            all.add(mapBookmark(b));
        }

        // X bookmarks (from x_bookmarks table, richer data)
        List<XBookmark> xBookmarks = xBookmarkRepository.findByUserIdOrderBySyncedAtDesc(userId.toString());
        for (XBookmark xb : xBookmarks) {
            all.add(mapXBookmark(xb));
        }

        // Sort by date descending
        all.sort(Comparator.comparing(
                UnifiedBookmarkResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return all;
    }

    /**
     * Get bookmarks filtered by platform
     */
    public List<UnifiedBookmarkResponse> getBookmarksByPlatform(UUID userId, PlatformType platform) {
        if (platform == PlatformType.X) {
            return xBookmarkRepository.findByUserIdOrderBySyncedAtDesc(userId.toString())
                    .stream()
                    .map(this::mapXBookmark)
                    .collect(Collectors.toList());
        }

        return bookmarkRepository.findByUserIdAndPlatform(userId, platform)
                .stream()
                .map(this::mapBookmark)
                .collect(Collectors.toList());
    }

    /**
     * Search bookmarks by keyword
     */
    public List<UnifiedBookmarkResponse> searchAll(UUID userId, String query) {
        String q = query.toLowerCase();
        return getAllBookmarksForUser(userId).stream()
                .filter(b -> {
                    String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
                    String desc = b.getDescription() != null ? b.getDescription().toLowerCase() : "";
                    String url = b.getUrl() != null ? b.getUrl().toLowerCase() : "";
                    return title.contains(q) || desc.contains(q) || url.contains(q);
                })
                .collect(Collectors.toList());
    }

    /**
     * Count all bookmarks for a user
     */
    public long countAll(UUID userId) {
        long unified = bookmarkRepository.countByUserId(userId);
        long xCount = xBookmarkRepository.countByUserId(userId.toString());
        return unified + xCount;
    }

    // ════════════════════════════════════════════
    // MAPPERS
    // ════════════════════════════════════════════
    private UnifiedBookmarkResponse mapBookmark(Bookmark b) {
        return UnifiedBookmarkResponse.builder()
                .id(b.getId().toString())
                .url(b.getUrl())
                .title(b.getTitle())
                .description(b.getDescription())
                .platform(b.getPlatform())
                .contentType(b.getContentType() != null ? b.getContentType().name() : null)
                .createdAt(b.getCreatedAt())
                .build();
    }

    private UnifiedBookmarkResponse mapXBookmark(XBookmark xb) {
        return UnifiedBookmarkResponse.builder()
                .id(xb.getId() != null ? xb.getId().toString() : xb.getTweetId())
                .url(xb.getTweetUrl())
                .title(xb.getContent() != null && xb.getContent().length() > 100
                        ? xb.getContent().substring(0, 100) + "..."
                        : xb.getContent())
                .description(xb.getContent())
                .platform(PlatformType.X)
                .contentType("ARTICLE")
                .createdAt(xb.getSyncedAt())
                .authorName(xb.getAuthorDisplayName())
                .authorHandle(xb.getAuthorUsername())
                .build();
    }
}