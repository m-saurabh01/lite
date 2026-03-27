package com.aircraft.emms.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ImportLogDto {
    private Long id;
    private String fileName;
    private String xmlVersion;
    private int recordsImported;
    private int recordsFailed;
    private String status;
    private String errorMessage;
    private LocalDateTime importedAt;
}
