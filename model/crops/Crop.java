package model.crops;

import model.enums.CropFamily;
import model.enums.GrowthStage;
import java.time.LocalDate;

public class Crop {
    private String name;
    private CropFamily family;
    private LocalDate plantingDate;
    private LocalDate expectedHarvestDate;
    private GrowthStage growthStage;
    private double optimalPHMin;
    private double optimalPHMax;
    private double optimalMoistureMin;
    private double optimalMoistureMax;

    public Crop(String name, CropFamily family, LocalDate plantingDate, LocalDate expectedHarvestDate,
                double optimalPHMin, double optimalPHMax, double optimalMoistureMin, double optimalMoistureMax) {
        this.name = name;
        this.family = family;
        this.plantingDate = plantingDate;
        this.expectedHarvestDate = expectedHarvestDate;
        this.optimalPHMin = optimalPHMin;
        this.optimalPHMax = optimalPHMax;
        this.optimalMoistureMin = optimalMoistureMin;
        this.optimalMoistureMax = optimalMoistureMax;
        this.growthStage = GrowthStage.SOWING;
    }

    // Getters
    public String getName() { return name; }
    public CropFamily getFamily() { return family; }
    public LocalDate getPlantingDate() { return plantingDate; }
    public LocalDate getExpectedHarvestDate() { return expectedHarvestDate; }
    public GrowthStage getGrowthStage() { return growthStage; }
    public double getOptimalPHMin() { return optimalPHMin; }
    public double getOptimalPHMax() { return optimalPHMax; }
    public double getOptimalMoistureMin() { return optimalMoistureMin; }
    public double getOptimalMoistureMax() { return optimalMoistureMax; }

    // Setters
    public void setGrowthStage(GrowthStage growthStage) { this.growthStage = growthStage; }
    public void setName(String name) { this.name = name; }
    public void setFamily(CropFamily family) { this.family = family; }
    public void setPlantingDate(LocalDate plantingDate) { this.plantingDate = plantingDate; }
    public void setExpectedHarvestDate(LocalDate expectedHarvestDate) { this.expectedHarvestDate = expectedHarvestDate; }
    public void setOptimalPHMin(double optimalPHMin) { this.optimalPHMin = optimalPHMin; }
    public void setOptimalPHMax(double optimalPHMax) { this.optimalPHMax = optimalPHMax; }
    public void setOptimalMoistureMin(double optimalMoistureMin) { this.optimalMoistureMin = optimalMoistureMin; }
    public void setOptimalMoistureMax(double optimalMoistureMax) { this.optimalMoistureMax = optimalMoistureMax; }
}