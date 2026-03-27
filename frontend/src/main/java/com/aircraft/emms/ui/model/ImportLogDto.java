package com.aircraft.emms.ui.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ImportLogDto {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    private Long id;
    private String fileName;
    private String xmlVersion;
    private int recordsImported;
    private int recordsFailed;
    private String status;
    private String errorMessage;
    private LocalDateTime importedAt;

    public ImportLogDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getXmlVersion() { return xmlVersion; }
    public void setXmlVersion(String xmlVersion) { this.xmlVersion = xmlVersion; }
    public int getRecordsImported() { return recordsImported; }
    public void setRecordsImported(int recordsImported) { this.recordsImported = recordsImported; }
    public int getRecordsFailed() { return recordsFailed; }
    public void setRecordsFailed(int recordsFailed) { this.recordsFailed = recordsFailed; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }

    public String getFormattedImportedAt() {
        return importedAt != null ? importedAt.format(FMT) : "";
    }
}
