package com.subham.convergence.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XSyncResponse {
    private String status;
    private int newBookmarks;
    private int updatedBookmarks;
    private long totalBookmarks;
    private String lastSyncTime;
    private String message;
}