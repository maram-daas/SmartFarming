package model.sensors;

public class EnvironmentSensor extends Sensor {
    private String measurementType;

    public EnvironmentSensor(String code, String zoneCode, double min, double max, String measurementType) {
        super(code, zoneCode, min, max);
        this.measurementType = measurementType;
    }

    public String getMeasurementType() { return measurementType; }

    @Override
    public String getUnit() {
        switch(measurementType.toLowerCase()) {
            case "temperature": return "°C";
            case "humidity": return "%";
            case "rainfall": return "mm";
            default: return "unknown";
        }
    }
}