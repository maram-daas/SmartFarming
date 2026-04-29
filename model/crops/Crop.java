package model.crops;

import model.enums.CropFamily;
import model.enums.GrowthStage;
import java.time.LocalDate;

public class Crop {
    private String name;
    private CropFamily family;
    private LocalDate plantingDate, expectedHarvestDate;
    private GrowthStage growthStage;
    private double optimalPHMin, optimalPHMax, optimalMoistureMin, optimalMoistureMax;

    public Crop(String name, CropFamily family, LocalDate plant, LocalDate harvest, double pHMin, double pHMax, double mMin, double mMax) {
        this.name = name;
        this.family = family;
        this.plantingDate = plant;
        this.expectedHarvestDate = harvest;
        this.optimalPHMin = pHMin;
        this.optimalPHMax = pHMax;
        this.optimalMoistureMin = mMin;
        this.optimalMoistureMax = mMax;
        this.growthStage = GrowthStage.SOWING;
    }

    public String getName() { return name; }
    public CropFamily getFamily() { return family; }
    public GrowthStage getGrowthStage() { return growthStage; }
    public void setGrowthStage(GrowthStage g) { this.growthStage = g; }
    public LocalDate getPlantingDate() { return plantingDate; }
    public LocalDate getExpectedHarvestDate() { return expectedHarvestDate; }
}
