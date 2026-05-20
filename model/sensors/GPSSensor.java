package model.sensors;

import model.entities.Position;
import model.zones.Zone;

public class GPSSensor extends Sensor {
    private String animalId;
    private Position lastPosition;

    public GPSSensor(String code, String zoneCode, double min, double max, String animalId) {
        super(code, zoneCode, min, max);
        this.animalId = animalId;
    }

    public String getAnimalId() { return animalId; }
    public Position getLastPosition() { return lastPosition; }
    public void setLastPosition(Position position) { this.lastPosition = position; }

    public BoundsCheckResult checkAnimalInBounds(Zone zone) {
        if (lastPosition == null || zone.getCenter() == null) {
            return new BoundsCheckResult(animalId, zone.getBoundingRadius(), 0.0, true, "Position or zone center not set");
        }
        
        double distance = haversineDistance(lastPosition.getLatitude(), lastPosition.getLongitude(),
                                           zone.getCenter().getLatitude(), zone.getCenter().getLongitude());
        boolean inside = distance <= zone.getBoundingRadius();
        String message = inside 
            ? "Animal is within zone boundaries."
            : "Animal has strayed " + String.format("%.1f", distance - zone.getBoundingRadius()) + "m outside the zone!";
        
        return new BoundsCheckResult(animalId, zone.getBoundingRadius(), distance, inside, message);
    }
    
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Earth's radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public String getUnit() { return "coordinates"; }
    
    public static class BoundsCheckResult {
        private final String animalId;
        private final double zoneRadius;
        private final double distanceFromCenter;
        private final boolean inside;
        private final String message;
        
        public BoundsCheckResult(String animalId, double zoneRadius, double distanceFromCenter, boolean inside, String message) {
            this.animalId = animalId;
            this.zoneRadius = zoneRadius;
            this.distanceFromCenter = distanceFromCenter;
            this.inside = inside;
            this.message = message;
        }
        
        public String getAnimalId() { return animalId; }
        public double getZoneRadius() { return zoneRadius; }
        public double getDistanceFromCenter() { return distanceFromCenter; }
        public boolean isInside() { return inside; }
        public String getMessage() { return message; }
    }
}