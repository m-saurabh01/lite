package com.aircraft.emms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "meter_definitions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MeterDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "meter_name", nullable = false, length = 100)
    private String meterName;

    @Column(name = "aircraft_type", length = 50)
    private String aircraftType;

    @Column(nullable = false)
    @Builder.Default
    private boolean mandatory = false;

    @Column(name = "display_order")
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "unit_of_measure", length = 30)
    private String unitOfMeasure;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
