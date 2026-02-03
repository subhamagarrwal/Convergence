package com.subham.convergence.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.subham.convergence.enums.PlatformType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformConnectionResponse {
    private UUID id;
    private PlatformType platform;
    private String platformUsername;
    private boolean isActive;
    private LocalDateTime connectedAt;
    private LocalDateTime lastSyncAt;
    private Long bookmarksCount;
}