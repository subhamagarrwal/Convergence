package com.subham.convergence.ingestion.repository;

import com.subham.convergence.ingestion.common.YouTubeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface YouTubeSessionRepository extends JpaRepository<YouTubeSession, Long> {
    Optional<YouTubeSession> findByUserId(UUID userId);
}