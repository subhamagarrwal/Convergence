package com.subham.convergence.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeExtensionPayload {

    private String userId;
    private List<VideoItem> videos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoItem {
        private String videoId;
        private String title;
        private String channelName;
        private String thumbnailUrl;
        private String url;
        private String duration;
    }
}