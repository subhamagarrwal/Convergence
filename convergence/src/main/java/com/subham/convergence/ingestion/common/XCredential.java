package com.subham.convergence.ingestion.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "x_credentials")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class XCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    // ------- Session Keys -------
    @Column(length = 500)
    private String authToken;

    @Column(length = 500)
    private String csrfToken;

    @Column(length = 200)
    private String queryId;

    // ------- Browser Fingerprint -------
    @Column(length = 500)
    private String userAgent;

    @Column(name = "sec_ch_ua", length = 200)
    private String secChUa;

    @Column(name = "sec_ch_platform", length = 50)
    private String secChPlatform;

    // ------- Dead Man's Switch heartbeat -------
    private Long lastActiveTimestamp;

    // ------- Full cookie string -------
    @Column(name = "full_cookie_string", columnDefinition = "TEXT")
    private String fullCookieString;

    // ------- GraphQL features JSON -------
    @Column(name = "features_json", columnDefinition = "TEXT")
    private String featuresJson;
}