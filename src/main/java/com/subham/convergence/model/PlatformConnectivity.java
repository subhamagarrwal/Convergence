package com.subham.convergence.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.EnumNaming;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

import com.subham.convergence.enums.PlatformType;
@Entity
@Table(name = "platform_connectivity", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_platform", columnList = "platform")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PlatformConnectivity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch= jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformType platform;

    @Column(nullable = false, length = 255)
    private String platformUsername;

    @Column(nullable = false)
    private String accessToken;

    @Column(nullable = false)
    
}
