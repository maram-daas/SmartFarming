package model.animals;

import model.enums.AnimalType;
import model.entities.ProductionRecord;
import model.interfaces.Producing;

import java.time.LocalDateTime;

public class Ruminant extends Animal implements Producing {
    private final ProductionRecord productionRecord = new ProductionRecord("L");

    public Ruminant(String id, String species, int age, double weight) {
        super(id, species, age, weight);
    }

    @Override
    public double getProduction() {
        return productionRecord.getTotalProduction();
    }

    @Override
    public void recordProduction(double amount) {
        productionRecord.addProduction(amount);
    }

    @Override
    public void recordProduction(double amount, LocalDateTime recordedAt) {
        productionRecord.addProduction(amount, recordedAt);
    }

    @Override
    public ProductionRecord getProductionRecord() {
        return productionRecord;
    }

    public double getMilkYield() {
        return getProduction();
    }

    public void addMilkYield(double liters) {
        recordProduction(liters);
    }

    @Override
    public AnimalType getAnimalType() {
        return AnimalType.RUMINANT;
    }
}
