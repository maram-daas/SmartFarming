package model.sensors;

public class WaterSensor extends Sensor {
    private String measurementType;

    public WaterSensor(String code, String zoneCode, double min, double max, String type) {
        super(code, zoneCode, min, max);
        this.measurementType = type;
    }

    public String getMeasurementType() { return measurementType; }
    @Override public String getUnit() { return measurementType.equals("temperature") ? "Â°C" : "mg/L"; }
}
