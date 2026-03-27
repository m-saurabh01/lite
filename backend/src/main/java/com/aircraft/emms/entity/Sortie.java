package com.aircraft.emms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "sorties")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Sortie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "sortie_number", unique = true, nullable = false, length = 30)
    private String sortieNumber;

    @NotBlank
    @Column(name = "aircraft_type", nullable = false, length = 50)
    private String aircraftType;

    @NotBlank
    @Column(name = "aircraft_number", nullable = false, length = 50)
    private String aircraftNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captain_id")
    private User captain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pilot_id")
    private User pilot;

    @NotNull
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_start")
    private LocalTime scheduledStart;

    @Column(name = "scheduled_end")
    private LocalTime scheduledEnd;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SortieStatus status = SortieStatus.CREATED;

    @Column(length = 500)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private AircraftDataSet dataset;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
