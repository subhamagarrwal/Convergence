package com.subham.convergence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.model.PlatformConnection;

@Repository
public interface PlatformConnectionRepository extends JpaRepository<PlatformConnection, UUID> {
    
    // ============ BASIC QUERIES ============
    
    List<PlatformConnection> findByUserId(UUID userId);
    
    Optional<PlatformConnection> findByUserIdAndPlatform(UUID userId, PlatformType platform);
    
    boolean existsByUserIdAndPlatform(UUID userId, PlatformType platform);
    
    

    
    @Query("SELECT pc FROM PlatformConnection pc WHERE pc.user.id = :userId AND pc.isActive = true")
    List<PlatformConnection> findActiveConnectionsByUserId(@Param("userId") UUID userId);
    
    
    // ============ TOKEN EXPIRATION ============
    
    @Query("SELECT pc FROM PlatformConnection pc WHERE pc.user.id = :userId AND pc.tokenExpiresAt <= :now")
    List<PlatformConnection> findExpiredTokensByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT pc FROM PlatformConnection pc WHERE pc.tokenExpiresAt <= :now AND pc.isActive = true")
    List<PlatformConnection> findAllExpiredTokens(@Param("now") LocalDateTime now);
    
    
    // ============ PLATFORM SPECIFIC ============
    
    List<PlatformConnection> findByPlatform(PlatformType platform);
    
    @Query("SELECT COUNT(pc) FROM PlatformConnection pc WHERE pc.platform = :platform")
    Long countByPlatform(@Param("platform") PlatformType platform);
    
    
    // ============ SYNC STATUS ============
    
    @Query("SELECT pc FROM PlatformConnection pc WHERE pc.user.id = :userId AND pc.lastSyncAt < :date")
    List<PlatformConnection> findConnectionsNotSyncedSince(
        @Param("userId") UUID userId,
        @Param("date") LocalDateTime date
    );
    
    
    // ============ SEARCH ============
    
    @Query("SELECT pc FROM PlatformConnection pc WHERE pc.user.id = :userId AND LOWER(pc.platformUsername) LIKE LOWER(CONCAT('%', :username, '%'))")
    List<PlatformConnection> findByPlatformUsername(
        @Param("userId") UUID userId,
        @Param("username") String username
    );
    
    
    // ============ PAGINATION ============
    
    Page<PlatformConnection> findByUserId(UUID userId, Pageable pageable);
    
    
    // ============ AGGREGATION ============
    
    @Query("SELECT COUNT(pc) FROM PlatformConnection pc WHERE pc.user.id = :userId")
    Long countByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT COUNT(pc) FROM PlatformConnection pc WHERE pc.user.id = :userId AND pc.isActive = true")
    Long countActiveConnectionsByUserId(@Param("userId") UUID userId);
}
