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
import java.util.Scanner;

public class Main {
    private static Scanner scanner = new Scanner(System.in);
    private static AlertSystem alertSystem = new AlertSystem();

    // Data storage
    private static List<CropZone> cropZones = new ArrayList<>();
    private static List<LivestockZone> livestockZones = new ArrayList<>();
    private static List<AquacultureZone> aquacultureZones = new ArrayList<>();
    private static List<ProductionRecord> productionRecords = new ArrayList<>();

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("     SMART FARMING MANAGEMENT SYSTEM");
            System.out.println("=".repeat(60));
            System.out.println("1. Manage Crop Zones");
            System.out.println("2. Manage Livestock Zones");
            System.out.println("3. Manage Aquaculture Zones");
            System.out.println("4. Manage Sensors");
            System.out.println("5. Monitor Alerts");
            System.out.println("6. Production Records");
            System.out.println("7. Animal Health Management");
            System.out.println("8. View System Reports");
            System.out.println("9. Exit");
            System.out.println("-".repeat(40));
            System.out.print("Choose option: ");

            int choice = getIntInput();

            switch (choice) {
                case 1: manageCropZones(); break;
                case 2: manageLivestockZones(); break;
                case 3: manageAquacultureZones(); break;
                case 4: manageSensors(); break;
                case 5: monitorAlerts(); break;
                case 6: manageProductionRecords(); break;
                case 7: manageAnimalHealth(); break;
                case 8: viewSystemReports(); break;
                case 9:
                    System.out.println("Exiting Smart Farming System...");
                    return;
                default: System.out.println("Invalid option!");
            }
        }
    }

    // ========== CROP ZONE MANAGEMENT ==========
    private static void manageCropZones() {
        System.out.println("\n--- CROP ZONE MANAGEMENT ---");
        System.out.println("1. Create New Crop Zone");
        System.out.println("2. Add Crop to Zone");
        System.out.println("3. Update Crop Growth Stage");
        System.out.println("4. List All Crop Zones");
        System.out.println("5. Back to Main Menu");
        System.out.print("Choose: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                createCropZone();
                break;
            case 2:
                addCropToZone();
                break;
            case 3:
                updateCropGrowthStage();
                break;
            case 4:
                listCropZones();
                break;
        }
    }

    private static void createCropZone() {
        System.out.print("Enter zone code: ");
        String code = scanner.nextLine();
        System.out.print("Enter zone name: ");
        String name = scanner.nextLine();

        CropZone zone = new CropZone(code, name);
        cropZones.add(zone);
        System.out.println("Crop zone created successfully!");
    }

    private static void addCropToZone() {
        if (cropZones.isEmpty()) {
            System.out.println("No crop zones available. Create one first!");
            return;
        }

        System.out.println("Select crop zone:");
        for (int i = 0; i < cropZones.size(); i++) {
            System.out.println((i+1) + ". " + cropZones.get(i).getName());
        }
        int zoneIdx = getIntInput() - 1;

        if (zoneIdx >= 0 && zoneIdx < cropZones.size()) {
            System.out.print("Enter crop name: ");
            String cropName = scanner.nextLine();

            System.out.println("Select crop family:");
            System.out.println("1. CEREALS");
            System.out.println("2. VEGETABLES");
            System.out.println("3. FRUITS");
            int familyChoice = getIntInput();
            CropFamily family = CropFamily.values()[familyChoice - 1];

            System.out.print("Enter planting date (YYYY-MM-DD): ");
            LocalDate plantDate = LocalDate.parse(scanner.nextLine());
            System.out.print("Enter expected harvest date (YYYY-MM-DD): ");
            LocalDate harvestDate = LocalDate.parse(scanner.nextLine());

            System.out.print("Enter optimal pH min: ");
            double pHMin = getDoubleInput();
            System.out.print("Enter optimal pH max: ");
            double pHMax = getDoubleInput();
            System.out.print("Enter optimal moisture min (%): ");
            double mMin = getDoubleInput();
            System.out.print("Enter optimal moisture max (%): ");
            double mMax = getDoubleInput();

            Crop crop = new Crop(cropName, family, plantDate, harvestDate, pHMin, pHMax, mMin, mMax);
            cropZones.get(zoneIdx).addCrop(crop);
            System.out.println("Crop '" + cropName + "' added successfully!");
        }
    }

    private static void updateCropGrowthStage() {
        if (cropZones.isEmpty()) {
            System.out.println("No crop zones available!");
            return;
        }

        System.out.println("Select crop zone:");
        for (int i = 0; i < cropZones.size(); i++) {
            System.out.println((i+1) + ". " + cropZones.get(i).getName());
        }
        int zoneIdx = getIntInput() - 1;

        if (zoneIdx >= 0 && zoneIdx < cropZones.size()) {
            CropZone zone = cropZones.get(zoneIdx);
            List<Crop> crops = zone.getCrops();

            if (crops.isEmpty()) {
                System.out.println("No crops in this zone!");
                return;
            }

            System.out.println("Select crop:");
            for (int i = 0; i < crops.size(); i++) {
                System.out.println((i+1) + ". " + crops.get(i).getName());
            }
            int cropIdx = getIntInput() - 1;

            if (cropIdx >= 0 && cropIdx < crops.size()) {
                Crop crop = crops.get(cropIdx);
                System.out.println("Current stage: " + crop.getGrowthStage());
                System.out.println("Select new stage:");
                GrowthStage[] stages = GrowthStage.values();
                for (int i = 0; i < stages.length; i++) {
                    System.out.println((i+1) + ". " + stages[i]);
                }
                int stageIdx = getIntInput() - 1;

                if (stageIdx >= 0 && stageIdx < stages.length) {
                    crop.setGrowthStage(stages[stageIdx]);
                    System.out.println("Growth stage updated to " + stages[stageIdx]);
                }
            }
        }
    }

    private static void listCropZones() {
        System.out.println("\n=== CROP ZONES ===");
        for (CropZone zone : cropZones) {
            System.out.println("\nZone: " + zone.getName() + " (" + zone.getCode() + ")");
            System.out.println("Status: " + zone.getStatus());
            System.out.println("Crops (" + zone.getEntityCount() + "):");
            for (Crop crop : zone.getCrops()) {
                System.out.println("  - " + crop.getName() + " | " + crop.getFamily() +
                        " | Stage: " + crop.getGrowthStage());
            }
        }
    }

    // ========== LIVESTOCK ZONE MANAGEMENT ==========
    private static void manageLivestockZones() {
        System.out.println("\n--- LIVESTOCK ZONE MANAGEMENT ---");
        System.out.println("1. Create New Livestock Zone");
        System.out.println("2. Add Animal to Zone");
        System.out.println("3. Set Feeding Program");
        System.out.println("4. Record Production");
        System.out.println("5. List All Livestock Zones");
        System.out.println("6. Back to Main Menu");
        System.out.print("Choose: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                createLivestockZone();
                break;
            case 2:
                addAnimalToZone();
                break;
            case 3:
                setFeedingProgram();
                break;
            case 4:
                recordAnimalProduction();
                break;
            case 5:
                listLivestockZones();
                break;
        }
    }

    private static void createLivestockZone() {
        System.out.print("Enter zone code: ");
        String code = scanner.nextLine();
        System.out.print("Enter zone name: ");
        String name = scanner.nextLine();

        LivestockZone zone = new LivestockZone(code, name);
        livestockZones.add(zone);
        System.out.println("Livestock zone created successfully!");
    }

    private static void addAnimalToZone() {
        if (livestockZones.isEmpty()) {
            System.out.println("No livestock zones available. Create one first!");
            return;
        }

        System.out.println("Select livestock zone:");
        for (int i = 0; i < livestockZones.size(); i++) {
            System.out.println((i+1) + ". " + livestockZones.get(i).getName());
        }
        int zoneIdx = getIntInput() - 1;

        if (zoneIdx >= 0 && zoneIdx < livestockZones.size()) {
            System.out.println("Select animal type:");
            System.out.println("1. Ruminant (Cow, Sheep, Goat)");
            System.out.println("2. Poultry (Chicken, Duck)");
            int typeChoice = getIntInput();

            System.out.print("Enter animal ID: ");
            String id = scanner.nextLine();
            System.out.print("Enter species: ");
            String species = scanner.nextLine();
            System.out.print("Enter age (years for ruminants, months for poultry): ");
            int age = getIntInput();
            System.out.print("Enter weight (kg): ");
            double weight = getDoubleInput();

            if (typeChoice == 1) {
                Ruminant ruminant = new Ruminant(id, species, age, weight);
                livestockZones.get(zoneIdx).addAnimal(ruminant);
                System.out.println("Ruminant added successfully!");
            } else {
                Poultry poultry = new Poultry(id, species, age, weight);
                livestockZones.get(zoneIdx).addAnimal(poultry);
                System.out.println("Poultry added successfully!");
            }
        }
    }

    private static void setFeedingProgram() {
        if (livestockZones.isEmpty()) {
            System.out.println("No livestock zones available!");
            return;
        }

        System.out.println("Select livestock zone:");
        for (int i = 0; i < livestockZones.size(); i++) {
            System.out.println((i+1) + ". " + livestockZones.get(i).getName());
        }
        int zoneIdx = getIntInput() - 1;

        if (zoneIdx >= 0 && zoneIdx < livestockZones.size()) {
            System.out.print("Enter feed type: ");
            String feedType = scanner.nextLine();
            System.out.print("Enter quantity per meal (kg): ");
            double quantity = getDoubleInput();
            System.out.print("Enter meals per day: ");
            int meals = getIntInput();

            FeedingProgram program = new FeedingProgram(feedType, quantity, meals);
            livestockZones.get(zoneIdx).setFeedingProgram(program);
            System.out.println("Feeding program set! Daily quantity: " + program.getDailyQuantity() + " kg");
        }
    }

    private static void recordAnimalProduction() {
        Animal animal = selectAnimal();
        if (animal == null) return;

        if (animal instanceof Ruminant) {
            System.out.print("Enter milk yield (liters): ");
            double milk = getDoubleInput();
            ((Ruminant) animal).addMilkYield(milk);
            System.out.println("Milk production recorded: " + milk + " L");
        } else if (animal instanceof Poultry) {
            System.out.print("Enter number of eggs: ");
            int eggs = getIntInput();
            ((Poultry) animal).addEggs(eggs);
            System.out.println("Egg production recorded: " + eggs + " eggs");
        }
    }

    private static Animal selectAnimal() {
        if (livestockZones.isEmpty()) {
            System.out.println("No livestock zones available!");
            return null;
        }

        System.out.println("Select livestock zone:");
        for (int i = 0; i < livestockZones.size(); i++) {
            System.out.println((i+1) + ". " + livestockZones.get(i).getName());
        }
        int zoneIdx = getIntInput() - 1;

        if (zoneIdx >= 0 && zoneIdx < livestockZones.size()) {
            LivestockZone zone = livestockZones.get(zoneIdx);
            List<Animal> animals = zone.getAnimals();

            if (animals.isEmpty()) {
                System.out.println("No animals in this zone!");
                return null;
            }

            System.out.println("Select animal:");
            for (int i = 0; i < animals.size(); i++) {
                Animal a = animals.get(i);
                System.out.println((i+1) + ". " + a.getId() + " - " + a.getSpecies() +
                        " (Type: " + a.getAnimalType() + ")");
            }
            int animalIdx = getIntInput() - 1;

            if (animalIdx >= 0 && animalIdx < animals.size()) {
                return animals.get(animalIdx);
            }
        }
        return null;
    }

    private static void listLivestockZones() {
        System.out.println("\n=== LIVESTOCK ZONES ===");
        for (LivestockZone zone : livestockZones) {
            System.out.println("\nZone: " + zone.getName() + " (" + zone.getCode() + ")");
            System.out.println("Status: " + zone.getStatus());
            System.out.println("Animals (" + zone.getEntityCount() + "):");

            FeedingProgram fp = zone.getFeedingProgram();
            if (fp != null) {
                System.out.println("Feeding Program: " + fp.getFeedType() +
                        " - " + fp.getDailyQuantity() + " kg/day");
            }

            for (Animal animal : zone.getAnimals()) {
                System.out.println("  - " + animal.getId() + " | " + animal.getSpecies() +
                        " | Age: " + animal.getAge() + " | Weight: " + animal.getWeight() + " kg" +
                        " | Health: " + animal.getHealthStatus());

                if (animal instanceof Ruminant) {
                    System.out.println("    Milk Yield: " + ((Ruminant) animal).getMilkYield() + " L");
                } else if (animal instanceof Poultry) {
                    System.out.println("    Egg Count: " + ((Poultry) animal).getEggCount());
                }
            }
        }
    }

    // ========== AQUACULTURE ZONE MANAGEMENT ==========
    private static void manageAquacultureZones() {
        System.out.println("\n--- AQUACULTURE ZONE MANAGEMENT ---");
        System.out.println("1. Create New Aquaculture Zone");
        System.out.println("2. Add Species to Zone");
        System.out.println("3. Set Feeding Program");
        System.out.println("4. List All Aquaculture Zones");
        System.out.println("5. Back to Main Menu");
        System.out.print("Choose: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                createAquacultureZone();
                break;
            case 2:
                addSpeciesToZone();
                break;
            case 3:
                setAquaFeedingProgram();
                break;
            case 4:
                listAquacultureZones();
                break;
        }
    }

    private static void createAquacultureZone() {
        System.out.print("Enter zone code: ");
        String code = scanner.nextLine();
        System.out.print("Enter zone name: ");
        String name = scanner.nextLine();

        AquacultureZone zone = new AquacultureZone(code, name);
        aquacultureZones.add(zone);
        System.out.println("Aquaculture zone created successfully!");
    }

    private static void addSpeciesToZone() {
        if (aquacultureZones.isEmpty()) {
            System.out.println("No aquaculture zones available!");
            return;
        }

        System.out.println("Select aquaculture zone:");
        for (int i = 0; i < aquacultureZones.size(); i++) {
            System.out.println((i+1) + ". " + aquacultureZones.get(i).getName());
        }
        int zoneIdx = getIntInput() - 1;

        if (zoneIdx >= 0 && zoneIdx < aquacultureZones.size()) {
            System.out.print("Enter species name: ");
            String species = scanner.nextLine();
            aquacultureZones.get(zoneIdx).addSpecies(species);
            System.out.println("Species '" + species + "' added successfully!");

            System.out.print("Enter total animal count: ");
            int count = getIntInput();
            aquacultureZones.get(zoneIdx).setAnimalCount(count);
        }
    }

    private static void setAquaFeedingProgram() {
        if (aquacultureZones.isEmpty()) {
            System.out.println("No aquaculture zones available!");
            return;
        }

        System.out.println("Select aquaculture zone:");
        for (int i = 0; i < aquacultureZones.size(); i++) {
            System.out.println((i+1) + ". " + aquacultureZones.get(i).getName());
        }
        int zoneIdx = getIntInput() - 1;

        if (zoneIdx >= 0 && zoneIdx < aquacultureZones.size()) {
            System.out.print("Enter feed type: ");
            String feedType = scanner.nextLine();
            System.out.print("Enter quantity per meal (kg): ");
            double quantity = getDoubleInput();
            System.out.print("Enter meals per day: ");
            int meals = getIntInput();

            FeedingProgram program = new FeedingProgram(feedType, quantity, meals);
            aquacultureZones.get(zoneIdx).setFeedingProgram(program);
            System.out.println("Feeding program set! Daily quantity: " + program.getDailyQuantity() + " kg");
        }
    }

    private static void listAquacultureZones() {
        System.out.println("\n=== AQUACULTURE ZONES ===");
        for (AquacultureZone zone : aquacultureZones) {
            System.out.println("\nZone: " + zone.getName() + " (" + zone.getCode() + ")");
            System.out.println("Status: " + zone.getStatus());
            System.out.println("Species: " + String.join(", ", zone.getSpecies()));
            System.out.println("Animal Count: " + zone.getAnimalCount());

            FeedingProgram fp = zone.getFeedingProgram();
            if (fp != null) {
                System.out.println("Feeding Program: " + fp.getFeedType() +
                        " - " + fp.getDailyQuantity() + " kg/day");
            }
        }
    }

    // ========== SENSOR MANAGEMENT ==========
    private static void manageSensors() {
        System.out.println("\n--- SENSOR MANAGEMENT ---");
        System.out.println("1. Add Environment Sensor");
        System.out.println("2. Add Soil Sensor");
        System.out.println("3. Add Biometric Sensor");
        System.out.println("4. Add GPS Sensor");
        System.out.println("5. Add Water Sensor");
        System.out.println("6. Add Sensor Reading");
        System.out.println("7. Suspend/Activate Sensor");
        System.out.println("8. List All Sensors");
        System.out.println("9. Back to Main Menu");
        System.out.print("Choose: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                addEnvironmentSensor();
                break;
            case 2:
                addSoilSensor();
                break;
            case 3:
                addBiometricSensor();
                break;
            case 4:
                addGPSSensor();
                break;
            case 5:
                addWaterSensor();
                break;
            case 6:
                addSensorReading();
                break;
            case 7:
                toggleSensorStatus();
                break;
            case 8:
                listAllSensors();
                break;
        }
    }

    private static Zone selectAnyZone() {
        System.out.println("Select zone type:");
        System.out.println("1. Crop Zone");
        System.out.println("2. Livestock Zone");
        System.out.println("3. Aquaculture Zone");
        int type = getIntInput();

        if (type == 1 && !cropZones.isEmpty()) {
            System.out.println("Select crop zone:");
            for (int i = 0; i < cropZones.size(); i++) {
                System.out.println((i+1) + ". " + cropZones.get(i).getName());
            }
            int idx = getIntInput() - 1;
            if (idx >= 0 && idx < cropZones.size()) return cropZones.get(idx);
        } else if (type == 2 && !livestockZones.isEmpty()) {
            System.out.println("Select livestock zone:");
            for (int i = 0; i < livestockZones.size(); i++) {
                System.out.println((i+1) + ". " + livestockZones.get(i).getName());
            }
            int idx = getIntInput() - 1;
            if (idx >= 0 && idx < livestockZones.size()) return livestockZones.get(idx);
        } else if (type == 3 && !aquacultureZones.isEmpty()) {
            System.out.println("Select aquaculture zone:");
            for (int i = 0; i < aquacultureZones.size(); i++) {
                System.out.println((i+1) + ". " + aquacultureZones.get(i).getName());
            }
            int idx = getIntInput() - 1;
            if (idx >= 0 && idx < aquacultureZones.size()) return aquacultureZones.get(idx);
        }
        return null;
    }

    private static void addEnvironmentSensor() {
        Zone zone = selectAnyZone();
        if (zone == null) {
            System.out.println("No zones available!");
            return;
        }

        System.out.print("Enter sensor code: ");
        String code = scanner.nextLine();
        System.out.print("Enter measurement type (temperature/humidity/rainfall): ");
        String type = scanner.nextLine();
        System.out.print("Enter threshold min: ");
        double min = getDoubleInput();
        System.out.print("Enter threshold max: ");
        double max = getDoubleInput();

        EnvironmentSensor sensor = new EnvironmentSensor(code, zone.getCode(), min, max, type);
        zone.addSensor(sensor);
        System.out.println("Environment sensor added successfully!");
    }

    private static void addSoilSensor() {
        Zone zone = selectAnyZone();
        if (zone == null) {
            System.out.println("No zones available!");
            return;
        }

        System.out.print("Enter sensor code: ");
        String code = scanner.nextLine();
        System.out.print("Enter measurement type (pH/moisture/nitrogen): ");
        String type = scanner.nextLine();
        System.out.print("Enter threshold min: ");
        double min = getDoubleInput();
        System.out.print("Enter threshold max: ");
        double max = getDoubleInput();

        SoilSensor sensor = new SoilSensor(code, zone.getCode(), min, max, type);
        zone.addSensor(sensor);
        System.out.println("Soil sensor added successfully!");
    }

    private static void addBiometricSensor() {
        Zone zone = selectAnyZone();
        if (zone == null) {
            System.out.println("No zones available!");
            return;
        }

        System.out.print("Enter sensor code: ");
        String code = scanner.nextLine();
        System.out.print("Enter animal ID: ");
        String animalId = scanner.nextLine();
        System.out.print("Enter measurement type (temperature/heart rate): ");
        String type = scanner.nextLine();
        System.out.print("Enter threshold min: ");
        double min = getDoubleInput();
        System.out.print("Enter threshold max: ");
        double max = getDoubleInput();

        BiometricSensor sensor = new BiometricSensor(code, zone.getCode(), min, max, animalId, type);
        zone.addSensor(sensor);
        System.out.println("Biometric sensor added successfully!");
    }

    private static void addGPSSensor() {
        Zone zone = selectAnyZone();
        if (zone == null) {
            System.out.println("No zones available!");
            return;
        }

        System.out.print("Enter sensor code: ");
        String code = scanner.nextLine();
        System.out.print("Enter animal ID: ");
        String animalId = scanner.nextLine();

        GPSSensor sensor = new GPSSensor(code, zone.getCode(), 0, 0, animalId);
        zone.addSensor(sensor);
        System.out.println("GPS sensor added successfully!");
    }

    private static void addWaterSensor() {
        Zone zone = selectAnyZone();
        if (zone == null) {
            System.out.println("No zones available!");
            return;
        }

        System.out.print("Enter sensor code: ");
        String code = scanner.nextLine();
        System.out.print("Enter measurement type (temperature/dissolved oxygen): ");
        String type = scanner.nextLine();
        System.out.print("Enter threshold min: ");
        double min = getDoubleInput();
        System.out.print("Enter threshold max: ");
        double max = getDoubleInput();

        WaterSensor sensor = new WaterSensor(code, zone.getCode(), min, max, type);
        zone.addSensor(sensor);
        System.out.println("Water sensor added successfully!");
    }

    private static void addSensorReading() {
        System.out.println("Select zone type:");
        System.out.println("1. Crop Zone");
        System.out.println("2. Livestock Zone");
        System.out.println("3. Aquaculture Zone");
        int type = getIntInput();

        List<? extends Zone> zones = null;
        if (type == 1) zones = cropZones;
        else if (type == 2) zones = livestockZones;
        else if (type == 3) zones = aquacultureZones;

        if (zones == null || zones.isEmpty()) {
            System.out.println("No zones available!");
            return;
        }

        System.out.println("Select zone:");
        for (int i = 0; i < zones.size(); i++) {
            System.out.println((i+1) + ". " + zones.get(i).getName());
        }
        int zoneIdx = getIntInput() - 1;

        if (zoneIdx >= 0 && zoneIdx < zones.size()) {
            Zone zone = zones.get(zoneIdx);
            List<Sensor> sensors = zone.getSensors();

            if (sensors.isEmpty()) {
                System.out.println("No sensors in this zone!");
                return;
            }

            System.out.println("Select sensor:");
            for (int i = 0; i < sensors.size(); i++) {
                Sensor s = sensors.get(i);
                System.out.println((i+1) + ". " + s.getCode() + " (" + s.getUnit() + ")");
            }
            int sensorIdx = getIntInput() - 1;

            if (sensorIdx >= 0 && sensorIdx < sensors.size()) {
                 Sensor sensor = sensors.get(sensorIdx);
                 System.out.print("Enter reading value: ");
                 double value = getDoubleInput();

                 Reading reading = new Reading(sensor.getCode(), value, sensor.getUnit(), LocalDateTime.now());
                 sensor.addReading(reading);

                 // Check for alerts
                 if (value < sensor.getThresholdMin() || value > sensor.getThresholdMax()) {
                     SeverityLevel severity = SeverityLevel.WARNING;
                     if (Math.abs(value - sensor.getThresholdMin()) > sensor.getThresholdMin() * 0.2 ||
                             Math.abs(value - sensor.getThresholdMax()) > sensor.getThresholdMax() * 0.2) {
                         severity = SeverityLevel.CRITICAL;
                     }
                     Alert alert = alertSystem.checkReading(sensor.getCode(), value,
                             sensor.getThresholdMin(), sensor.getThresholdMax(), severity);
                     System.out.println("ALERT: " + alert.getSeverity() + " - Value out of range!");
                 }

                 System.out.println("Reading added successfully!");
             }
        }
    }

    private static void toggleSensorStatus() {
        System.out.println("Select zone type:");
        System.out.println("1. Crop Zone");
        System.out.println("2. Livestock Zone");
        System.out.println("3. Aquaculture Zone");
        int type = getIntInput();

        List<? extends Zone> zones = null;
        if (type == 1) zones = cropZones;
        else if (type == 2) zones = livestockZones;
        else if (type == 3) zones = aquacultureZones;

        if (zones == null || zones.isEmpty()) {
            System.out.println("No zones available!");
            return;
        }

        System.out.println("Select zone:");
        for (int i = 0; i < zones.size(); i++) {
            System.out.println((i+1) + ". " + zones.get(i).getName());
        }
        int zoneIdx = getIntInput() - 1;

        if (zoneIdx >= 0 && zoneIdx < zones.size()) {
            Zone zone = zones.get(zoneIdx);
            List<Sensor> sensors = zone.getSensors();

            if (sensors.isEmpty()) {
                System.out.println("No sensors in this zone!");
                return;
            }

            System.out.println("Select sensor:");
            for (int i = 0; i < sensors.size(); i++) {
                Sensor s = sensors.get(i);
                System.out.println((i+1) + ". " + s.getCode() + " - Status: " + s.getStatus());
            }
            int sensorIdx = getIntInput() - 1;

            if (sensorIdx >= 0 && sensorIdx < sensors.size()) {
                Sensor sensor = sensors.get(sensorIdx);
                if (sensor.getStatus() == SensorStatus.ACTIVE) {
                    sensor.suspend();
                    System.out.println("Sensor suspended");
                } else {
                    sensor.activate();
                    System.out.println("Sensor activated");
                }
            }
        }
    }

    private static void listAllSensors() {
        System.out.println("\n=== ALL SENSORS ===");

        for (CropZone zone : cropZones) {
            System.out.println("\n[CROP ZONE: " + zone.getName() + "]");
            for (Sensor s : zone.getSensors()) {
                System.out.println("  - " + s.getCode() + " | Type: " + s.getClass().getSimpleName() +
                        " | Status: " + s.getStatus() + " | Unit: " + s.getUnit());
            }
        }

        for (LivestockZone zone : livestockZones) {
            System.out.println("\n[LIVESTOCK ZONE: " + zone.getName() + "]");
            for (Sensor s : zone.getSensors()) {
                System.out.println("  - " + s.getCode() + " | Type: " + s.getClass().getSimpleName() +
                        " | Status: " + s.getStatus() + " | Unit: " + s.getUnit());
            }
        }

        for (AquacultureZone zone : aquacultureZones) {
            System.out.println("\n[AQUACULTURE ZONE: " + zone.getName() + "]");
            for (Sensor s : zone.getSensors()) {
                System.out.println("  - " + s.getCode() + " | Type: " + s.getClass().getSimpleName() +
                        " | Status: " + s.getStatus() + " | Unit: " + s.getUnit());
            }
        }
    }

    // ========== ALERT MONITORING ==========
    private static void monitorAlerts() {
        System.out.println("\n--- ALERT MONITORING ---");
        System.out.println("1. View Active Alerts");
        System.out.println("2. View Alert History");
        System.out.println("3. Acknowledge Alert");
        System.out.println("4. Dismiss Alert");
        System.out.println("5. View Critical Alerts");
        System.out.println("6. Back to Main Menu");
        System.out.print("Choose: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                viewActiveAlerts();
                break;
            case 2:
                viewAlertHistory();
                break;
            case 3:
                acknowledgeAlert();
                break;
            case 4:
                dismissAlert();
                break;
            case 5:
                viewCriticalAlerts();
                break;
        }
    }

    private static void viewActiveAlerts() {
        List<Alert> alerts = alertSystem.getActiveAlerts();
        if (alerts.isEmpty()) {
            System.out.println("No active alerts!");
            return;
        }

        System.out.println("\n=== ACTIVE ALERTS ===");
        for (Alert a : alerts) {
            System.out.println("ID: " + a.getId());
            System.out.println("  Sensor: " + a.getSensorCode());
            System.out.println("  Value: " + a.getReadingValue());
            System.out.println("  Threshold: [" + a.getThresholdMin() + " - " + a.getThresholdMax() + "]");
            System.out.println("  Severity: " + a.getSeverity());
            System.out.println("  Time: " + a.getTimestamp());
            System.out.println("  Acknowledged: " + a.isAcknowledged());
            System.out.println();
        }
    }

    private static void viewAlertHistory() {
        List<Alert> alerts = alertSystem.getAlertHistory();
        if (alerts.isEmpty()) {
            System.out.println("No alert history!");
            return;
        }

        System.out.println("\n=== ALERT HISTORY (" + alerts.size() + " total) ===");
        for (Alert a : alerts) {
            System.out.println(a.getId() + " | " + a.getSensorCode() + " | " + a.getSeverity() +
                    " | " + a.getTimestamp());
        }
    }

    private static void acknowledgeAlert() {
        List<Alert> alerts = alertSystem.getActiveAlerts();
        if (alerts.isEmpty()) {
            System.out.println("No active alerts!");
            return;
        }

        System.out.println("Select alert to acknowledge:");
        for (int i = 0; i < alerts.size(); i++) {
            System.out.println((i+1) + ". " + alerts.get(i).getId() + " - " + alerts.get(i).getSensorCode());
        }
        int idx = getIntInput() - 1;

        if (idx >= 0 && idx < alerts.size()) {
            alertSystem.acknowledgeAlert(alerts.get(idx).getId());
            System.out.println("Alert acknowledged!");
        }
    }

    private static void dismissAlert() {
        List<Alert> alerts = alertSystem.getActiveAlerts();
        if (alerts.isEmpty()) {
            System.out.println("No active alerts!");
            return;
        }

        System.out.println("Select alert to dismiss:");
        for (int i = 0; i < alerts.size(); i++) {
            System.out.println((i+1) + ". " + alerts.get(i).getId() + " - " + alerts.get(i).getSensorCode());
        }
        int idx = getIntInput() - 1;

        if (idx >= 0 && idx < alerts.size()) {
            alertSystem.dismissAlert(alerts.get(idx).getId());
            System.out.println("Alert dismissed!");
        }
    }

    private static void viewCriticalAlerts() {
        List<Alert> critical = alertSystem.getAlertsBySeverity(SeverityLevel.CRITICAL);
        System.out.println("\n=== CRITICAL ALERTS (" + critical.size() + ") ===");
        for (Alert a : critical) {
            System.out.println(a.getId() + " | " + a.getSensorCode() + " | " + a.getTimestamp());
        }
    }

    // ========== PRODUCTION RECORDS ==========
    private static void manageProductionRecords() {
        System.out.println("\n--- PRODUCTION RECORDS ---");
        System.out.println("1. Create New Production Record");
        System.out.println("2. Add Production Entry");
        System.out.println("3. View Production Summary");
        System.out.println("4. Back to Main Menu");
        System.out.print("Choose: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                createProductionRecord();
                break;
            case 2:
                addProductionEntry();
                break;
            case 3:
                viewProductionSummary();
                break;
        }
    }

    private static void createProductionRecord() {
        System.out.print("Enter unit (e.g., liters, kg, eggs): ");
        String unit = scanner.nextLine();
        ProductionRecord record = new ProductionRecord(unit);
        productionRecords.add(record);
        System.out.println("Production record created for " + unit);
    }

    private static void addProductionEntry() {
        if (productionRecords.isEmpty()) {
            System.out.println("No production records. Create one first!");
            return;
        }

        System.out.println("Select record:");
        for (int i = 0; i < productionRecords.size(); i++) {
            System.out.println((i+1) + ". " + productionRecords.get(i).getUnit());
        }
        int idx = getIntInput() - 1;

        if (idx >= 0 && idx < productionRecords.size()) {
            System.out.print("Enter amount: ");
            double amount = getDoubleInput();
            productionRecords.get(idx).addProduction(amount);
            System.out.println("Production added!");
        }
    }

    private static void viewProductionSummary() {
        if (productionRecords.isEmpty()) {
            System.out.println("No production records!");
            return;
        }

        System.out.println("\n=== PRODUCTION SUMMARY ===");
        for (ProductionRecord record : productionRecords) {
            System.out.println("\nUnit: " + record.getUnit());
            System.out.println("  Total: " + record.getTotalProduction());
            System.out.println("  Average: " + String.format("%.2f", record.getAverageProduction()));
            System.out.println("  Entries: " + record.getProductions().size());
        }
    }

    // ========== ANIMAL HEALTH MANAGEMENT ==========
    private static void manageAnimalHealth() {
        System.out.println("\n--- ANIMAL HEALTH MANAGEMENT ---");
        System.out.println("1. Update Health Status");
        System.out.println("2. Log Health Event");
        System.out.println("3. Update Animal Weight");
        System.out.println("4. View Health Records");
        System.out.println("5. Back to Main Menu");
        System.out.print("Choose: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                updateHealthStatus();
                break;
            case 2:
                logHealthEvent();
                break;
            case 3:
                updateAnimalWeight();
                break;
            case 4:
                viewHealthRecords();
                break;
        }
    }

    private static void updateHealthStatus() {
        Animal animal = selectAnimal();
        if (animal == null) return;

        System.out.println("Current status: " + animal.getHealthStatus());
        System.out.println("Select new status:");
        System.out.println("1. HEALTHY");
        System.out.println("2. SICK");
        System.out.println("3. QUARANTINED");
        int statusChoice = getIntInput();

        HealthStatus status = HealthStatus.values()[statusChoice - 1];
        animal.setHealthStatus(status);
        System.out.println("Health status updated to " + status);
    }

    private static void logHealthEvent() {
        Animal animal = selectAnimal();
        if (animal == null) return;

        System.out.print("Enter health event description: ");
        String event = scanner.nextLine();
        animal.logHealthEvent(event);
        System.out.println("Health event logged!");
    }

    private static void updateAnimalWeight() {
        Animal animal = selectAnimal();
        if (animal == null) return;

        System.out.print("Enter new weight (kg): ");
        double weight = getDoubleInput();
        animal.setWeight(weight);
        System.out.println("Weight updated to " + weight + " kg");
    }

    private static void viewHealthRecords() {
        Animal animal = selectAnimal();
        if (animal == null) return;

        System.out.println("\n=== HEALTH RECORDS FOR " + animal.getId() + " ===");
        System.out.println("Species: " + animal.getSpecies());
        System.out.println("Age: " + animal.getAge());
        System.out.println("Weight: " + animal.getWeight() + " kg");
        System.out.println("Health Status: " + animal.getHealthStatus());
        System.out.println("\nHealth Events:");
        for (String event : animal.getHealthEvents()) {
            System.out.println("  - " + event);
        }
    }

    // ========== SYSTEM REPORTS ==========
    private static void viewSystemReports() {
        System.out.println("\n=== SYSTEM REPORTS ===");
        System.out.println("=".repeat(50));

        int totalCrops = 0;
        for (CropZone zone : cropZones) {
            totalCrops += zone.getEntityCount();
        }

        int totalLivestock = 0;
        for (LivestockZone zone : livestockZones) {
            totalLivestock += zone.getEntityCount();
        }

        int totalAquaculture = 0;
        for (AquacultureZone zone : aquacultureZones) {
            totalAquaculture += zone.getAnimalCount();
        }

        int totalSensors = 0;
        for (CropZone zone : cropZones) totalSensors += zone.getSensors().size();
        for (LivestockZone zone : livestockZones) totalSensors += zone.getSensors().size();
        for (AquacultureZone zone : aquacultureZones) totalSensors += zone.getSensors().size();

        System.out.println("ZONES SUMMARY:");
        System.out.println("  Crop Zones: " + cropZones.size() + " (Total Crops: " + totalCrops + ")");
        System.out.println("  Livestock Zones: " + livestockZones.size() + " (Total Animals: " + totalLivestock + ")");
        System.out.println("  Aquaculture Zones: " + aquacultureZones.size() + " (Total Stock: " + totalAquaculture + ")");

        System.out.println("\nSENSORS SUMMARY:");
        System.out.println("  Total Sensors: " + totalSensors);

        System.out.println("\nALERTS SUMMARY:");
        System.out.println("  Active Alerts: " + alertSystem.getActiveAlerts().size());
        System.out.println("  Total Alerts: " + alertSystem.getAlertHistory().size());
        System.out.println("  Critical Alerts: " + alertSystem.getAlertsBySeverity(SeverityLevel.CRITICAL).size());

        System.out.println("\nPRODUCTION SUMMARY:");
        for (ProductionRecord record : productionRecords) {
            System.out.println("  " + record.getUnit() + ": Total = " + record.getTotalProduction() +
                    ", Avg = " + String.format("%.2f", record.getAverageProduction()));
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    // ========== UTILITY METHODS ==========
    private static int getIntInput() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Please enter a number: ");
            }
        }
    }

    private static double getDoubleInput() {
        while (true) {
            try {
                return Double.parseDouble(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Please enter a number: ");
            }
        }
    }
}

// Alert System Class (needed for the main class)
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
}