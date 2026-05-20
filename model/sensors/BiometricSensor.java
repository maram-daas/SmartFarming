package model.sensors;

public class BiometricSensor extends Sensor {
    private String animalId;
    private String measurementType;

    public BiometricSensor(String code, String zoneCode, double min, double max, String animalId, String measurementType) {
        super(code, zoneCode, min, max);
        this.animalId = animalId;
        this.measurementType = measurementType;
    }

    public String getAnimalId() { return animalId; }
    public String getMeasurementType() { return measurementType; }

    @Override
    public String getUnit() {
        return measurementType.equalsIgnoreCase("temperature") ? "°C" : "steps/min";
    }
}