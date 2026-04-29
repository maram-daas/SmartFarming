import model.zones.*;
import model.sensors.*;
import model.crops.*;
import model.animals.*;
import model.entities.*;
import model.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    private static Scanner scanner = new Scanner(System.in);
    private static List<CropZone> cropZones = new ArrayList<>();
    private static List<LivestockZone> livestockZones = new ArrayList<>();
    private static List<AquacultureZone> aquacultureZones = new ArrayList<>();
    private static AlertSystem alertSystem = new AlertSystem();
    private static int sensorCounter = 100;
    private static int animalCounter = 1000;

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("     SMART FARMING SYSTEM");
        System.out.println("     Interactive Management Console");
        System.out.println("=".repeat(60));

        // Add sample data
        addSampleData();

        while (true) {
            showMainMenu();
            int choice = getIntInput("Enter your choice: ");

            switch (choice) {
                case 1: manageZones(); break;
                case 2: manageCrops(); break;
                case 3: manageAnimals(); break;
                case 4: manageSensors(); break;
                case 5: manageAlerts(); break;
                case 6: viewReports(); break;
                case 7: recordProduction(); break;
                case 0:
                    System.out.println("\nExiting Smart Farming System. Goodbye!");
                    System.exit(0);
                    break;
                default: System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    private static void showMainMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("MAIN MENU");
        System.out.println("=".repeat(50));
        System.out.println("1. Manage Zones");
        System.out.println("2. Manage Crops");
        System.out.println("3. Manage Animals");
        System.out.println("4. Manage Sensors");
        System.out.println("5. Manage Alerts");
        System.out.println("6. View Reports");
        System.out.println("7. Record Production");
        System.out.println("0. Exit");
        System.out.println("-".repeat(50));
    }

    // ==================== ZONE MANAGEMENT ====================
    private static void manageZones() {
        while (true) {
            System.out.println("\n--- ZONE MANAGEMENT ---");
            System.out.println("1. Add Crop Zone");
            System.out.println("2. Add Livestock Zone");
            System.out.println("3. Add Aquaculture Zone");
            System.out.println("4. View All Zones");
            System.out.println("5. Suspend/Reactivate Zone");
            System.out.println("6. Back to Main Menu");

            int choice = getIntInput("Choice: ");

            switch (choice) {
                case 1: addCropZone(); break;
                case 2: addLivestockZone(); break;
                case 3: addAquacultureZone(); break;
                case 4: viewAllZones(); break;
                case 5: toggleZoneStatus(); break;
                case 6: return;
                default: System.out.println("Invalid choice!");
            }
        }
    }

    private static void addCropZone() {
        String code = getStringInput("Enter zone code (e.g., CZ001): ");
        String name = getStringInput("Enter zone name: ");
        CropZone zone = new CropZone(code, name);
        cropZones.add(zone);
        System.out.println("✓ Crop zone '" + name + "' created successfully!");
    }

    private static void addLivestockZone() {
        String code = getStringInput("Enter zone code (e.g., LZ001): ");
        String name = getStringInput("Enter zone name: ");
        LivestockZone zone = new LivestockZone(code, name);

        // Add feeding program
        System.out.println("\n--- Set up feeding program ---");
        String feedType = getStringInput("Feed type: ");
        double quantity = getDoubleInput("Quantity per meal (kg): ");
        int meals = getIntInput("Meals per day: ");
        zone.setFeedingProgram(new FeedingProgram(feedType, quantity, meals));

        livestockZones.add(zone);
        System.out.println("✓ Livestock zone '" + name + "' created successfully!");
    }

    private static void addAquacultureZone() {
        String code = getStringInput("Enter zone code (e.g., AZ001): ");
        String name = getStringInput("Enter zone name: ");
        AquacultureZone zone = new AquacultureZone(code, name);

        int numSpecies = getIntInput("How many species in this tank? ");
        for (int i = 0; i < numSpecies; i++) {
            String species = getStringInput("Enter species name: ");
            zone.addSpecies(species);
        }

        int count = getIntInput("Number of animals: ");
        zone.setAnimalCount(count);

        String feedType = getStringInput("Feed type: ");
        double quantity = getDoubleInput("Quantity per meal (kg): ");
        int meals = getIntInput("Meals per day: ");
        zone.setFeedingProgram(new FeedingProgram(feedType, quantity, meals));

        aquacultureZones.add(zone);
        System.out.println("✓ Aquaculture zone '" + name + "' created successfully!");
    }

    private static void viewAllZones() {
        System.out.println("\n" + "=".repeat(70));
        System.out.printf("%-15s %-20s %-12s %-15s%n", "Type", "Code", "Status", "Entities");
        System.out.println("-".repeat(70));

        for (CropZone z : cropZones) {
            System.out.printf("%-15s %-20s %-12s %-15d%n", "CROP", z.getCode(), z.getStatus(), z.getEntityCount());
        }
        for (LivestockZone z : livestockZones) {
            System.out.printf("%-15s %-20s %-12s %-15d%n", "LIVESTOCK", z.getCode(), z.getStatus(), z.getEntityCount());
        }
        for (AquacultureZone z : aquacultureZones) {
            System.out.printf("%-15s %-20s %-12s %-15d%n", "AQUACULTURE", z.getCode(), z.getStatus(), z.getEntityCount());
        }
        System.out.println("=".repeat(70));
    }

    private static void toggleZoneStatus() {
        System.out.println("\n--- Select Zone Type ---");
        System.out.println("1. Crop Zone");
        System.out.println("2. Livestock Zone");
        System.out.println("3. Aquaculture Zone");
        int type = getIntInput("Choice: ");

        String code = getStringInput("Enter zone code: ");
        Zone zone = findZoneByCode(code, type);

        if (zone == null) {
            System.out.println("Zone not found!");
            return;
        }

        System.out.println("Current status: " + zone.getStatus());
        System.out.println("1. Suspend");
        System.out.println("2. Activate");
        int action = getIntInput("Choice: ");

        if (action == 1) {
            zone.suspend();
            System.out.println("Zone suspended. All sensors deactivated.");
        } else if (action == 2) {
            zone.activate();
            System.out.println("Zone activated. All sensors restored.");
        }
    }

    // ==================== CROP MANAGEMENT ====================
    private static void manageCrops() {
        if (cropZones.isEmpty()) {
            System.out.println("No crop zones available! Create one first.");
            return;
        }

        System.out.println("\n--- Select Crop Zone ---");
        for (int i = 0; i < cropZones.size(); i++) {
            System.out.println((i+1) + ". " + cropZones.get(i).getName() + " (" + cropZones.get(i).getCode() + ")");
        }
        int idx = getIntInput("Choice: ") - 1;
        if (idx < 0 || idx >= cropZones.size()) return;
        CropZone zone = cropZones.get(idx);

        while (true) {
            System.out.println("\n--- CROP MANAGEMENT in " + zone.getName() + " ---");
            System.out.println("1. Add New Crop");
            System.out.println("2. View All Crops");
            System.out.println("3. Update Growth Stage");
            System.out.println("4. Generate Crop Status Report");
            System.out.println("5. Back");

            int choice = getIntInput("Choice: ");

            switch (choice) {
                case 1: addCrop(zone); break;
                case 2: viewCrops(zone); break;
                case 3: updateGrowthStage(zone); break;
                case 4: generateCropReport(zone); break;
                case 5: return;
            }
        }
    }

    private static void addCrop(CropZone zone) {
        String name = getStringInput("Crop name: ");

        System.out.println("Crop Family:");
        CropFamily[] families = CropFamily.values();
        for (int i = 0; i < families.length; i++) {
            System.out.println((i+1) + ". " + families[i]);
        }
        int familyIdx = getIntInput("Choice: ") - 1;
        CropFamily family = families[familyIdx];

        LocalDate plantDate = getDateInput("Planting date (YYYY-MM-DD): ");
        LocalDate harvestDate = getDateInput("Expected harvest date (YYYY-MM-DD): ");

        double pHMin = getDoubleInput("Optimal pH minimum: ");
        double pHMax = getDoubleInput("Optimal pH maximum: ");
        double moistMin = getDoubleInput("Optimal moisture minimum (%): ");
        double moistMax = getDoubleInput("Optimal moisture maximum (%): ");

        Crop crop = new Crop(name, family, plantDate, harvestDate, pHMin, pHMax, moistMin, moistMax);
        zone.addCrop(crop);
        System.out.println("✓ Crop '" + name + "' added successfully!");
    }

    private static void viewCrops(CropZone zone) {
        if (zone.getCrops().isEmpty()) {
            System.out.println("No crops in this zone.");
            return;
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("%-20s %-12s %-12s %-15s %-15s%n", "Name", "Family", "Stage", "Planting Date", "Harvest Date");
        System.out.println("-".repeat(80));
        for (Crop c : zone.getCrops()) {
            System.out.printf("%-20s %-12s %-12s %-15s %-15s%n",
                    c.getName(), c.getFamily(), c.getGrowthStage(),
                    c.getPlantingDate(), c.getExpectedHarvestDate());
        }
    }

    private static void updateGrowthStage(CropZone zone) {
        if (zone.getCrops().isEmpty()) {
            System.out.println("No crops in this zone.");
            return;
        }

        System.out.println("\n--- Select Crop ---");
        for (int i = 0; i < zone.getCrops().size(); i++) {
            System.out.println((i+1) + ". " + zone.getCrops().get(i).getName() + " (Current: " + zone.getCrops().get(i).getGrowthStage() + ")");
        }
        int idx = getIntInput("Choice: ") - 1;
        if (idx < 0 || idx >= zone.getCrops().size()) return;
        Crop crop = zone.getCrops().get(idx);

        System.out.println("\nSelect new growth stage:");
        GrowthStage[] stages = GrowthStage.values();
        for (int i = 0; i < stages.length; i++) {
            System.out.println((i+1) + ". " + stages[i]);
        }
        int stageIdx = getIntInput("Choice: ") - 1;
        crop.setGrowthStage(stages[stageIdx]);
        System.out.println("✓ Growth stage updated to " + stages[stageIdx]);
    }

    private static void generateCropReport(CropZone zone) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CROP STATUS REPORT - " + zone.getName());
        System.out.println("=".repeat(60));

        for (Crop c : zone.getCrops()) {
            System.out.println("\n▶ " + c.getName() + " (" + c.getFamily() + ")");
            System.out.println("  Growth Stage: " + c.getGrowthStage());
            System.out.println("  Days to harvest: " + getDaysBetween(c.getExpectedHarvestDate(), LocalDate.now()));
            System.out.println("  Soil Requirements:");
            System.out.println("    - pH: " + c.getOptimalPHMin() + " - " + c.getOptimalPHMax());
            System.out.println("    - Moisture: " + c.getOptimalMoistureMin() + "% - " + c.getOptimalMoistureMax() + "%");
        }
    }

    // ==================== ANIMAL MANAGEMENT ====================
    private static void manageAnimals() {
        if (livestockZones.isEmpty()) {
            System.out.println("No livestock zones available! Create one first.");
            return;
        }

        System.out.println("\n--- Select Livestock Zone ---");
        for (int i = 0; i < livestockZones.size(); i++) {
            System.out.println((i+1) + ". " + livestockZones.get(i).getName() + " (" + livestockZones.get(i).getCode() + ")");
        }
        int idx = getIntInput("Choice: ") - 1;
        if (idx < 0 || idx >= livestockZones.size()) return;
        LivestockZone zone = livestockZones.get(idx);

        while (true) {
            System.out.println("\n--- ANIMAL MANAGEMENT in " + zone.getName() + " ---");
            System.out.println("1. Add Ruminant (Cow, Sheep, Goat)");
            System.out.println("2. Add Poultry (Chicken, Turkey)");
            System.out.println("3. View All Animals");
            System.out.println("4. Log Health Event");
            System.out.println("5. View Feeding Schedule");
            System.out.println("6. Update Feeding Program");
            System.out.println("7. Back");

            int choice = getIntInput("Choice: ");

            switch (choice) {
                case 1: addRuminant(zone); break;
                case 2: addPoultry(zone); break;
                case 3: viewAnimals(zone); break;
                case 4: logHealthEvent(zone); break;
                case 5: viewFeedingSchedule(zone); break;
                case 6: updateFeedingProgram(zone); break;
                case 7: return;
            }
        }
    }

    private static void addRuminant(LivestockZone zone) {
        String id = "R" + (animalCounter++);
        String species = getStringInput("Species (Cow/Sheep/Goat): ");
        int age = getIntInput("Age (years): ");
        double weight = getDoubleInput("Weight (kg): ");

        Ruminant animal = new Ruminant(id, species, age, weight);
        zone.addAnimal(animal);
        System.out.println("✓ Ruminant added! ID: " + id);
    }

    private static void addPoultry(LivestockZone zone) {
        String id = "P" + (animalCounter++);
        String species = getStringInput("Species (Chicken/Turkey): ");
        int age = getIntInput("Age (years): ");
        double weight = getDoubleInput("Weight (kg): ");

        Poultry animal = new Poultry(id, species, age, weight);
        zone.addAnimal(animal);
        System.out.println("✓ Poultry added! ID: " + id);
    }

    private static void viewAnimals(LivestockZone zone) {
        if (zone.getAnimals().isEmpty()) {
            System.out.println("No animals in this zone.");
            return;
        }

        System.out.println("\n" + "=".repeat(90));
        System.out.printf("%-10s %-15s %-10s %-10s %-15s %-10s%n", "ID", "Species", "Type", "Age", "Health", "Weight(kg)");
        System.out.println("-".repeat(90));
        for (Animal a : zone.getAnimals()) {
            System.out.printf("%-10s %-15s %-10s %-10d %-15s %-10.1f%n",
                    a.getId(), a.getSpecies(), a.getAnimalType(), a.getAge(), a.getHealthStatus(), a.getWeight());
        }
    }

    private static void logHealthEvent(LivestockZone zone) {
        if (zone.getAnimals().isEmpty()) return;

        System.out.println("\n--- Select Animal ---");
        for (int i = 0; i < zone.getAnimals().size(); i++) {
            Animal a = zone.getAnimals().get(i);
            System.out.println((i+1) + ". " + a.getId() + " - " + a.getSpecies() + " (" + a.getHealthStatus() + ")");
        }
        int idx = getIntInput("Choice: ") - 1;
        if (idx < 0 || idx >= zone.getAnimals().size()) return;
        Animal animal = zone.getAnimals().get(idx);

        System.out.println("1. Change Health Status");
        System.out.println("2. Update Weight");
        int action = getIntInput("Choice: ");

        if (action == 1) {
            System.out.println("Health Status:");
            HealthStatus[] statuses = HealthStatus.values();
            for (int i = 0; i < statuses.length; i++) {
                System.out.println((i+1) + ". " + statuses[i]);
            }
            int statusIdx = getIntInput("Choice: ") - 1;
            animal.setHealthStatus(statuses[statusIdx]);
            System.out.println("✓ Health status updated to " + statuses[statusIdx]);
        } else if (action == 2) {
            double newWeight = getDoubleInput("New weight (kg): ");
            animal.setWeight(newWeight);
            System.out.println("✓ Weight updated to " + newWeight + " kg");
        }
    }

    private static void viewFeedingSchedule(LivestockZone zone) {
        if (zone.getFeedingProgram() == null) {
            System.out.println("No feeding program set.");
            return;
        }
        FeedingProgram fp = zone.getFeedingProgram();
        System.out.println("\n--- FEEDING SCHEDULE for " + zone.getName() + " ---");
        System.out.println("Feed Type: " + fp.getFeedType());
        System.out.println("Quantity per meal: " + fp.getQuantityPerMeal() + " kg");
        System.out.println("Meals per day: " + fp.getMealsPerDay());
        System.out.println("Total daily feed: " + fp.getDailyQuantity() + " kg");
    }

    private static void updateFeedingProgram(LivestockZone zone) {
        String feedType = getStringInput("New feed type: ");
        double quantity = getDoubleInput("Quantity per meal (kg): ");
        int meals = getIntInput("Meals per day: ");
        zone.setFeedingProgram(new FeedingProgram(feedType, quantity, meals));
        System.out.println("✓ Feeding program updated!");
    }

    // ==================== SENSOR MANAGEMENT ====================
    private static void manageSensors() {
        System.out.println("\n--- Select Zone Type for Sensor ---");
        System.out.println("1. Crop Zone");
        System.out.println("2. Livestock Zone");
        System.out.println("3. Aquaculture Zone");
        int type = getIntInput("Choice: ");

        Zone zone = selectZone(type);
        if (zone == null) return;

        while (true) {
            System.out.println("\n--- SENSOR MANAGEMENT in " + zone.getName() + " ---");
            System.out.println("1. Add Sensor");
            System.out.println("2. View All Sensors");
            System.out.println("3. Add Reading");
            System.out.println("4. View Readings Dashboard");
            System.out.println("5. Change Sensor Status");
            System.out.println("6. Back");

            int choice = getIntInput("Choice: ");

            switch (choice) {
                case 1: addSensor(zone, type); break;
                case 2: viewSensors(zone); break;
                case 3: addReading(zone); break;
                case 4: viewReadingsDashboard(zone); break;
                case 5: changeSensorStatus(zone); break;
                case 6: return;
            }
        }
    }

    private static Zone selectZone(int zoneType) {
        if (zoneType == 1 && cropZones.isEmpty()) {
            System.out.println("No crop zones available!");
            return null;
        } else if (zoneType == 2 && livestockZones.isEmpty()) {
            System.out.println("No livestock zones available!");
            return null;
        } else if (zoneType == 3 && aquacultureZones.isEmpty()) {
            System.out.println("No aquaculture zones available!");
            return null;
        }

        List<? extends Zone> zones;
        if (zoneType == 1) zones = cropZones;
        else if (zoneType == 2) zones = livestockZones;
        else zones = aquacultureZones;

        System.out.println("\n--- Select Zone ---");
        for (int i = 0; i < zones.size(); i++) {
            System.out.println((i+1) + ". " + zones.get(i).getName());
        }
        int idx = getIntInput("Choice: ") - 1;
        if (idx < 0 || idx >= zones.size()) return null;

        return zones.get(idx);
    }

    private static void addSensor(Zone zone, int zoneType) {
        String code = "SENS" + (sensorCounter++);

        System.out.println("Sensor Type:");
        if (zoneType == 1) {
            System.out.println("1. Environment Sensor (temp/humidity/rainfall)");
            System.out.println("2. Soil Sensor (pH/moisture/nitrogen)");
        } else if (zoneType == 2) {
            System.out.println("1. Biometric Sensor (temperature/activity)");
            System.out.println("2. GPS Collar Sensor");
        } else {
            System.out.println("1. Water Sensor (temperature/dissolved oxygen)");
        }

        int sensorType = getIntInput("Choice: ");
        String measurement = getStringInput("Measurement type: ");
        double min = getDoubleInput("Threshold minimum: ");
        double max = getDoubleInput("Threshold maximum: ");

        Sensor sensor = null;
        String animalId = null;

        if (zoneType == 1) {
            if (sensorType == 1) sensor = new EnvironmentSensor(code, zone.getCode(), min, max, measurement);
            else sensor = new SoilSensor(code, zone.getCode(), min, max, measurement);
        } else if (zoneType == 2) {
            if (sensorType == 2) {
                animalId = getStringInput("Animal ID for GPS collar: ");
                sensor = new GPSSensor(code, zone.getCode(), min, max, animalId);
            } else {
                animalId = getStringInput("Animal ID: ");
                sensor = new BiometricSensor(code, zone.getCode(), min, max, animalId, measurement);
            }
        } else {
            sensor = new WaterSensor(code, zone.getCode(), min, max, measurement);
        }

        zone.addSensor(sensor);
        System.out.println("✓ Sensor added! Code: " + code);
    }

    private static void viewSensors(Zone zone) {
        if (zone.getSensors().isEmpty()) {
            System.out.println("No sensors in this zone.");
            return;
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("%-12s %-20s %-12s %-12s%n", "Code", "Type", "Status", "Threshold");
        System.out.println("-".repeat(80));
        for (Sensor s : zone.getSensors()) {
            String type = s.getClass().getSimpleName();
            System.out.printf("%-12s %-20s %-12s [%.1f - %.1f]%n",
                    s.getCode(), type, s.getStatus(), s.getThresholdMin(), s.getThresholdMax());
        }
    }

    private static void addReading(Zone zone) {
        if (zone.getSensors().isEmpty()) {
            System.out.println("No sensors in this zone.");
            return;
        }

        System.out.println("\n--- Select Sensor ---");
        for (int i = 0; i < zone.getSensors().size(); i++) {
            Sensor s = zone.getSensors().get(i);
            System.out.println((i+1) + ". " + s.getCode() + " (" + s.getClass().getSimpleName() + ")");
        }
        int idx = getIntInput("Choice: ") - 1;
        if (idx < 0 || idx >= zone.getSensors().size()) return;
        Sensor sensor = zone.getSensors().get(idx);

        double value = getDoubleInput("Enter reading value: ");
        Reading reading = new Reading(sensor.getCode(), value, sensor.getUnit(), LocalDateTime.now());
        sensor.addReading(reading);

        // Check for alert
        if (value < sensor.getThresholdMin() || value > sensor.getThresholdMax()) {
            SeverityLevel severity = SeverityLevel.WARNING;
            if (Math.abs(value - sensor.getThresholdMin()) > sensor.getThresholdMin() * 0.3 ||
                    Math.abs(value - sensor.getThresholdMax()) > sensor.getThresholdMax() * 0.3) {
                severity = SeverityLevel.CRITICAL;
            }
            alertSystem.checkReading(sensor.getCode(), value, sensor.getThresholdMin(), sensor.getThresholdMax(), severity);
            System.out.println("⚠ ALERT triggered! Value outside threshold range.");
        }

        System.out.println("✓ Reading recorded: " + value + " " + sensor.getUnit());
    }

    private static void viewReadingsDashboard(Zone zone) {
        System.out.println("\n--- READINGS DASHBOARD for " + zone.getName() + " ---");
        System.out.printf("%-15s %-15s %-10s %-15s%n", "Sensor", "Last Value", "Unit", "Status");
        System.out.println("-".repeat(55));

        for (Sensor s : zone.getSensors()) {
            if (!s.getReadings().isEmpty()) {
                Reading last = s.getReadings().get(s.getReadings().size() - 1);
                String status = getReadingStatus(last.getValue(), s.getThresholdMin(), s.getThresholdMax());
                System.out.printf("%-15s %-15.2f %-10s %-15s%n",
                        s.getCode(), last.getValue(), s.getUnit(), status);
            }
        }
    }

    private static void changeSensorStatus(Zone zone) {
        if (zone.getSensors().isEmpty()) return;

        System.out.println("\n--- Select Sensor ---");
        for (int i = 0; i < zone.getSensors().size(); i++) {
            Sensor s = zone.getSensors().get(i);
            System.out.println((i+1) + ". " + s.getCode() + " (Current: " + s.getStatus() + ")");
        }
        int idx = getIntInput("Choice: ") - 1;
        if (idx < 0 || idx >= zone.getSensors().size()) return;
        Sensor sensor = zone.getSensors().get(idx);

        System.out.println("1. Active");
        System.out.println("2. Faulty");
        System.out.println("3. Suspended");
        int statusChoice = getIntInput("Choice: ");

        SensorStatus newStatus = SensorStatus.ACTIVE;
        if (statusChoice == 2) newStatus = SensorStatus.FAULTY;
        else if (statusChoice == 3) newStatus = SensorStatus.SUSPENDED;

        sensor.setStatus(newStatus);
        System.out.println("✓ Sensor status updated to " + newStatus);
    }

    // ==================== ALERT MANAGEMENT ====================
    private static void manageAlerts() {
        while (true) {
            System.out.println("\n--- ALERT MANAGEMENT ---");
            System.out.println("1. View Active Alerts");
            System.out.println("2. Acknowledge Alert");
            System.out.println("3. Dismiss Alert");
            System.out.println("4. View Alert History");
            System.out.println("5. Filter Alerts by Severity");
            System.out.println("6. Back");

            int choice = getIntInput("Choice: ");

            switch (choice) {
                case 1: viewActiveAlerts(); break;
                case 2: acknowledgeAlert(); break;
                case 3: dismissAlert(); break;
                case 4: viewAlertHistory(); break;
                case 5: filterAlerts(); break;
                case 6: return;
            }
        }
    }

    private static void viewActiveAlerts() {
        List<Alert> alerts = alertSystem.getActiveAlerts();
        if (alerts.isEmpty()) {
            System.out.println("No active alerts.");
            return;
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("%-10s %-15s %-15s %-15s %-10s%n", "ID", "Sensor", "Value", "Threshold", "Severity");
        System.out.println("-".repeat(80));
        for (Alert a : alerts) {
            System.out.printf("%-10s %-15s %-15.1f [%.1f-%.1f] %-10s%n",
                    a.getId(), a.getSensorCode(), a.getReadingValue(),
                    a.getThresholdMin(), a.getThresholdMax(), a.getSeverity());
        }
    }

    private static void acknowledgeAlert() {
        List<Alert> alerts = alertSystem.getActiveAlerts();
        if (alerts.isEmpty()) {
            System.out.println("No active alerts.");
            return;
        }

        viewActiveAlerts();
        String id = getStringInput("Enter alert ID to acknowledge: ");
        alertSystem.acknowledgeAlert(id);
        System.out.println("✓ Alert acknowledged.");
    }

    private static void dismissAlert() {
        List<Alert> alerts = alertSystem.getActiveAlerts();
        if (alerts.isEmpty()) {
            System.out.println("No active alerts.");
            return;
        }

        viewActiveAlerts();
        String id = getStringInput("Enter alert ID to dismiss: ");
        alertSystem.dismissAlert(id);
        System.out.println("✓ Alert dismissed.");
    }

    private static void viewAlertHistory() {
        List<Alert> history = alertSystem.getAlertHistory();
        if (history.isEmpty()) {
            System.out.println("No alert history.");
            return;
        }

        System.out.println("\n--- ALERT HISTORY ---");
        for (Alert a : history) {
            System.out.println(a.getId() + " | " + a.getSensorCode() + " | " + a.getSeverity() +
                    " | Acknowledged: " + a.isAcknowledged() + " | Dismissed: " + a.isDismissed());
        }
    }

    private static void filterAlerts() {
        System.out.println("Filter by severity:");
        System.out.println("1. WARNING");
        System.out.println("2. CRITICAL");
        int choice = getIntInput("Choice: ");
        SeverityLevel level = choice == 1 ? SeverityLevel.WARNING : SeverityLevel.CRITICAL;

        List<Alert> filtered = alertSystem.getAlertsBySeverity(level);
        System.out.println("\n--- " + level + " ALERTS ---");
        for (Alert a : filtered) {
            System.out.println(a.getId() + " | " + a.getSensorCode() + " | " + a.getReadingValue());
        }
    }

    // ==================== REPORTS ====================
    private static void viewReports() {
        while (true) {
            System.out.println("\n--- REPORTS ---");
            System.out.println("1. Zone Summary");
            System.out.println("2. Production Report");
            System.out.println("3. Sensor Summary");
            System.out.println("4. Animal Health Summary");
            System.out.println("5. Back");

            int choice = getIntInput("Choice: ");

            switch (choice) {
                case 1: zoneSummary(); break;
                case 2: productionReport(); break;
                case 3: sensorSummary(); break;
                case 4: animalHealthSummary(); break;
                case 5: return;
            }
        }
    }

    private static void zoneSummary() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("FARM ZONE SUMMARY");
        System.out.println("=".repeat(60));

        int totalZones = cropZones.size() + livestockZones.size() + aquacultureZones.size();
        int totalSensors = 0;
        int totalEntities = 0;

        for (CropZone z : cropZones) {
            totalSensors += z.getSensors().size();
            totalEntities += z.getEntityCount();
        }
        for (LivestockZone z : livestockZones) {
            totalSensors += z.getSensors().size();
            totalEntities += z.getEntityCount();
        }
        for (AquacultureZone z : aquacultureZones) {
            totalSensors += z.getSensors().size();
            totalEntities += z.getAnimalCount();
        }

        System.out.println("Total Zones: " + totalZones);
        System.out.println("Total Entities: " + totalEntities);
        System.out.println("Total Sensors: " + totalSensors);
        System.out.println("Active Alerts: " + alertSystem.getActiveAlerts().size());
    }

    private static void productionReport() {
        System.out.println("\n--- PRODUCTION REPORT ---");
        double totalMilk = 0;
        int totalEggs = 0;

        for (LivestockZone z : livestockZones) {
            for (Animal a : z.getAnimals()) {
                if (a instanceof Ruminant) {
                    totalMilk += ((Ruminant) a).getMilkYield();
                } else if (a instanceof Poultry) {
                    totalEggs += ((Poultry) a).getEggCount();
                }
            }
        }

        System.out.println("Total Milk Production: " + totalMilk + " L");
        System.out.println("Total Egg Production: " + totalEggs + " eggs");
    }

    private static void sensorSummary() {
        int totalSensors = 0;
        int activeSensors = 0;

        for (CropZone z : cropZones) {
            totalSensors += z.getSensors().size();
            for (Sensor s : z.getSensors()) {
                if (s.getStatus() == SensorStatus.ACTIVE) activeSensors++;
            }
        }
        for (LivestockZone z : livestockZones) {
            totalSensors += z.getSensors().size();
            for (Sensor s : z.getSensors()) {
                if (s.getStatus() == SensorStatus.ACTIVE) activeSensors++;
            }
        }
        for (AquacultureZone z : aquacultureZones) {
            totalSensors += z.getSensors().size();
            for (Sensor s : z.getSensors()) {
                if (s.getStatus() == SensorStatus.ACTIVE) activeSensors++;
            }
        }

        System.out.println("Total Sensors: " + totalSensors);
        System.out.println("Active Sensors: " + activeSensors);
    }

    private static void animalHealthSummary() {
        System.out.println("\n--- ANIMAL HEALTH SUMMARY ---");
        int healthy = 0, sick = 0, quarantined = 0;

        for (LivestockZone z : livestockZones) {
            for (Animal a : z.getAnimals()) {
                switch (a.getHealthStatus()) {
                    case HEALTHY: healthy++; break;
                    case SICK: sick++; break;
                    case QUARANTINED: quarantined++; break;
                }
            }
        }

        System.out.println("Healthy: " + healthy);
        System.out.println("Sick: " + sick);
        System.out.println("Quarantined: " + quarantined);
    }

    // ==================== PRODUCTION RECORDING ====================
    private static void recordProduction() {
        System.out.println("\n--- RECORD PRODUCTION ---");
        System.out.println("1. Record Milk Production");
        System.out.println("2. Record Egg Production");
        System.out.println("3. Record Crop Yield");

        int choice = getIntInput("Choice: ");

        if (choice == 1) {
            String animalId = getStringInput("Enter animal ID: ");
            double liters = getDoubleInput("Milk produced (liters): ");

            for (LivestockZone z : livestockZones) {
                for (Animal a : z.getAnimals()) {
                    if (a instanceof Ruminant && a.getId().equals(animalId)) {
                        ((Ruminant) a).addMilkYield(liters);
                        System.out.println("✓ Recorded " + liters + " L for " + animalId);
                        return;
                    }
                }
            }
            System.out.println("Animal not found!");
        } else if (choice == 2) {
            String animalId = getStringInput("Enter animal ID: ");
            int eggs = getIntInput("Eggs produced: ");

            for (LivestockZone z : livestockZones) {
                for (Animal a : z.getAnimals()) {
                    if (a instanceof Poultry && a.getId().equals(animalId)) {
                        ((Poultry) a).addEggs(eggs);
                        System.out.println("✓ Recorded " + eggs + " eggs for " + animalId);
                        return;
                    }
                }
            }
            System.out.println("Poultry not found!");
        } else if (choice == 3) {
            System.out.println("Crop yield recorded in crop status report.");
        }
    }

    // ==================== HELPER METHODS ====================
    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static double getDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Double.parseDouble(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static LocalDate getDateInput(String prompt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        while (true) {
            try {
                System.out.print(prompt);
                return LocalDate.parse(scanner.nextLine(), formatter);
            } catch (Exception e) {
                System.out.println("Please enter date in format YYYY-MM-DD");
            }
        }
    }

    private static Zone findZoneByCode(String code, int type) {
        if (type == 1) {
            for (CropZone z : cropZones) if (z.getCode().equals(code)) return z;
        } else if (type == 2) {
            for (LivestockZone z : livestockZones) if (z.getCode().equals(code)) return z;
        } else {
            for (AquacultureZone z : aquacultureZones) if (z.getCode().equals(code)) return z;
        }
        return null;
    }

    private static String getReadingStatus(double value, double min, double max) {
        if (value < min || value > max) return "CRITICAL";
        if (value < min + (min * 0.1) || value > max - (max * 0.1)) return "WARNING";
        return "NORMAL";
    }

    private static int getDaysBetween(LocalDate date1, LocalDate date2) {
        if (date1.isBefore(date2)) {
            return -date1.until(date2).getDays();
        }
        return date2.until(date1).getDays();
    }

    private static void addSampleData() {
        // Sample Crop Zone
        CropZone cz = new CropZone("CZ001", "North Valley");
        Crop wheat = new Crop("Winter Wheat", CropFamily.CEREALS, LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 7, 15), 6.0, 7.5, 20.0, 30.0);
        cz.addCrop(wheat);
        cropZones.add(cz);

        // Sample Livestock Zone
        LivestockZone lz = new LivestockZone("LZ001", "East Pasture");
        lz.setFeedingProgram(new FeedingProgram("Hay & Grain", 5.5, 3));
        Ruminant cow = new Ruminant("R1001", "Holstein", 4, 650);
        lz.addAnimal(cow);
        livestockZones.add(lz);

        // Sample Aquaculture Zone
        AquacultureZone az = new AquacultureZone("AZ001", "West Pond");
        az.addSpecies("Tilapia");
        az.setAnimalCount(500);
        az.setFeedingProgram(new FeedingProgram("Pellets", 2.5, 3));
        aquacultureZones.add(az);

        // Sample Sensors
        EnvironmentSensor tempSensor = new EnvironmentSensor("SENS101", "CZ001", 10.0, 35.0, "temperature");
        cz.addSensor(tempSensor);
        tempSensor.addReading(new Reading("SENS101", 23.5, "°C", LocalDateTime.now()));

        System.out.println("Sample data loaded for demonstration!");
    }
}

// AlertSystem class (same as before)
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

    public int getAcknowledgedCount() {
        int count = 0;
        for (Alert a : alertHistory) {
            if (a.isAcknowledged()) count++;
        }
        return count;
    }

    public int getDismissedCount() {
        int count = 0;
        for (Alert a : alertHistory) {
            if (a.isDismissed()) count++;
        }
        return count;
    }
}