package com.subham.convergence.ingestion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subham.convergence.dto.response.ApiResponse;
import com.subham.convergence.ingestion.common.XCredential;
import com.subham.convergence.ingestion.dto.XHeartbeatPayload;
import com.subham.convergence.ingestion.dto.XKeyPayload;
import com.subham.convergence.ingestion.dto.XSyncPayload;
import com.subham.convergence.ingestion.dto.XSyncResponse;
import com.subham.convergence.ingestion.repository.XCredentialRepository;
import com.subham.convergence.ingestion.service.XBookmarkProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/x")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class XExtensionController {

    private final XBookmarkProcessor bookmarkProcessor;
    private final XCredentialRepository credentialRepository;

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(@RequestBody XHeartbeatPayload payload) {
        log.info("[X] Heartbeat from user {}", payload.getUserId());
        credentialRepository.findByUserId(payload.getUserId()).ifPresent(cred -> {
            cred.setLastActiveTimestamp(System.currentTimeMillis());
            credentialRepository.save(cred);
        });
        return ResponseEntity.ok(ApiResponse.success(null, "Heartbeat received"));
    }

    @PostMapping("/keys")
    public ResponseEntity<ApiResponse<Void>> saveKeys(@RequestBody XKeyPayload payload) {
        log.info("[X] Saving credentials for user {}", payload.getUserId());

        XCredential cred = credentialRepository.findByUserId(payload.getUserId())
                .orElse(new XCredential());

        cred.setUserId(payload.getUserId());
        cred.setAuthToken(payload.getAuthToken());
        cred.setCsrfToken(payload.getCsrfToken());
        cred.setQueryId(payload.getQueryId());
        cred.setFeaturesJson(payload.getFeaturesJson());
        cred.setFullCookieString(payload.getFullCookieString());
        cred.setLastActiveTimestamp(System.currentTimeMillis());

        if (payload.getFingerprint() != null) {
            cred.setUserAgent(payload.getFingerprint().getUserAgent());
            cred.setSecChUa(payload.getFingerprint().getSecChUa());
            cred.setSecChPlatform(payload.getFingerprint().getSecChPlatform());
        }

        credentialRepository.save(cred);
        return ResponseEntity.ok(ApiResponse.success(null, "Keys saved"));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<XSyncResponse>> sync(@RequestBody XSyncPayload payload) {
        log.info("[X] Sync from user {}", payload.getUserId());
        XSyncResponse result = bookmarkProcessor.processSync(payload);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }
}