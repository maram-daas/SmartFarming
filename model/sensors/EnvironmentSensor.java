package model.sensors;

public class EnvironmentSensor extends Sensor {
    private String measurementType;

    public EnvironmentSensor(String code, String zoneCode, double min, double max, String type) {
        super(code, zoneCode, min, max);
        this.measurementType = type;
    }

    public String getMeasurementType() { return measurementType; }
    @Override public String getUnit() {
        switch(measurementType) {
            case "temperature": return "Â°C";
            case "humidity": return "%";
            case "rainfall": return "mm";
            default: return "unknown";
        }
    }
}
