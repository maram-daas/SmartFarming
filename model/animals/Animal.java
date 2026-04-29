package model.animals;

import model.enums.AnimalType;
import model.enums.HealthStatus;
import java.util.ArrayList;
import java.util.List;

public abstract class Animal {
    protected String id, species;
    protected int age;
    protected double weight;
    protected HealthStatus healthStatus;
    protected List<String> healthEvents;

    public Animal(String id, String species, int age, double weight) {
        this.id = id;
        this.species = species;
        this.age = age;
        this.weight = weight;
        this.healthStatus = HealthStatus.HEALTHY;
        this.healthEvents = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getSpecies() { return species; }
    public int getAge() { return age; }
    public double getWeight() { return weight; }
    public void setWeight(double w) { this.weight = w; logHealthEvent("Weight: " + w + " kg"); }
    public HealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthStatus hs) { this.healthStatus = hs; logHealthEvent("Status: " + hs); }
    public void logHealthEvent(String e) { healthEvents.add(e); }
    public List<String> getHealthEvents() { return healthEvents; }
    public abstract AnimalType getAnimalType();
}
