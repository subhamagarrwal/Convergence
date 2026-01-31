package com.subham.convergence.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.subham.convergence.enums.ContentType;
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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bookmarks", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_platform", columnList = "platform"),
    @Index(name = "idx_url", columnList = "url"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class Bookmark {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformType platform;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(length = 2000)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType contentType;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
