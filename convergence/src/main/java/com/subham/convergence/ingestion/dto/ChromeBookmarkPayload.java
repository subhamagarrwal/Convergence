package com.subham.convergence.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChromeBookmarkPayload {

    private String userId;
    private List<ChromeBookmarkItem> bookmarks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChromeBookmarkItem {
        private String chromeId;
        private String title;
        private String url;
        private String dateAdded;
        private String parentTitle;
    }
}