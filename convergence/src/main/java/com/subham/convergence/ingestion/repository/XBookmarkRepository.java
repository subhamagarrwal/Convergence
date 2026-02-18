package com.subham.convergence.ingestion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.subham.convergence.ingestion.common.XBookmark;

@Repository
public interface XBookmarkRepository extends JpaRepository<XBookmark, Long> {
    Optional<XBookmark> findByUserIdAndTweetId(String userId, String tweetId);
    List<XBookmark> findByUserIdOrderBySyncedAtDesc(String userId);
    long countByUserId(String userId);
}