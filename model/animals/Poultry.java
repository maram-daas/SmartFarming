package model.animals;

import model.enums.AnimalType;
import model.entities.ProductionRecord;
import model.interfaces.Producing;

import java.time.LocalDateTime;

public class Poultry extends Animal implements Producing {
    private final ProductionRecord productionRecord = new ProductionRecord("eggs");

    public Poultry(String id, String species, int age, double weight) {
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

    public int getEggCount() {
        return (int) getProduction();
    }

    public void addEggs(int count) {
        recordProduction(count);
    }

    @Override
    public AnimalType getAnimalType() {
        return AnimalType.POULTRY;
    }
}
