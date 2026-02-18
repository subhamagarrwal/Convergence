package com.subham.convergence.ingestion.common;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "youtube_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "google_email")
    private String googleEmail;

    // Full cookie JSON string from Playwright
    @Column(name = "cookies_json", columnDefinition = "TEXT")
    private String cookiesJson;

    @Column(name = "is_valid")
    private Boolean isValid = false;

    @Column(name = "logged_in_at")
    private LocalDateTime loggedInAt;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
}