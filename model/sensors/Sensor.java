package model.sensors;

import model.entities.Reading;
import model.enums.SensorStatus;
import java.util.ArrayList;
import java.util.List;

public abstract class Sensor {
    protected String code;
    protected String zoneCode;
    protected SensorStatus status;
    protected double thresholdMin;
    protected double thresholdMax;
    protected List<Reading> readings;

    public Sensor(String code, String zoneCode, double thresholdMin, double thresholdMax) {
        this.code = code;
        this.zoneCode = zoneCode;
        this.thresholdMin = thresholdMin;
        this.thresholdMax = thresholdMax;
        this.status = SensorStatus.ACTIVE;
        this.readings = new ArrayList<>();
    }

    // Getters
    public String getCode() { return code; }
    public String getZoneCode() { return zoneCode; }
    public SensorStatus getStatus() { return status; }
    public double getThresholdMin() { return thresholdMin; }
    public double getThresholdMax() { return thresholdMax; }
    public List<Reading> getReadings() { return readings; }

    // Setters
    public void setStatus(SensorStatus status) { this.status = status; }
    public void suspend() { this.status = SensorStatus.SUSPENDED; }
    public void activate() { this.status = SensorStatus.ACTIVE; }
    public void setThresholdMin(double min) { this.thresholdMin = min; }
    public void setThresholdMax(double max) { this.thresholdMax = max; }

    public void addReading(Reading reading) {
        if (status == SensorStatus.ACTIVE) {
            readings.add(reading);
        }
    }

    public abstract String getUnit();
}