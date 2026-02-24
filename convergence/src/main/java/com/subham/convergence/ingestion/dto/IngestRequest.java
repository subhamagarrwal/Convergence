package com.subham.convergence.ingestion.dto;

import com.subham.convergence.enums.PlatformType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class IngestRequest {
    private String url;
    private String title;
    private PlatformType platform;
    private String description;
    private String externalId;
    private String thumbnailUrl;
    private Map<String, Object> metadata;

}