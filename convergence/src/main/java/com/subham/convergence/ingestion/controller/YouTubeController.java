package com.subham.convergence.ingestion.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.subham.convergence.dto.response.ApiResponse;
import com.subham.convergence.ingestion.dto.YouTubeExtensionPayload;
import com.subham.convergence.ingestion.dto.YouTubeSyncResponse;
import com.subham.convergence.ingestion.service.YouTubeExtensionProcessor;
import com.subham.convergence.ingestion.service.YouTubeScraperService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class YouTubeController {

    private final YouTubeExtensionProcessor extensionProcessor;
    private final YouTubeScraperService scraperService;

    // ──────────────────────────────────────────────────────────
    // FROM EXTENSION — receives parsed video list
    // ──────────────────────────────────────────────────────────
    @PostMapping("/extension/sync")
    public ResponseEntity<ApiResponse<YouTubeSyncResponse>> extensionSync(
            @RequestBody YouTubeExtensionPayload payload) {

        log.info("[YouTube] Extension sync: user={} videos={}",
                payload.getUserId(),
                payload.getVideos() != null ? payload.getVideos().size() : 0);

        YouTubeSyncResponse result = extensionProcessor.processAndSave(payload);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    // ──────────────────────────────────────────────────────────
    // BACKEND FALLBACK — Playwright scraping (dead man's switch)
    // ──────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestParam String userId) {
        log.info("[YouTube] Backend login for user {}", userId);
        String result = scraperService.openLoginBrowser(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(result, "Login complete"));
    }

    @PostMapping("/sync/{userId}")
    public ResponseEntity<ApiResponse<YouTubeSyncResponse>> backendSync(
            @PathVariable String userId) {

        log.info("[YouTube] Backend scrape for user {}", userId);
        YouTubeSyncResponse result = scraperService.scrapeWatchLater(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                scraperService.getStatus(UUID.fromString(userId)), "YouTube status"));
    }

    @DeleteMapping("/logout/{userId}")
    public ResponseEntity<ApiResponse<Void>> logout(@PathVariable String userId) {
        scraperService.invalidateSession(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(null, "YouTube session cleared"));
    }
}