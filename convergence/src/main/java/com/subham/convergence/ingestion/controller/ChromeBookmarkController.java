package com.subham.convergence.ingestion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subham.convergence.dto.response.ApiResponse;
import com.subham.convergence.ingestion.dto.ChromeBookmarkPayload;
import com.subham.convergence.ingestion.dto.XSyncResponse;
import com.subham.convergence.ingestion.service.ChromeBookmarkProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/bookmarks/chrome")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ChromeBookmarkController {

    private final ChromeBookmarkProcessor processor;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<XSyncResponse>> sync(
            @RequestBody ChromeBookmarkPayload payload) {

        log.info("[Chrome] Sync: user={} bookmarks={}",
                payload.getUserId(),
                payload.getBookmarks() != null ? payload.getBookmarks().size() : 0);

        XSyncResponse result = processor.processAndSave(payload);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }
}