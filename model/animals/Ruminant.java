package model.animals;

import model.enums.AnimalType;

public class Ruminant extends Animal {
    private double milkYield;

    public Ruminant(String id, String species, int age, double weight) {
        super(id, species, age, weight);
        this.milkYield = 0;
    }

    public double getMilkYield() { return milkYield; }
    public void addMilkYield(double l) { this.milkYield += l; }
    @Override public AnimalType getAnimalType() { return AnimalType.RUMINANT; }
}
