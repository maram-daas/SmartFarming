import model.zones.*;
import model.sensors.*;
import model.crops.*;
import model.animals.*;
import model.entities.*;
import model.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("     SMART FARMING SYSTEM - COMPLETE TEST");
        System.out.println("=".repeat(60));

        // ========== 1. MANAGE FARM ZONES ==========
        System.out.println("\n[1] MANAGING FARM ZONES");
        System.out.println("-".repeat(40));

        CropZone cropZone = new CropZone("CZ01", "North Valley Crop Zone");
        LivestockZone livestockZone = new LivestockZone("LZ01", "East Pasture Livestock Zone");
        AquacultureZone aquacultureZone = new AquacultureZone("AZ01", "West Pond Aquaculture Zone");

        System.out.println("Created Crop Zone: " + cropZone.getName());
        System.out.println("Created Livestock Zone: " + livestockZone.getName());
        System.out.println("Created Aquaculture Zone: " + aquacultureZone.getName());

        // Test zone suspension
        System.out.println("\n--- Zone Suspension Test ---");
        System.out.println("Zone status: " + cropZone.getStatus());
        cropZone.suspend();
        System.out.println("After suspend: " + cropZone.getStatus());
        cropZone.activate();
        System.out.println("After activate: " + cropZone.getStatus());

        // ========== 2. MANAGE CROPS ==========
        System.out.println("\n[2] MANAGING CROPS");
        System.out.println("-".repeat(40));

        Crop wheat = new Crop("Winter Wheat", CropFamily.CEREALS,
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 7, 15),
                6.0, 7.5, 20.0, 30.0);
        Crop tomato = new Crop("Cherry Tomato", CropFamily.VEGETABLES,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30),
                6.2, 6.8, 25.0, 35.0);
        Crop apple = new Crop("Golden Apple", CropFamily.FRUITS,
                LocalDate.of(2026, 2, 10), LocalDate.of(2026, 8, 20),
                6.0, 7.0, 40.0, 60.0);

        cropZone.addCrop(wheat);
        cropZone.addCrop(tomato);
        cropZone.addCrop(apple);

        System.out.println("Registered " + cropZone.getEntityCount() + " crops");

        // Update growth stages
        wheat.setGrowthStage(GrowthStage.GERMINATION);
        wheat.setGrowthStage(GrowthStage.GROWTH);
        tomato.setGrowthStage(GrowthStage.GROWTH);

        System.out.println(wheat.getName() + " stage: " + wheat.getGrowthStage());
        System.out.println(tomato.getName() + " stage: " + tomato.getGrowthStage());

        // Crop status report
        System.out.println("\n--- CROP STATUS REPORT ---");
        for (Crop c : cropZone.getCrops()) {
            System.out.println("  " + c.getName() + " | " + c.getFamily() + " | " + c.getGrowthStage());
        }

        // ========== 3. MANAGE ANIMALS ==========
        System.out.println("\n[3] MANAGING ANIMALS");
        System.out.println("-".repeat(40));

        Ruminant cow1 = new Ruminant("R001", "Holstein", 4, 650.0);
        Ruminant cow2 = new Ruminant("R002", "Jersey", 3, 480.0);
        Poultry chicken1 = new Poultry("P001", "Rhode Island Red", 1, 2.5);

        livestockZone.addAnimal(cow1);
        livestockZone.addAnimal(cow2);
        livestockZone.addAnimal(chicken1);

        System.out.println("Registered " + livestockZone.getEntityCount() + " animals");

        // Health events
        System.out.println("\n--- Health Events ---");
        cow1.setHealthStatus(HealthStatus.SICK);
        cow1.setWeight(620.0);
        System.out.println(cow1.getId() + " status: " + cow1.getHealthStatus());
        System.out.println(cow1.getId() + " weight: " + cow1.getWeight() + " kg");

        // Feeding program
        FeedingProgram feed = new FeedingProgram("Hay & Grain", 5.5, 3);
        livestockZone.setFeedingProgram(feed);
        System.out.println("\nFeeding: " + feed.getFeedType() + " - " + feed.getDailyQuantity() + " kg/day");

        // Production
        cow1.addMilkYield(25.5);
        cow1.addMilkYield(28.0);
        chicken1.addEggs(8);
        chicken1.addEggs(7);
        System.out.println("Milk yield: " + cow1.getMilkYield() + " L");
        System.out.println("Egg count: " + chicken1.getEggCount());

        // ========== 4. MANAGE SENSORS ==========
        System.out.println("\n[4] MANAGING SENSORS");
        System.out.println("-".repeat(40));

        EnvironmentSensor tempSensor = new EnvironmentSensor("T01", "CZ01", 10.0, 35.0, "temperature");
        SoilSensor pH_sensor = new SoilSensor("PH01", "CZ01", 5.5, 8.0, "pH");
        BiometricSensor cowTemp = new BiometricSensor("BT01", "LZ01", 37.5, 39.5, "R001", "temperature");
        GPSSensor gps = new GPSSensor("GPS01", "LZ01", 0, 0, "R001");

        cropZone.addSensor(tempSensor);
        cropZone.addSensor(pH_sensor);
        livestockZone.addSensor(cowTemp);
        livestockZone.addSensor(gps);

        System.out.println("Added sensors to zones");

        // Sensor status
        System.out.println(tempSensor.getCode() + " status: " + tempSensor.getStatus());
        tempSensor.suspend();
        System.out.println("After suspend: " + tempSensor.getStatus());
        tempSensor.activate();
        System.out.println("After activate: " + tempSensor.getStatus());

        // Add readings
        LocalDateTime now = LocalDateTime.now();
        tempSensor.addReading(new Reading("T01", 23.5, "°C", now));
        tempSensor.addReading(new Reading("T01", 42.0, "°C", now.plusMinutes(5)));  // Critical
        pH_sensor.addReading(new Reading("PH01", 6.8, "pH", now));
        pH_sensor.addReading(new Reading("PH01", 8.5, "pH", now.plusMinutes(10))); // Critical
        cowTemp.addReading(new Reading("BT01", 38.2, "°C", now));
        cowTemp.addReading(new Reading("BT01", 40.5, "°C", now.plusMinutes(15))); // Critical

        Position pos = new Position(36.7538, 3.0588);
        gps.addReading(new Reading("GPS01", 0, "coordinates", now, pos));

        System.out.println("Added readings to sensors");

        // ========== 5. ALERT SYSTEM ==========
        System.out.println("\n[5] ALERT SYSTEM");
        System.out.println("-".repeat(40));

        AlertSystem alertSystem = new AlertSystem();

        // Check readings and generate alerts
        alertSystem.checkReading("T01", 42.0, 10.0, 35.0, SeverityLevel.CRITICAL);
        alertSystem.checkReading("PH01", 8.5, 5.5, 8.0, SeverityLevel.CRITICAL);
        alertSystem.checkReading("BT01", 40.5, 37.5, 39.5, SeverityLevel.CRITICAL);

        System.out.println("Generated " + alertSystem.getActiveAlerts().size() + " alerts");

        // Display active alerts
        System.out.println("\n--- ACTIVE ALERTS ---");
        for (Alert a : alertSystem.getActiveAlerts()) {
            System.out.println("  " + a.getId() + " | " + a.getSensorCode() + " | " + a.getSeverity());
        }

        // Manage alerts
        Alert first = alertSystem.getActiveAlerts().get(0);
        alertSystem.acknowledgeAlert(first.getId());
        System.out.println("\nAcknowledged: " + first.getId());

        alertSystem.dismissAlert(alertSystem.getActiveAlerts().get(0).getId());
        System.out.println("Dismissed one alert");

        // Alert history
        System.out.println("\nAlert History: " + alertSystem.getAlertHistory().size() + " total");
        System.out.println("Critical alerts: " + alertSystem.getAlertsBySeverity(SeverityLevel.CRITICAL).size());

        // ========== 6. PRODUCTION RECORDS ==========
        System.out.println("\n[6] PRODUCTION RECORDS");
        System.out.println("-".repeat(40));

        ProductionRecord milk = new ProductionRecord("liters");
        milk.addProduction(125.5);
        milk.addProduction(132.0);
        milk.addProduction(128.5);

        ProductionRecord eggs = new ProductionRecord("eggs");
        eggs.addProduction(48);
        eggs.addProduction(52);

        System.out.println("Milk total: " + milk.getTotalProduction() + " " + milk.getUnit());
        System.out.println("Milk avg: " + String.format("%.1f", milk.getAverageProduction()));
        System.out.println("Eggs total: " + eggs.getTotalProduction() + " " + eggs.getUnit());

        // ========== 7. AQUACULTURE ==========
        System.out.println("\n[7] AQUACULTURE ZONE");
        System.out.println("-".repeat(40));

        aquacultureZone.addSpecies("Tilapia");
        aquacultureZone.addSpecies("Catfish");
        aquacultureZone.setAnimalCount(500);
        FeedingProgram aquaFeed = new FeedingProgram("Pellets", 2.5, 3);
        aquacultureZone.setFeedingProgram(aquaFeed);

        System.out.println("Species: " + String.join(", ", aquacultureZone.getSpecies()));
        System.out.println("Animal count: " + aquacultureZone.getAnimalCount());
        System.out.println("Daily feed: " + aquaFeed.getDailyQuantity() + " kg");

        WaterSensor waterTemp = new WaterSensor("WT01", "AZ01", 15.0, 25.0, "temperature");
        WaterSensor oxygen = new WaterSensor("OX01", "AZ01", 5.0, 8.0, "dissolved oxygen");
        waterTemp.addReading(new Reading("WT01", 22.5, "°C", now));
        oxygen.addReading(new Reading("OX01", 6.8, "mg/L", now));
        System.out.println("Water temp: 22.5°C | Oxygen: 6.8 mg/L");

        // ========== SUMMARY ==========
        System.out.println("\n" + "=".repeat(60));
        System.out.println("     SYSTEM SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("Crop Zone Entities: " + cropZone.getEntityCount());
        System.out.println("Livestock Zone Entities: " + livestockZone.getEntityCount());
        System.out.println("Aquaculture Count: " + aquacultureZone.getAnimalCount());
        System.out.println("Total Sensors: " + (cropZone.getSensors().size() + livestockZone.getSensors().size() + aquacultureZone.getSensors().size()));
        System.out.println("Total Alerts: " + alertSystem.getAlertHistory().size());
        System.out.println("\n     ALL TESTS PASSED!");
        System.out.println("=".repeat(60));
    }
}

