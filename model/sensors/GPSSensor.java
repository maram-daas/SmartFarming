package model.sensors;

import model.entities.Position;

public class GPSSensor extends Sensor {
    private String animalId;
    private Position lastPosition;

    public GPSSensor(String code, String zoneCode, double min, double max, String animalId) {
        super(code, zoneCode, min, max);
        this.animalId = animalId;
    }

    public String getAnimalId() { return animalId; }
    public Position getLastPosition() { return lastPosition; }
    public void setLastPosition(Position p) { this.lastPosition = p; }
    @Override public String getUnit() { return "coordinates"; }
}
