package com.subham.convergence.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.subham.convergence.enums.PlatformType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "platform_connections", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_platform", columnList = "platform"),
    @Index(name = "idx_token_expires", columnList = "token_expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformConnection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformType platform;

    @Column(nullable = false, length = 255)
    private String platformUsername;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    @Column
    private LocalDateTime tokenExpiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime connectedAt;

    @Column(nullable = false)
    private LocalDateTime lastSyncAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        lastSyncAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }
}
