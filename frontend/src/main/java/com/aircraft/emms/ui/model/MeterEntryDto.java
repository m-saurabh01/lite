package com.aircraft.emms.ui.model;

import java.math.BigDecimal;

public class MeterEntryDto {
    private Long id;
    private Long flbId;
    private Long meterDefinitionId;
    private String meterName;
    private BigDecimal meterValue;
    private BigDecimal previousValue;
    private boolean mandatory;
    private String unitOfMeasure;
    private String validationMsg;

    public MeterEntryDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFlbId() { return flbId; }
    public void setFlbId(Long flbId) { this.flbId = flbId; }
    public Long getMeterDefinitionId() { return meterDefinitionId; }
    public void setMeterDefinitionId(Long meterDefinitionId) { this.meterDefinitionId = meterDefinitionId; }
    public String getMeterName() { return meterName; }
    public void setMeterName(String meterName) { this.meterName = meterName; }
    public BigDecimal getMeterValue() { return meterValue; }
    public void setMeterValue(BigDecimal meterValue) { this.meterValue = meterValue; }
    public BigDecimal getPreviousValue() { return previousValue; }
    public void setPreviousValue(BigDecimal previousValue) { this.previousValue = previousValue; }
    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
    public String getValidationMsg() { return validationMsg; }
    public void setValidationMsg(String validationMsg) { this.validationMsg = validationMsg; }
}
