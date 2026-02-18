package com.subham.convergence.ingestion.dto;

import com.subham.convergence.enums.PlatformType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Generic ingestion request — used by Reddit, YouTube, Medium etc.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestRequest {
    private String userId;
    private PlatformType platform;
    private String rawData;
}