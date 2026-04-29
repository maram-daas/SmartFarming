package model.entities;

import model.enums.SeverityLevel;
import java.time.LocalDateTime;

public class Alert {
    private String id;
    private String sensorCode;
    private double readingValue, thresholdMin, thresholdMax;
    private SeverityLevel severity;
    private LocalDateTime timestamp;
    private boolean acknowledged, dismissed;

    public Alert(String id, String sensorCode, double readingValue, double thresholdMin, double thresholdMax, SeverityLevel severity, LocalDateTime timestamp) {
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

    public String getId() { return id; }
    public String getSensorCode() { return sensorCode; }
    public double getReadingValue() { return readingValue; }
    public SeverityLevel getSeverity() { return severity; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isAcknowledged() { return acknowledged; }
    public boolean isDismissed() { return dismissed; }
    public void acknowledge() { this.acknowledged = true; }
    public void dismiss() { this.dismissed = true; }
}
