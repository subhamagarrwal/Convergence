//storing the last ingestion timestamp for each platform
package com.subham.convergence.ingestion.common;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.subham.convergence.enums.PlatformType;

@Entity
@Table(name = "timestamp_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestionState {
    @Id
    @Enumerated(EnumType.STRING)
    private PlatformType platform;

    private String lastProcessedId;

    private long lastProcessedTimestamp;
}