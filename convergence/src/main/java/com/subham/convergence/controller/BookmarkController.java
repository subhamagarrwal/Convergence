package com.subham.convergence.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.subham.convergence.dto.response.ApiResponse;
import com.subham.convergence.dto.response.UnifiedBookmarkResponse;
import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.service.BookmarkService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // GET /api/bookmarks/user/{userId}  →  ALL platforms merged
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<UnifiedBookmarkResponse>>> getAll(
            @PathVariable UUID userId) {
        List<UnifiedBookmarkResponse> bookmarks = bookmarkService.getAllBookmarksForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(bookmarks,
                "Retrieved " + bookmarks.size() + " bookmarks across all platforms"));
    }

    // GET /api/bookmarks/user/{userId}/platform/CHROME
    @GetMapping("/user/{userId}/platform/{platform}")
    public ResponseEntity<ApiResponse<List<UnifiedBookmarkResponse>>> getByPlatform(
            @PathVariable UUID userId,
            @PathVariable PlatformType platform) {
        List<UnifiedBookmarkResponse> bookmarks =
                bookmarkService.getBookmarksByPlatform(userId, platform);
        return ResponseEntity.ok(ApiResponse.success(bookmarks,
                "Retrieved " + bookmarks.size() + " " + platform + " bookmarks"));
    }

    // GET /api/bookmarks/user/{userId}/search?q=react
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<ApiResponse<List<UnifiedBookmarkResponse>>> search(
            @PathVariable UUID userId,
            @RequestParam String q) {
        List<UnifiedBookmarkResponse> results = bookmarkService.searchAll(userId, q);
        return ResponseEntity.ok(ApiResponse.success(results,
                "Found " + results.size() + " results for '" + q + "'"));
    }

    // GET /api/bookmarks/user/{userId}/count
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<ApiResponse<Long>> count(@PathVariable UUID userId) {
        long total = bookmarkService.countAll(userId);
        return ResponseEntity.ok(ApiResponse.success(total, "Total bookmarks: " + total));
    }
}