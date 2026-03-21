package com.aircraft.emms.xmlimport;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class XmlImportResult {

    private String fileName;
    private int totalRecords;
    private int successCount;
    private int failureCount;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    private String xmlVersion;
    private boolean success;

    public void addError(String error) {
        errors.add(error);
    }
}
