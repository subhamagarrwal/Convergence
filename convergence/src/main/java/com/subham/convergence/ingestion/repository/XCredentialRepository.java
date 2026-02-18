package com.subham.convergence.ingestion.repository;

import com.subham.convergence.ingestion.common.XCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface XCredentialRepository extends JpaRepository<XCredential, Long> {
    Optional<XCredential> findByUserId(String userId);
}