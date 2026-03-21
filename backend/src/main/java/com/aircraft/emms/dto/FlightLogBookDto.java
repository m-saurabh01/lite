package com.aircraft.emms.dto;

import com.aircraft.emms.entity.FlbStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FlightLogBookDto {

    private Long id;
    private Long sortieId;
    private String sortieNumber;

    @NotBlank(message = "Aircraft type is required")
    private String aircraftType;

    @NotBlank(message = "Aircraft number is required")
    private String aircraftNumber;

    private Long pilotId;
    private String pilotName;

    private LocalTime actualTakeoffTime;
    private LocalTime actualLandingTime;
    private Long durationMinutes;

    private String remarks;
    private FlbStatus status;

    private List<MeterEntryDto> meterEntries;
}
