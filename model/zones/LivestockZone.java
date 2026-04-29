package model.zones;

import model.animals.Animal;
import model.entities.FeedingProgram;
import java.util.ArrayList;
import java.util.List;

public class LivestockZone extends Zone {
    private List<Animal> animals;
    private FeedingProgram feedingProgram;

    public LivestockZone(String code, String name) {
        super(code, name);
        this.animals = new ArrayList<>();
    }

    public void addAnimal(Animal animal) { animals.add(animal); }
    public void setFeedingProgram(FeedingProgram fp) { this.feedingProgram = fp; }
    public List<Animal> getAnimals() { return animals; }
    public FeedingProgram getFeedingProgram() { return feedingProgram; }
    @Override public int getEntityCount() { return animals.size(); }
}
