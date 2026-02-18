package com.subham.convergence.dto.response;

import com.subham.convergence.enums.PlatformType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedBookmarkResponse {
    private String id;
    private String url;
    private String title;
    private String description;
    private PlatformType platform;
    private String contentType;
    private LocalDateTime createdAt;
    private String authorName;
    private String authorHandle;
}