package com.aircraft.emms.ui.model;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public class FlightLogBookDto {
    private Long id;
    private Long sortieId;
    private String sortieNumber;
    private String aircraftType;
    private String aircraftNumber;
    private Long pilotId;
    private String pilotName;
    private LocalTime actualTakeoffTime;
    private LocalTime actualLandingTime;
    private Long durationMinutes;
    private String remarks;
    private FlbStatus status;
    private List<MeterEntryDto> meterEntries;

    public FlightLogBookDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSortieId() { return sortieId; }
    public void setSortieId(Long sortieId) { this.sortieId = sortieId; }
    public String getSortieNumber() { return sortieNumber; }
    public void setSortieNumber(String sortieNumber) { this.sortieNumber = sortieNumber; }
    public String getAircraftType() { return aircraftType; }
    public void setAircraftType(String aircraftType) { this.aircraftType = aircraftType; }
    public String getAircraftNumber() { return aircraftNumber; }
    public void setAircraftNumber(String aircraftNumber) { this.aircraftNumber = aircraftNumber; }
    public Long getPilotId() { return pilotId; }
    public void setPilotId(Long pilotId) { this.pilotId = pilotId; }
    public String getPilotName() { return pilotName; }
    public void setPilotName(String pilotName) { this.pilotName = pilotName; }
    public LocalTime getActualTakeoffTime() { return actualTakeoffTime; }
    public void setActualTakeoffTime(LocalTime actualTakeoffTime) { this.actualTakeoffTime = actualTakeoffTime; }
    public LocalTime getActualLandingTime() { return actualLandingTime; }
    public void setActualLandingTime(LocalTime actualLandingTime) { this.actualLandingTime = actualLandingTime; }
    public Long getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Long durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public FlbStatus getStatus() { return status; }
    public void setStatus(FlbStatus status) { this.status = status; }
    public List<MeterEntryDto> getMeterEntries() { return meterEntries; }
    public void setMeterEntries(List<MeterEntryDto> meterEntries) { this.meterEntries = meterEntries; }
}
