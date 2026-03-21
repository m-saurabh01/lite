package com.aircraft.emms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flight_log_books")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FlightLogBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sortie_id")
    private Sortie sortie;

    @NotBlank
    @Column(name = "aircraft_type", nullable = false, length = 50)
    private String aircraftType;

    @NotBlank
    @Column(name = "aircraft_number", nullable = false, length = 50)
    private String aircraftNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pilot_id", nullable = false)
    private User pilot;

    @Column(name = "actual_takeoff_time")
    private LocalTime actualTakeoffTime;

    @Column(name = "actual_landing_time")
    private LocalTime actualLandingTime;

    @Column(name = "duration_minutes")
    private Long durationMinutes;

    @Column(length = 500)
    private String remarks;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FlbStatus status = FlbStatus.DRAFT;

    @OneToMany(mappedBy = "flightLogBook", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MeterEntry> meterEntries = new ArrayList<>();

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

    /** Auto-calculate duration from actual times. */
    public void calculateDuration() {
        if (actualTakeoffTime != null && actualLandingTime != null) {
            long minutes = Duration.between(actualTakeoffTime, actualLandingTime).toMinutes();
            this.durationMinutes = minutes >= 0 ? minutes : minutes + 1440; // handle overnight
        }
    }

    public void addMeterEntry(MeterEntry entry) {
        meterEntries.add(entry);
        entry.setFlightLogBook(this);
    }
}
