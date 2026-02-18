package com.subham.convergence.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class XKeyPayload {

    private String userId;
    private String authToken;
    private String csrfToken;
    private String queryId;
    private String featuresJson;
    private String fullCookieString;
    private Fingerprint fingerprint;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Fingerprint {
        private String userAgent;
        private String secChUa;
        private String secChPlatform;
    }
}