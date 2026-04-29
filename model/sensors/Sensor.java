package model.sensors;

import model.entities.Reading;
import model.enums.SensorStatus;
import java.util.ArrayList;
import java.util.List;

public abstract class Sensor {
    protected String code, zoneCode;
    protected SensorStatus status;
    protected double thresholdMin, thresholdMax;
    protected List<Reading> readings;

    public Sensor(String code, String zoneCode, double min, double max) {
        this.code = code;
        this.zoneCode = zoneCode;
        this.thresholdMin = min;
        this.thresholdMax = max;
        this.status = SensorStatus.ACTIVE;
        this.readings = new ArrayList<>();
    }

    public String getCode() { return code; }
    public String getZoneCode() { return zoneCode; }
    public SensorStatus getStatus() { return status; }
    public void setStatus(SensorStatus s) { this.status = s; }
    public void suspend() { this.status = SensorStatus.SUSPENDED; }
    public void activate() { this.status = SensorStatus.ACTIVE; }
    public void addReading(Reading r) { if (status == SensorStatus.ACTIVE) readings.add(r); }
    public abstract String getUnit();
}
