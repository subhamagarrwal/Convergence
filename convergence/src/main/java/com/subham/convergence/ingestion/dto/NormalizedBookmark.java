package com.subham.convergence.ingestion.dto;

import java.util.Date;
import com.subham.convergence.enums.PlatformType;

public record NormalizedBookmark(
    String id,
    String bookmarkId,
    String userId,
    String title,
    String content,
    String url,
    PlatformType platform,
    Date timestamp
) {}
