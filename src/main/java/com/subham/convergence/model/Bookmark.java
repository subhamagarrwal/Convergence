package com.subham.convergence.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.subham.convergence.enums.ContentType;
import com.subham.convergence.enums.PlatformType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "bookmarks")
@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
@Table(name = "bookmarks")
public class Bookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformType platform;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private String description;

    @Column(columnDefinition="json")
    private String metadata;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType contentType;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
    }

}