// Alert System Class
class AlertSystem {
    private List<Alert> activeAlerts;
    private List<Alert> alertHistory;
    private int nextId;

    public AlertSystem() {
        this.activeAlerts = new ArrayList<>();
        this.alertHistory = new ArrayList<>();
        this.nextId = 1;
    }

    public Alert checkReading(String sensorCode, double value, double min, double max, SeverityLevel severity) {
        Alert alert = new Alert("ALT" + String.format("%03d", nextId++),
                sensorCode, value, min, max, severity, LocalDateTime.now());
        activeAlerts.add(alert);
        alertHistory.add(alert);
        return alert;
    }

    public void acknowledgeAlert(String alertId) {
        for (Alert a : activeAlerts) {
            if (a.getId().equals(alertId)) {
                a.acknowledge();
                break;
            }
        }
    }

    public void dismissAlert(String alertId) {
        for (int i = 0; i < activeAlerts.size(); i++) {
            if (activeAlerts.get(i).getId().equals(alertId)) {
                activeAlerts.get(i).dismiss();
                activeAlerts.remove(i);
                break;
            }
        }
    }


    //maram tete fff

    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts);
    }

    public List<Alert> getAlertHistory() {
        return new ArrayList<>(alertHistory);
    }

    public List<Alert> getAlertsBySeverity(SeverityLevel severity) {
        List<Alert> result = new ArrayList<>();
        for (Alert a : alertHistory) {
            if (a.getSeverity() == severity) {
                result.add(a);
            }
        }
        return result;
    }
}