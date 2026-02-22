package com.subham.convergence.ingestion.common;

import com.google.api.client.util.DateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.util.UUID;
import java.time.LocalDateTime;
@Entity
@Table(name = "x_bookmarks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class XBookmark {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "tweet_id", nullable = false)
    private String tweetId;


    @Column(name = "timestamp", nullable = false)
    private DateTime timestamp;    

    @Column(name = "Content", nullable = false)
    private String content;

    @Column(name = "profile_name", nullable = false)
    private String profileName;

    @Column(name = "is_liked")
    private Boolean isLiked;

    @Column(name = "url", nullable = false)
    private String tweetUrl;

}
