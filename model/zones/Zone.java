package model.zones;

import model.enums.ZoneStatus;
import model.sensors.Sensor;
import java.util.ArrayList;
import java.util.List;

public abstract class Zone {
    protected String code, name;
    protected ZoneStatus status;
    protected List<Sensor> sensors;

    public Zone(String code, String name) {
        this.code = code;
        this.name = name;
        this.status = ZoneStatus.ACTIVE;
        this.sensors = new ArrayList<>();
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public ZoneStatus getStatus() { return status; }
    public List<Sensor> getSensors() { return sensors; }

    public void suspend() {
        this.status = ZoneStatus.SUSPENDED;
        for (Sensor s : sensors) s.suspend();
    }

    public void activate() {
        this.status = ZoneStatus.ACTIVE;
        for (Sensor s : sensors) s.activate();
    }

    public void addSensor(Sensor sensor) { sensors.add(sensor); }
    public abstract int getEntityCount();
}
