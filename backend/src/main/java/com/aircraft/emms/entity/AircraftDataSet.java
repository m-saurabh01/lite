package com.aircraft.emms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "aircraft_data_sets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AircraftDataSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "asset_num", unique = true, nullable = false, length = 100)
    private String assetNum;

    @NotBlank
    @Column(name = "aircraft_name", nullable = false, length = 200)
    private String aircraftName;

    @NotBlank
    @Column(name = "aircraft_type", nullable = false, length = 100)
    private String aircraftType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = false;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (importedAt == null) importedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
