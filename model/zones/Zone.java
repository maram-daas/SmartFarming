package model.zones;

import model.enums.ZoneStatus;
import model.enums.CropFamily;
import model.enums.AnimalType;
import model.sensors.Sensor;
import java.util.ArrayList;
import java.util.List;

public abstract class Zone {
    protected String code;
    protected String name;
    protected ZoneStatus status;
    protected List<Sensor> sensors;
    protected double boundNorth;
    protected double boundSouth;
    protected double boundEast;
    protected double boundWest;
    protected CropFamily allowedCropFamily;
    protected AnimalType allowedAnimalType;

    public Zone(String code, String name) {
        this.code = code;
        this.name = name;
        this.status = ZoneStatus.ACTIVE;
        this.sensors = new ArrayList<>();
        this.boundNorth = 0;
        this.boundSouth = 0;
        this.boundEast = 0;
        this.boundWest = 0;
    }

    public Zone(String code, String name, double north, double south, double east, double west) {
        this(code, name);
        this.boundNorth = north;
        this.boundSouth = south;
        this.boundEast = east;
        this.boundWest = west;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ZoneStatus getStatus() { return status; }
    public List<Sensor> getSensors() { return sensors; }

    public double getBoundNorth() { return boundNorth; }
    public double getBoundSouth() { return boundSouth; }
    public double getBoundEast() { return boundEast; }
    public double getBoundWest() { return boundWest; }

    public void setBounds(double north, double south, double east, double west) {
        this.boundNorth = north;
        this.boundSouth = south;
        this.boundEast = east;
        this.boundWest = west;
    }

    public CropFamily getAllowedCropFamily() { return allowedCropFamily; }
    public void setAllowedCropFamily(CropFamily family) { this.allowedCropFamily = family; }

    public AnimalType getAllowedAnimalType() { return allowedAnimalType; }
    public void setAllowedAnimalType(AnimalType type) { this.allowedAnimalType = type; }

    public boolean isWithinBounds(double latitude, double longitude) {
        return latitude >= boundSouth && latitude <= boundNorth &&
               longitude >= boundWest && longitude <= boundEast;
    }

    public void suspend() {
        this.status = ZoneStatus.SUSPENDED;
        for (Sensor s : sensors) s.suspend();
    }

    public void activate() {
        this.status = ZoneStatus.ACTIVE;
        for (Sensor s : sensors) s.activate();
    }

    public void addSensor(Sensor sensor) {
        sensors.add(sensor);
        if (status == ZoneStatus.SUSPENDED) {
            sensor.suspend();
        }
    }
    public abstract int getEntityCount();
}