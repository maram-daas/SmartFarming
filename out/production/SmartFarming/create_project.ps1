# create_project.ps1 - Run this in your SmartFarming/src/ folder

$folders = @(
    "model\enums","model\interfaces","model\entities","model\zones",
    "model\sensors","model\crops","model\animals"
)
foreach ($folder in $folders) { New-Item -ItemType Directory -Force -Path $folder }

# Enums
@"
package model.enums;

public enum ZoneStatus { ACTIVE, SUSPENDED }
"@ | Out-File -FilePath "model\enums\ZoneStatus.java" -Encoding UTF8

@"
package model.enums;

public enum SensorStatus { ACTIVE, FAULTY, SUSPENDED }
"@ | Out-File -FilePath "model\enums\SensorStatus.java" -Encoding UTF8

@"
package model.enums;

public enum SeverityLevel { WARNING, CRITICAL }
"@ | Out-File -FilePath "model\enums\SeverityLevel.java" -Encoding UTF8

@"
package model.enums;

public enum GrowthStage { SOWING, GERMINATION, GROWTH, MATURITY, HARVEST }
"@ | Out-File -FilePath "model\enums\GrowthStage.java" -Encoding UTF8

@"
package model.enums;

public enum HealthStatus { HEALTHY, SICK, QUARANTINED }
"@ | Out-File -FilePath "model\enums\HealthStatus.java" -Encoding UTF8

@"
package model.enums;

public enum CropFamily { CEREALS, VEGETABLES, FRUITS }
"@ | Out-File -FilePath "model\enums\CropFamily.java" -Encoding UTF8

@"
package model.enums;

public enum AnimalType { RUMINANT, POULTRY }
"@ | Out-File -FilePath "model\enums\AnimalType.java" -Encoding UTF8

# Interface
@"
package model.interfaces;

public interface Producing {
    double getProduction();
    void recordProduction(double amount);
}
"@ | Out-File -FilePath "model\interfaces\Producing.java" -Encoding UTF8

# Entities
@"
package model.entities;

