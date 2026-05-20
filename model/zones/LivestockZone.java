package model.zones;

import model.animals.Animal;
import model.entities.FeedingProgram;
import model.enums.AnimalType;
import java.util.ArrayList;
import java.util.List;

public class LivestockZone extends Zone {
    private List<Animal> animals;
    private FeedingProgram feedingProgram;

    public LivestockZone(String code, String name) {
        super(code, name);
        this.animals = new ArrayList<>();
    }

    public LivestockZone(String code, String name, double north, double south, double east, double west, AnimalType allowedType) {
        super(code, name, north, south, east, west);
        this.animals = new ArrayList<>();
        this.setAllowedAnimalType(allowedType);
    }

    public void addAnimal(Animal animal) {
        if (getAllowedAnimalType() == null || animal.getAnimalType() == getAllowedAnimalType()) {
            animals.add(animal);
        } else {
            throw new IllegalArgumentException("This zone only allows " + getAllowedAnimalType() + " animals");
        }
    }

    public void setFeedingProgram(FeedingProgram fp) { this.feedingProgram = fp; }
    public List<Animal> getAnimals() { return animals; }
    public FeedingProgram getFeedingProgram() { return feedingProgram; }
    @Override public int getEntityCount() { return animals.size(); }
}