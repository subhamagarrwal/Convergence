package com.subham.convergence.ingestion.common;

import com.subham.convergence.enums.PlatformType;

import jakarta.persistence.Id;

import com.google.api.client.util.DateTime;

import jakarta.persistence.Enumerated;

@Entity
@Table(name = "ingestion_states")
public class IngestionState {
    @Id
    @Enumerated(EnumType.STRING)
    private PlatformType provider;

    private DateTime lastIngestionTime;
}
