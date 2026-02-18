package com.subham.convergence.ingestion.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class XSyncPayload {
    private String userId;
    private List<XTweetItem> bookmarks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class XTweetItem {
        private String tweetId;
        private String tweetUrl;
        private String authorUsername;
        private String authorDisplayName;
        private String authorProfileImage;
        private String content;
        private String mediaUrls;
        private Integer likeCount;
        private Integer retweetCount;
        private Integer replyCount;
    }
}