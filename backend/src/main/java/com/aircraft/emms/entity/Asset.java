package com.aircraft.emms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "assets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "asset_num", nullable = false, length = 100)
    private String assetNum;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "parent_asset_num", length = 100)
    private String parentAssetNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private AircraftDataSet dataset;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
