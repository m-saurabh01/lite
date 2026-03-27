package com.aircraft.emms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "snags")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Snag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "asset_num", nullable = false, length = 100)
    private String assetNum;

    @NotBlank
    @Column(nullable = false, length = 2000)
    private String description;

    @NotBlank
    @Column(name = "reported_by", nullable = false, length = 50)
    private String reportedBy;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private AircraftDataSet dataset;

    @Column(nullable = false, length = 20)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private SnagStatus status = SnagStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
