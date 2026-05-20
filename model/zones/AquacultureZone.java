package model.zones;

import model.entities.FeedingProgram;
import java.util.ArrayList;
import java.util.List;

public class AquacultureZone extends Zone {
    private List<String> species;
    private int animalCount;
    private FeedingProgram feedingProgram;

    public AquacultureZone(String code, String name) {
        super(code, name);
        this.species = new ArrayList<>();
        this.animalCount = 0;
    }

    public void addSpecies(String s) { species.add(s); }
    public void setAnimalCount(int c) { this.animalCount = c; }
    public void setFeedingProgram(FeedingProgram fp) { this.feedingProgram = fp; }
    public List<String> getSpecies() { return species; }
    public int getAnimalCount() { return animalCount; }
    public FeedingProgram getFeedingProgram() { return feedingProgram; }
    @Override public int getEntityCount() { return animalCount; }
}
