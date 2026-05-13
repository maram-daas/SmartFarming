package model.entities;

import model.enums.SeverityLevel;
import java.time.LocalDateTime;

public class Alert {
    private String id;
    private String sensorCode;
    private double readingValue;
    private double thresholdMin;
    private double thresholdMax;
    private SeverityLevel severity;
    private LocalDateTime timestamp;
    private boolean acknowledged;
    private boolean dismissed;

    public Alert(String id, String sensorCode, double readingValue,
                 double thresholdMin, double thresholdMax,
                 SeverityLevel severity, LocalDateTime timestamp) {
        this.id = id;
        this.sensorCode = sensorCode;
        this.readingValue = readingValue;
        this.thresholdMin = thresholdMin;
        this.thresholdMax = thresholdMax;
        this.severity = severity;
        this.timestamp = timestamp;
        this.acknowledged = false;
        this.dismissed = false;
    }

    // Getters
    public String getId() { return id; }
    public String getSensorCode() { return sensorCode; }
    public double getReadingValue() { return readingValue; }
    public double getThresholdMin() { return thresholdMin; }
    public double getThresholdMax() { return thresholdMax; }
    public SeverityLevel getSeverity() { return severity; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isAcknowledged() { return acknowledged; }
    public boolean isDismissed() { return dismissed; }

    // Setters
    public void acknowledge() { this.acknowledged = true; }
    public void dismiss() { this.dismissed = true; }
    public void setSeverity(SeverityLevel severity) { this.severity = severity; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}