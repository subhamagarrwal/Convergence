package com.subham.convergence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.subham.convergence.model.Bookmark;


public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {
    List<Bookmark> findByPlatform(String platform);
    List<Bookmark> findByTitle(String title);


}
