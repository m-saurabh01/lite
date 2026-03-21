package com.aircraft.emms.ui.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class SortieDto {
    private Long id;
    private String sortieNumber;
    private String aircraftType;
    private String aircraftNumber;
    private Long captainId;
    private String captainName;
    private Long pilotId;
    private String pilotName;
    private LocalDate scheduledDate;
    private LocalTime scheduledStart;
    private LocalTime scheduledEnd;
    private SortieStatus status;
    private String remarks;
    private boolean hasClash;
    private String clashDetails;

    public SortieDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSortieNumber() { return sortieNumber; }
    public void setSortieNumber(String sortieNumber) { this.sortieNumber = sortieNumber; }
    public String getAircraftType() { return aircraftType; }
    public void setAircraftType(String aircraftType) { this.aircraftType = aircraftType; }
    public String getAircraftNumber() { return aircraftNumber; }
    public void setAircraftNumber(String aircraftNumber) { this.aircraftNumber = aircraftNumber; }
    public Long getCaptainId() { return captainId; }
    public void setCaptainId(Long captainId) { this.captainId = captainId; }
    public String getCaptainName() { return captainName; }
    public void setCaptainName(String captainName) { this.captainName = captainName; }
    public Long getPilotId() { return pilotId; }
    public void setPilotId(Long pilotId) { this.pilotId = pilotId; }
    public String getPilotName() { return pilotName; }
    public void setPilotName(String pilotName) { this.pilotName = pilotName; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public LocalTime getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(LocalTime scheduledStart) { this.scheduledStart = scheduledStart; }
    public LocalTime getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(LocalTime scheduledEnd) { this.scheduledEnd = scheduledEnd; }
    public SortieStatus getStatus() { return status; }
    public void setStatus(SortieStatus status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public boolean isHasClash() { return hasClash; }
    public void setHasClash(boolean hasClash) { this.hasClash = hasClash; }
    public String getClashDetails() { return clashDetails; }
    public void setClashDetails(String clashDetails) { this.clashDetails = clashDetails; }
}
