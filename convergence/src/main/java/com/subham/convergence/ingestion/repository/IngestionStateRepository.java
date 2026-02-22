package com.subham.convergence.ingestion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.ingestion.common.IngestionState;

@Repository
public interface IngestionStateRepository extends JpaRepository<IngestionState, PlatformType> {
    
}
