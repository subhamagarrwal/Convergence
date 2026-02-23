package com.subham.convergence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.model.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {
    boolean existsByUrl(String url);
    long countByPlatform(PlatformType platform);
}