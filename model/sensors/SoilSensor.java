package model.sensors;

public class SoilSensor extends Sensor {
    private String measurementType;

    public SoilSensor(String code, String zoneCode, double min, double max, String measurementType) {
        super(code, zoneCode, min, max);
        this.measurementType = measurementType;
    }

    public String getMeasurementType() { return measurementType; }

    @Override
    public String getUnit() {
        switch(measurementType.toLowerCase()) {
            case "ph": return "pH";
            case "moisture": return "%";
            case "nitrogen": return "mg/kg";
            default: return "unknown";
        }
    }
}