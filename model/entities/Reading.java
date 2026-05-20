package model.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Reading {
    private String sensorCode;
    private double value;
    private String unit;
    private LocalDateTime timestamp;
    private Position position;

    public Reading(String sensorCode, double value, String unit, LocalDateTime timestamp) {
        this.sensorCode = sensorCode;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
    }

    public Reading(String sensorCode, double value, String unit, LocalDateTime timestamp, Position position) {
        this(sensorCode, value, unit, timestamp);
        this.position = position;
    }

    public String getSensorCode() { return sensorCode; }
    public double getValue() { return value; }
    public String getUnit() { return unit; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Position getPosition() { return position; }
    public boolean hasPosition() { return position != null; }
}
