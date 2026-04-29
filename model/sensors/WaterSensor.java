package model.sensors;

public class WaterSensor extends Sensor {
    private String measurementType;

    public WaterSensor(String code, String zoneCode, double min, double max, String measurementType) {
        super(code, zoneCode, min, max);
        this.measurementType = measurementType;
    }

    public String getMeasurementType() { return measurementType; }

    @Override
    public String getUnit() {
        return measurementType.equalsIgnoreCase("temperature") ? "°C" : "mg/L";
    }
}