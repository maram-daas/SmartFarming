package model.animals;

import model.enums.AnimalType;

public class Poultry extends Animal {
    private int eggCount;

    public Poultry(String id, String species, int age, double weight) {
        super(id, species, age, weight);
        this.eggCount = 0;
    }

    public int getEggCount() { return eggCount; }
    public void addEggs(int c) { this.eggCount += c; }
    @Override public AnimalType getAnimalType() { return AnimalType.POULTRY; }
}
