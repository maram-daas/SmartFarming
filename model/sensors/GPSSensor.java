package model.sensors;

import model.entities.Position;
import model.zones.Zone;

public class GPSSensor extends Sensor {
    private String animalId;
    private Position lastPosition;
    private Zone assignedZone;

    public GPSSensor(String code, String zoneCode, double min, double max, String animalId) {
        super(code, zoneCode, min, max);
        this.animalId = animalId;
    }

    public GPSSensor(String code, String zoneCode, double min, double max, String animalId, Zone zone) {
        super(code, zoneCode, min, max);
        this.animalId = animalId;
        this.assignedZone = zone;
    }

    public String getAnimalId() { return animalId; }
    public Position getLastPosition() { return lastPosition; }
    public void setLastPosition(Position position) {
        this.lastPosition = position;
    }

    public void setAssignedZone(Zone zone) { this.assignedZone = zone; }

    public boolean isWithinZoneBounds() {
        if (assignedZone == null || lastPosition == null) return true;
        return assignedZone.isWithinBounds(lastPosition.getLatitude(), lastPosition.getLongitude());
    }

    @Override
    public String getUnit() { return "coordinates"; }
}