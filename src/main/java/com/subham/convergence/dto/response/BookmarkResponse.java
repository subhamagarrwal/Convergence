package com.subham.convergence.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.subham.convergence.enums.ContentType;
import com.subham.convergence.enums.PlatformType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkResponse {
    private UUID id;
    private String url;
    private String title;
    private String description;
    private PlatformType platform;
    private ContentType contentType;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime savedAt;
}