package com.aircraft.emms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MeterEntryDto {

    private Long id;
    private Long flbId;
    private Long meterDefinitionId;

    @NotBlank(message = "Meter name is required")
    private String meterName;

    @NotNull(message = "Meter value is required")
    private BigDecimal meterValue;

    private BigDecimal previousValue;
    private boolean mandatory;
    private String unitOfMeasure;
}
