package com.subham.convergence.ingestion.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeSyncResponse {
    private String status;
    private int newVideos;
    private int totalVideos;
    private String lastSyncTime;
    private String message;
    private List<YouTubeVideoItem> videos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YouTubeVideoItem {
        private String videoId;
        private String title;
        private String channelName;
        private String thumbnailUrl;
        private String url;
        private String publishedAt;
    }
}