public class Position {
    private double latitude, longitude;
    public Position(double latitude, double longitude) { this.latitude = latitude; this.longitude = longitude; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
"@ | Out-File -FilePath "model\entities\Position.java" -Encoding UTF8

@"
package model.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Reading {
    private String sensorCode;
    private double value;
    private String unit;
    private LocalDateTime timestamp;
    private Position position;

    public Reading(String sensorCode, double value, String unit, LocalDateTime timestamp) {
        this.sensorCode = sensorCode;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
    }

    public Reading(String sensorCode, double value, String unit, LocalDateTime timestamp, Position position) {
        this(sensorCode, value, unit, timestamp);
        this.position = position;
    }

    public String getSensorCode() { return sensorCode; }
    public double getValue() { return value; }
    public String getUnit() { return unit; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Position getPosition() { return position; }
    public boolean hasPosition() { return position != null; }
}
"@ | Out-File -FilePath "model\entities\Reading.java" -Encoding UTF8

@"
package model.entities;

import model.enums.SeverityLevel;
import java.time.LocalDateTime;

public class Alert {
    private String id;
    private String sensorCode;
    private double readingValue, thresholdMin, thresholdMax;
    private SeverityLevel severity;
    private LocalDateTime timestamp;
    private boolean acknowledged, dismissed;

    public Alert(String id, String sensorCode, double readingValue, double thresholdMin, double thresholdMax, SeverityLevel severity, LocalDateTime timestamp) {
        this.id = id;
        this.sensorCode = sensorCode;
        this.readingValue = readingValue;
        this.thresholdMin = thresholdMin;
        this.thresholdMax = thresholdMax;
        this.severity = severity;
        this.timestamp = timestamp;
        this.acknowledged = false;
        this.dismissed = false;
    }

    public String getId() { return id; }
    public String getSensorCode() { return sensorCode; }
    public double getReadingValue() { return readingValue; }
    public SeverityLevel getSeverity() { return severity; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isAcknowledged() { return acknowledged; }
    public boolean isDismissed() { return dismissed; }
    public void acknowledge() { this.acknowledged = true; }
    public void dismiss() { this.dismissed = true; }
}
"@ | Out-File -FilePath "model\entities\Alert.java" -Encoding UTF8

@"
package model.entities;

public class FeedingProgram {
    private String feedType;
    private double quantityPerMeal;
    private int mealsPerDay;

    public FeedingProgram(String feedType, double quantityPerMeal, int mealsPerDay) {
        this.feedType = feedType;
        this.quantityPerMeal = quantityPerMeal;
        this.mealsPerDay = mealsPerDay;
    }

    public String getFeedType() { return feedType; }
    public double getQuantityPerMeal() { return quantityPerMeal; }
    public int getMealsPerDay() { return mealsPerDay; }
    public double getDailyQuantity() { return quantityPerMeal * mealsPerDay; }
}
"@ | Out-File -FilePath "model\entities\FeedingProgram.java" -Encoding UTF8

@"
package model.entities;

import java.util.ArrayList;
import java.util.List;

public class ProductionRecord {
    private List<Double> productions;
    private String unit;

    public ProductionRecord(String unit) {
        this.productions = new ArrayList<>();
        this.unit = unit;
    }

    public void addProduction(double amount) { productions.add(amount); }
    public double getTotalProduction() { return productions.stream().mapToDouble(Double::doubleValue).sum(); }
    public double getAverageProduction() { return productions.isEmpty() ? 0 : getTotalProduction() / productions.size(); }
    public List<Double> getProductions() { return productions; }
    public String getUnit() { return unit; }
}
"@ | Out-File -FilePath "model\entities\ProductionRecord.java" -Encoding UTF8

# Zones
@"
package model.zones;

import model.enums.ZoneStatus;
import model.sensors.Sensor;
import java.util.ArrayList;
import java.util.List;

public abstract class Zone {
    protected String code, name;
    protected ZoneStatus status;
    protected List<Sensor> sensors;

    public Zone(String code, String name) {
        this.code = code;
        this.name = name;
        this.status = ZoneStatus.ACTIVE;
        this.sensors = new ArrayList<>();
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public ZoneStatus getStatus() { return status; }
    public List<Sensor> getSensors() { return sensors; }

    public void suspend() {
        this.status = ZoneStatus.SUSPENDED;
        for (Sensor s : sensors) s.suspend();
    }

    public void activate() {
        this.status = ZoneStatus.ACTIVE;
        for (Sensor s : sensors) s.activate();
    }

    public void addSensor(Sensor sensor) { sensors.add(sensor); }
    public abstract int getEntityCount();
}
"@ | Out-File -FilePath "model\zones\Zone.java" -Encoding UTF8

@"
package model.zones;

import model.crops.Crop;
import java.util.ArrayList;
import java.util.List;

public class CropZone extends Zone {
    private List<Crop> crops;

    public CropZone(String code, String name) {
        super(code, name);
        this.crops = new ArrayList<>();
    }

    public void addCrop(Crop crop) { crops.add(crop); }
    public List<Crop> getCrops() { return crops; }
    @Override public int getEntityCount() { return crops.size(); }
}
"@ | Out-File -FilePath "model\zones\CropZone.java" -Encoding UTF8

@"
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
"@ | Out-File -FilePath "model\zones\LivestockZone.java" -Encoding UTF8

@"
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
"@ | Out-File -FilePath "model\zones\AquacultureZone.java" -Encoding UTF8

# Sensors
@"
package model.sensors;

import model.entities.Reading;
import model.enums.SensorStatus;
import java.util.ArrayList;
import java.util.List;

public abstract class Sensor {
    protected String code, zoneCode;
    protected SensorStatus status;
    protected double thresholdMin, thresholdMax;
    protected List<Reading> readings;

    public Sensor(String code, String zoneCode, double min, double max) {
        this.code = code;
        this.zoneCode = zoneCode;
        this.thresholdMin = min;
        this.thresholdMax = max;
        this.status = SensorStatus.ACTIVE;
        this.readings = new ArrayList<>();
    }

    public String getCode() { return code; }
    public String getZoneCode() { return zoneCode; }
    public SensorStatus getStatus() { return status; }
    public void setStatus(SensorStatus s) { this.status = s; }
    public void suspend() { this.status = SensorStatus.SUSPENDED; }
    public void activate() { this.status = SensorStatus.ACTIVE; }
    public void addReading(Reading r) { if (status == SensorStatus.ACTIVE) readings.add(r); }
    public abstract String getUnit();
}
"@ | Out-File -FilePath "model\sensors\Sensor.java" -Encoding UTF8

@"
package model.sensors;

public class EnvironmentSensor extends Sensor {
    private String measurementType;

    public EnvironmentSensor(String code, String zoneCode, double min, double max, String type) {
        super(code, zoneCode, min, max);
        this.measurementType = type;
    }

    public String getMeasurementType() { return measurementType; }
    @Override public String getUnit() {
        switch(measurementType) {
            case "temperature": return "°C";
            case "humidity": return "%";
            case "rainfall": return "mm";
            default: return "unknown";
        }
    }
}
"@ | Out-File -FilePath "model\sensors\EnvironmentSensor.java" -Encoding UTF8

@"
package model.sensors;

public class SoilSensor extends Sensor {
    private String measurementType;

    public SoilSensor(String code, String zoneCode, double min, double max, String type) {
        super(code, zoneCode, min, max);
        this.measurementType = type;
    }

    public String getMeasurementType() { return measurementType; }
    @Override public String getUnit() {
        switch(measurementType) {
            case "pH": return "pH";
            case "moisture": return "%";
            case "nitrogen": return "mg/kg";
            default: return "unknown";
        }
    }
}
"@ | Out-File -FilePath "model\sensors\SoilSensor.java" -Encoding UTF8

@"
package model.sensors;

public class BiometricSensor extends Sensor {
    private String animalId, measurementType;

    public BiometricSensor(String code, String zoneCode, double min, double max, String animalId, String type) {
        super(code, zoneCode, min, max);
        this.animalId = animalId;
        this.measurementType = type;
    }

    public String getAnimalId() { return animalId; }
    public String getMeasurementType() { return measurementType; }
    @Override public String getUnit() { return measurementType.equals("temperature") ? "°C" : "steps/min"; }
}
"@ | Out-File -FilePath "model\sensors\BiometricSensor.java" -Encoding UTF8

@"
package model.sensors;

import model.entities.Position;

public class GPSSensor extends Sensor {
    private String animalId;
    private Position lastPosition;

    public GPSSensor(String code, String zoneCode, double min, double max, String animalId) {
        super(code, zoneCode, min, max);
        this.animalId = animalId;
    }

    public String getAnimalId() { return animalId; }
    public Position getLastPosition() { return lastPosition; }
    public void setLastPosition(Position p) { this.lastPosition = p; }
    @Override public String getUnit() { return "coordinates"; }
}
"@ | Out-File -FilePath "model\sensors\GPSSensor.java" -Encoding UTF8

@"
package model.sensors;

public class WaterSensor extends Sensor {
    private String measurementType;

    public WaterSensor(String code, String zoneCode, double min, double max, String type) {
        super(code, zoneCode, min, max);
        this.measurementType = type;
    }

    public String getMeasurementType() { return measurementType; }
    @Override public String getUnit() { return measurementType.equals("temperature") ? "°C" : "mg/L"; }
}
"@ | Out-File -FilePath "model\sensors\WaterSensor.java" -Encoding UTF8

# Crops
@"
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
"@ | Out-File -FilePath "model\crops\Crop.java" -Encoding UTF8

# Animals
@"
package model.animals;

import model.enums.AnimalType;
import model.enums.HealthStatus;
import java.util.ArrayList;
import java.util.List;

public abstract class Animal {
    protected String id, species;
    protected int age;
    protected double weight;
    protected HealthStatus healthStatus;
    protected List<String> healthEvents;

    public Animal(String id, String species, int age, double weight) {
        this.id = id;
        this.species = species;
        this.age = age;
        this.weight = weight;
        this.healthStatus = HealthStatus.HEALTHY;
        this.healthEvents = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getSpecies() { return species; }
    public int getAge() { return age; }
    public double getWeight() { return weight; }
    public void setWeight(double w) { this.weight = w; logHealthEvent("Weight: " + w + " kg"); }
    public HealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthStatus hs) { this.healthStatus = hs; logHealthEvent("Status: " + hs); }
    public void logHealthEvent(String e) { healthEvents.add(e); }
    public List<String> getHealthEvents() { return healthEvents; }
    public abstract AnimalType getAnimalType();
}
"@ | Out-File -FilePath "model\animals\Animal.java" -Encoding UTF8

@"
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
"@ | Out-File -FilePath "model\animals\Ruminant.java" -Encoding UTF8

@"
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
"@ | Out-File -FilePath "model\animals\Poultry.java" -Encoding UTF8

# Main
@"
import model.zones.*;
import model.crops.*;
import model.animals.*;
import model.sensors.*;
import model.enums.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Smart Farming System ===\n");

        CropZone cropZone = new CropZone("CZ001", "North Field");
        Crop wheat = new Crop("Wheat", CropFamily.CEREALS, LocalDate.now(), LocalDate.now().plusMonths(4), 6.0, 7.5, 20.0, 30.0);
        cropZone.addCrop(wheat);

        LivestockZone livestockZone = new LivestockZone("LZ001", "South Pasture");
        Ruminant cow = new Ruminant("COW001", "Holstein", 3, 650);
        livestockZone.addAnimal(cow);

        EnvironmentSensor tempSensor = new EnvironmentSensor("SENS001", "CZ001", 15.0, 35.0, "temperature");
        cropZone.addSensor(tempSensor);

        System.out.println("✓ Crop Zone: " + cropZone.getName() + " | Entities: " + cropZone.getEntityCount());
        System.out.println("✓ Livestock Zone: " + livestockZone.getName() + " | Entities: " + livestockZone.getEntityCount());
        System.out.println("✓ Sensor: " + tempSensor.getCode() + " | Type: " + tempSensor.getMeasurementType() + " | Unit: " + tempSensor.getUnit());
        System.out.println("\nSystem ready! All classes loaded successfully.");
    }
}
"@ | Out-File -FilePath "Main.java" -Encoding UTF8

Write-Host "Project created successfully!" -ForegroundColor Green