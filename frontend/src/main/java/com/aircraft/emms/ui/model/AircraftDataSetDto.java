package com.aircraft.emms.ui.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AircraftDataSetDto {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    private Long id;
    private String assetNum;
    private String aircraftName;
    private String aircraftType;
    private boolean active;
    private String importedAt;

    public AircraftDataSetDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAssetNum() { return assetNum; }
    public void setAssetNum(String assetNum) { this.assetNum = assetNum; }
    public String getAircraftName() { return aircraftName; }
    public void setAircraftName(String aircraftName) { this.aircraftName = aircraftName; }
    public String getAircraftType() { return aircraftType; }
    public void setAircraftType(String aircraftType) { this.aircraftType = aircraftType; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getImportedAt() { return importedAt; }
    public void setImportedAt(String importedAt) { this.importedAt = importedAt; }

    public String getFormattedImportedAt() {
        if (importedAt == null || importedAt.isEmpty()) return "";
        try {
            LocalDateTime dt = LocalDateTime.parse(importedAt);
            return dt.format(FMT);
        } catch (Exception e) {
            return importedAt;
        }
    }

    @Override
    public String toString() {
        return assetNum + " - " + aircraftName + (active ? " [ACTIVE]" : "");
    }
}
