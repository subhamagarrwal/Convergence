package com.subham.convergence.ingestion.common;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "x_bookmarks",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tweet_id"}),
    indexes = {
        @Index(name = "idx_xb_user_synced", columnList = "user_id, synced_at"),
        @Index(name = "idx_xb_tweet_id",    columnList = "tweet_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class XBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "tweet_id", nullable = false)
    private String tweetId;

    @Column(name = "tweet_url")
    private String tweetUrl;

    @Column(name = "author_username")
    private String authorUsername;

    @Column(name = "author_display_name")
    private String authorDisplayName;

    @Column(name = "author_profile_image")
    private String authorProfileImage;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "media_urls", columnDefinition = "TEXT")
    private String mediaUrls;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "retweet_count")
    private Integer retweetCount;

    @Column(name = "reply_count")
    private Integer replyCount;

    @Column(name = "tweet_created_at")
    private LocalDateTime tweetCreatedAt;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    // Links to the unified Bookmark table
    @Column(name = "convergence_bookmark_id")
    private UUID convergenceBookmarkId;

    // "extension" or "backend_fallback"
    @Column(name = "source")
    private String source;

    @PrePersist
    public void prePersist() {
        if (syncedAt == null) syncedAt = LocalDateTime.now();
        if (isDeleted == null) isDeleted = false;
    }
}