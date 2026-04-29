package model.sensors;

public class BiometricSensor extends Sensor {
    private String animalId, measurementType;

    public BiometricSensor(String code, String zoneCode, double min, double max, String animalId, String type) {
        super(code, zoneCode, min, max);
        this.animalId = animalId;
        this.measurementType = type;
    }

    public String getAnimalId() { return animalId; }
    public String getMeasurementType() { return measurementType; }
    @Override public String getUnit() { return measurementType.equals("temperature") ? "Â°C" : "steps/min"; }
}
