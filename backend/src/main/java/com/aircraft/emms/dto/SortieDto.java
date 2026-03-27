package com.aircraft.emms.dto;

import com.aircraft.emms.entity.SortieStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SortieDto {

    private Long id;

    private String sortieNumber;

    private String aircraftType;

    private String aircraftNumber;

    private Long captainId;
    private String captainName;

    private Long pilotId;
    private String pilotName;

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;

    private LocalTime scheduledStart;
    private LocalTime scheduledEnd;

    private SortieStatus status;
    private String remarks;

    private boolean hasClash;
    private String clashDetails;
}
