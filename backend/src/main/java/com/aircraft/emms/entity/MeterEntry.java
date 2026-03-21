package com.aircraft.emms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "meter_entries")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MeterEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flb_id", nullable = false)
    private FlightLogBook flightLogBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_definition_id")
    private MeterDefinition meterDefinition;

    @NotBlank
    @Column(name = "meter_name", nullable = false, length = 100)
    private String meterName;

    @NotNull
    @Column(name = "meter_value", nullable = false, precision = 15, scale = 4)
    private BigDecimal meterValue;

    @Column(name = "previous_value", precision = 15, scale = 4)
    private BigDecimal previousValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
