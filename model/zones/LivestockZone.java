package model.zones;

import model.animals.Animal;
import model.animals.Poultry;
import model.animals.Ruminant;
import model.entities.FeedingProgram;
import model.enums.AnimalType;
import java.util.ArrayList;
import java.util.List;

public class LivestockZone extends Zone {
    private List<Animal> animals;
    private FeedingProgram feedingProgram;
    private AnimalType animalType; // Enforces single type per zone

    public LivestockZone(String code, String name) {
        super(code, name);
        this.animals = new ArrayList<>();
        this.animalType = null;
    }

    public AnimalType getAnimalType() { return animalType; }
    
    public void addAnimal(Animal animal) { 
        AnimalType newType = animal.getAnimalType();
        
        if (animalType == null) {
            // First animal in zone - set the type
            animalType = newType;
        } else if (!animalType.equals(newType)) {
            // Type mismatch
            throw new IllegalArgumentException(
                "This zone is designated for " + animalType + " only. Cannot add " + newType);
        }
        
        animals.add(animal);
    }
    
    public void setFeedingProgram(FeedingProgram fp) { this.feedingProgram = fp; }
    public List<Animal> getAnimals() { return animals; }
    public FeedingProgram getFeedingProgram() { return feedingProgram; }
    @Override public int getEntityCount() { return animals.size(); }
}
