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
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Data storage
    private static final List<CropZone> cropZones = new ArrayList<>();
    private static final List<LivestockZone> livestockZones = new ArrayList<>();
    private static final List<AquacultureZone> aquacultureZones = new ArrayList<>();
    private static final List<Alert> activeAlerts = new ArrayList<>();
    private static final List<Alert> alertHistory = new ArrayList<>();

    // Counters for auto-generation
    private static int alertCounter = 1;
    private static int sensorCounter = 100;
    private static int animalCounter = 1000;

    public static void main(String[] args) {
        loadSampleData();

        while (true) {
            printHeader("SMART FARMING SYSTEM");
            System.out.println("┌────────────────────────────────────────────────────┐");
            System.out.println("│                    MAIN MENU                        │");
            System.out.println("├────────────────────────────────────────────────────┤");
            System.out.println("│  1. 🌾 Manage Crop Zones                           │");
            System.out.println("│  2. 🐄 Manage Livestock Zones                      │");
            System.out.println("│  3. 🐟 Manage Aquaculture Zones                    │");
            System.out.println("│  4. 📊 View Reports                                │");
            System.out.println("│  5. ⚠️  Manage Alerts                              │");
            System.out.println("│  0. 🚪 Exit                                        │");
            System.out.println("└────────────────────────────────────────────────────┘");

            int choice = getIntInput("\n👉 Choose an option: ", 0, 5);

            switch (choice) {
                case 1 -> cropZoneMenu();
                case 2 -> livestockZoneMenu();
                case 3 -> aquacultureZoneMenu();
                case 4 -> reportsMenu();
                case 5 -> alertsMenu();
                case 0 -> {
                    System.out.println("\n🌸 Thank you for using Smart Farming System! Goodbye!\n");
                    System.exit(0);
                }
            }
        }
    }

    // ============================================================
    // CROP ZONE MENU
    // ============================================================
    private static void cropZoneMenu() {
        while (true) {
            printHeader("CROP ZONE MANAGEMENT");
            System.out.println("┌────────────────────────────────────────────────────┐");
            System.out.println("│  1. 🌱 Create New Crop Zone                        │");
            System.out.println("│  2. 📋 List All Crop Zones                         │");
            System.out.println("│  3. 🌿 Add Crop to Zone                            │");
            System.out.println("│  4. 📈 Update Crop Growth Stage                    │");
            System.out.println("│  5. 📑 Generate Crop Report                        │");
            System.out.println("│  6. ⏸️  Suspend/Activate Zone                       │");
            System.out.println("│  7. 🔙 Back to Main Menu                           │");
            System.out.println("└────────────────────────────────────────────────────┘");

            int choice = getIntInput("\n👉 Choose an option: ", 1, 7);

            switch (choice) {
                case 1 -> createCropZone();
                case 2 -> listCropZones();
                case 3 -> addCropToZone();
                case 4 -> updateCropGrowthStage();
                case 5 -> generateCropReport();
                case 6 -> toggleZoneStatus(cropZones, "CROP");
                case 7 -> { return; }
            }
        }
    }

    private static void createCropZone() {
        printSubHeader("CREATE NEW CROP ZONE");
        String code = getStringInput("Zone Code (e.g., CZ001): ");
        String name = getStringInput("Zone Name: ");
        cropZones.add(new CropZone(code, name));
        System.out.println("\n✅ Crop Zone '" + name + "' created successfully!");
        waitForEnter();
    }

    private static void listCropZones() {
        if (cropZones.isEmpty()) {
            System.out.println("\n📭 No crop zones found. Create one first!");
            waitForEnter();
            return;
        }

        printSubHeader("CROP ZONES LIST");
        System.out.printf("%-15s %-25s %-12s %-10s%n", "Code", "Name", "Status", "Crops");
        System.out.println("─".repeat(65));
        for (CropZone z : cropZones) {
            System.out.printf("%-15s %-25s %-12s %-10d%n",
                    z.getCode(), z.getName(), z.getStatus(), z.getEntityCount());
        }
        waitForEnter();
    }

    private static void addCropToZone() {
        if (cropZones.isEmpty()) {
            System.out.println("\n📭 No crop zones available! Create one first.");
            waitForEnter();
            return;
        }

        CropZone zone = selectCropZone();
        if (zone == null) return;

        printSubHeader("ADD NEW CROP to " + zone.getName());

        String name = getStringInput("Crop Name: ");

        System.out.println("\nCrop Family:");
        CropFamily[] families = CropFamily.values();
        for (int i = 0; i < families.length; i++) {
            System.out.println("  " + (i+1) + ". " + families[i]);
        }
        CropFamily family = families[getIntInput("Choice: ", 1, families.length) - 1];

        LocalDate plantDate = getDateInput("Planting Date (YYYY-MM-DD): ");
        LocalDate harvestDate = getDateInput("Expected Harvest Date (YYYY-MM-DD): ");

        double phMin = getDoubleInput("Optimal pH Minimum: ");
        double phMax = getDoubleInput("Optimal pH Maximum: ");
        double moistureMin = getDoubleInput("Optimal Moisture Minimum (%): ");
        double moistureMax = getDoubleInput("Optimal Moisture Maximum (%): ");

        Crop crop = new Crop(name, family, plantDate, harvestDate, phMin, phMax, moistureMin, moistureMax);
        zone.addCrop(crop);
        System.out.println("\n✅ Crop '" + name + "' added successfully!");
        waitForEnter();
    }

    private static void updateCropGrowthStage() {
        CropZone zone = selectCropZone();
        if (zone == null || zone.getCrops().isEmpty()) {
            System.out.println("\n📭 No crops in this zone.");
            waitForEnter();
            return;
        }

        printSubHeader("UPDATE GROWTH STAGE");

        System.out.println("\nSelect Crop:");
        for (int i = 0; i < zone.getCrops().size(); i++) {
            Crop c = zone.getCrops().get(i);
            System.out.println("  " + (i+1) + ". " + c.getName() + " (Current: " + c.getGrowthStage() + ")");
        }

        Crop crop = zone.getCrops().get(getIntInput("Choice: ", 1, zone.getCrops().size()) - 1);

        System.out.println("\nNew Growth Stage:");
        GrowthStage[] stages = GrowthStage.values();
        for (int i = 0; i < stages.length; i++) {
            System.out.println("  " + (i+1) + ". " + stages[i]);
        }

        GrowthStage newStage = stages[getIntInput("Choice: ", 1, stages.length) - 1];
        crop.setGrowthStage(newStage);
        System.out.println("\n✅ Growth stage updated to " + newStage);
        waitForEnter();
    }

    private static void generateCropReport() {
        CropZone zone = selectCropZone();
        if (zone == null || zone.getCrops().isEmpty()) {
            System.out.println("\n📭 No crops in this zone.");
            waitForEnter();
            return;
        }

        printSubHeader("CROP STATUS REPORT - " + zone.getName());
        System.out.println();

        for (Crop c : zone.getCrops()) {
            System.out.println("┌─────────────────────────────────────────┐");
            System.out.printf("│ 🌾 %-35s│%n", c.getName());
            System.out.println("├─────────────────────────────────────────┤");
            System.out.printf("│ Family:      %-25s│%n", c.getFamily());
            System.out.printf("│ Growth Stage: %-25s│%n", c.getGrowthStage());
            System.out.printf("│ Planting:    %-25s│%n", c.getPlantingDate());
            System.out.printf("│ Harvest:     %-25s│%n", c.getExpectedHarvestDate());
            System.out.printf("│ pH Range:    %.1f - %.1f %18s│%n", c.getOptimalPHMin(), c.getOptimalPHMax(), "");
            System.out.printf("│ Moisture:    %.0f%% - %.0f%% %18s│%n", c.getOptimalMoistureMin(), c.getOptimalMoistureMax(), "");
            System.out.println("└─────────────────────────────────────────┘");
            System.out.println();
        }
        waitForEnter();
    }

    // ============================================================
    // LIVESTOCK ZONE MENU
    // ============================================================
    private static void livestockZoneMenu() {
        while (true) {
            printHeader("LIVESTOCK ZONE MANAGEMENT");
            System.out.println("┌────────────────────────────────────────────────────┐");
            System.out.println("│  1. 🏠 Create New Livestock Zone                   │");
            System.out.println("│  2. 📋 List All Livestock Zones                    │");
            System.out.println("│  3. 🐮 Add Animal to Zone                          │");
            System.out.println("│  4. 📝 Log Health Event                            │");
            System.out.println("│  5. 🍽️  View/Update Feeding Schedule               │");
            System.out.println("│  6. 📊 Generate Animal Report                      │");
            System.out.println("│  7. ⏸️  Suspend/Activate Zone                       │");
            System.out.println("│  8. 🔙 Back to Main Menu                           │");
            System.out.println("└────────────────────────────────────────────────────┘");

            int choice = getIntInput("\n👉 Choose an option: ", 1, 8);

            switch (choice) {
                case 1 -> createLivestockZone();
                case 2 -> listLivestockZones();
                case 3 -> addAnimalToZone();
                case 4 -> logHealthEvent();
                case 5 -> manageFeedingSchedule();
                case 6 -> generateAnimalReport();
                case 7 -> toggleZoneStatus(livestockZones, "LIVESTOCK");
                case 8 -> { return; }
            }
        }
    }

    private static void createLivestockZone() {
        printSubHeader("CREATE NEW LIVESTOCK ZONE");
        String code = getStringInput("Zone Code (e.g., LZ001): ");
        String name = getStringInput("Zone Name: ");

        LivestockZone zone = new LivestockZone(code, name);

        String feedType = getStringInput("Feed Type: ");
        double quantity = getDoubleInput("Quantity per Meal (kg): ");
        int meals = getIntInput("Meals per Day: ", 1, 10);
        zone.setFeedingProgram(new FeedingProgram(feedType, quantity, meals));

        livestockZones.add(zone);
        System.out.println("\n✅ Livestock Zone '" + name + "' created successfully!");
        waitForEnter();
    }

    private static void listLivestockZones() {
        if (livestockZones.isEmpty()) {
            System.out.println("\n📭 No livestock zones found. Create one first!");
            waitForEnter();
            return;
        }

        printSubHeader("LIVESTOCK ZONES LIST");
        System.out.printf("%-15s %-25s %-12s %-10s%n", "Code", "Name", "Status", "Animals");
        System.out.println("─".repeat(65));
        for (LivestockZone z : livestockZones) {
            System.out.printf("%-15s %-25s %-12s %-10d%n",
                    z.getCode(), z.getName(), z.getStatus(), z.getEntityCount());
        }
        waitForEnter();
    }

    private static void addAnimalToZone() {
        if (livestockZones.isEmpty()) {
            System.out.println("\n📭 No livestock zones available!");
            waitForEnter();
            return;
        }

        LivestockZone zone = selectLivestockZone();
        if (zone == null) return;

        printSubHeader("ADD ANIMAL to " + zone.getName());

        System.out.println("\nAnimal Type:");
        System.out.println("  1. Ruminant (Cow, Sheep, Goat)");
        System.out.println("  2. Poultry (Chicken, Turkey)");
        int type = getIntInput("Choice: ", 1, 2);

        String id = (type == 1 ? "R" : "P") + (animalCounter++);
        String species = getStringInput("Species: ");
        int age = getIntInput("Age (years): ", 0, 30);
        double weight = getDoubleInput("Weight (kg): ");

        if (type == 1) {
            zone.addAnimal(new Ruminant(id, species, age, weight));
        } else {
            zone.addAnimal(new Poultry(id, species, age, weight));
        }

        System.out.println("\n✅ Animal added! ID: " + id);
        waitForEnter();
    }

    private static void logHealthEvent() {
        LivestockZone zone = selectLivestockZone();
        if (zone == null || zone.getAnimals().isEmpty()) {
            System.out.println("\n📭 No animals in this zone.");
            waitForEnter();
            return;
        }

        printSubHeader("LOG HEALTH EVENT");

        System.out.println("\nSelect Animal:");
        List<Animal> animals = zone.getAnimals();
        for (int i = 0; i < animals.size(); i++) {
            Animal a = animals.get(i);
            System.out.printf("  %d. %s - %s (Health: %s, Weight: %.1f kg)%n",
                    i+1, a.getId(), a.getSpecies(), a.getHealthStatus(), a.getWeight());
        }

        Animal animal = animals.get(getIntInput("Choice: ", 1, animals.size()) - 1);

        System.out.println("\nAction:");
        System.out.println("  1. Change Health Status");
        System.out.println("  2. Update Weight");
        int action = getIntInput("Choice: ", 1, 2);

        if (action == 1) {
            System.out.println("\nHealth Status:");
            HealthStatus[] statuses = HealthStatus.values();
            for (int i = 0; i < statuses.length; i++) {
                System.out.println("  " + (i+1) + ". " + statuses[i]);
            }
            HealthStatus newStatus = statuses[getIntInput("Choice: ", 1, statuses.length) - 1];
            animal.setHealthStatus(newStatus);
            System.out.println("\n✅ Health status updated to " + newStatus);
        } else {
            double newWeight = getDoubleInput("New Weight (kg): ");
            animal.setWeight(newWeight);
            System.out.println("\n✅ Weight updated to " + newWeight + " kg");
        }
        waitForEnter();
    }

    private static void manageFeedingSchedule() {
        LivestockZone zone = selectLivestockZone();
        if (zone == null) return;

        printSubHeader("FEEDING SCHEDULE - " + zone.getName());

        if (zone.getFeedingProgram() != null) {
            FeedingProgram fp = zone.getFeedingProgram();
            System.out.println("\nCurrent Schedule:");
            System.out.println("  Feed Type: " + fp.getFeedType());
            System.out.println("  Quantity per Meal: " + fp.getQuantityPerMeal() + " kg");
            System.out.println("  Meals per Day: " + fp.getMealsPerDay());
            System.out.println("  Total Daily Feed: " + fp.getDailyQuantity() + " kg");
        }

        System.out.println("\nUpdate Schedule?");
        System.out.println("  1. Yes");
        System.out.println("  2. No");
        if (getIntInput("Choice: ", 1, 2) == 1) {
            String feedType = getStringInput("New Feed Type: ");
            double quantity = getDoubleInput("Quantity per Meal (kg): ");
            int meals = getIntInput("Meals per Day: ", 1, 10);
            zone.setFeedingProgram(new FeedingProgram(feedType, quantity, meals));
            System.out.println("\n✅ Feeding schedule updated!");
        }
        waitForEnter();
    }

    private static void generateAnimalReport() {
        LivestockZone zone = selectLivestockZone();
        if (zone == null) return;

        printSubHeader("ANIMAL REPORT - " + zone.getName());

        if (zone.getAnimals().isEmpty()) {
            System.out.println("\n📭 No animals in this zone.");
            waitForEnter();
            return;
        }

        System.out.printf("\n%-12s %-15s %-10s %-8s %-12s %-10s%n",
                "ID", "Species", "Type", "Age", "Health", "Weight(kg)");
        System.out.println("─".repeat(70));

        double totalMilk = 0;
        int totalEggs = 0;

        for (Animal a : zone.getAnimals()) {
            String type = a.getAnimalType().toString();
            System.out.printf("%-12s %-15s %-10s %-8d %-12s %-10.1f%n",
                    a.getId(), a.getSpecies(), type, a.getAge(), a.getHealthStatus(), a.getWeight());

            if (a instanceof Ruminant) totalMilk += ((Ruminant) a).getMilkYield();
            if (a instanceof Poultry) totalEggs += ((Poultry) a).getEggCount();
        }

        if (totalMilk > 0) System.out.println("\n🥛 Total Milk Production: " + totalMilk + " L");
        if (totalEggs > 0) System.out.println("🥚 Total Egg Production: " + totalEggs + " eggs");

        waitForEnter();
    }

    // ============================================================
    // AQUACULTURE ZONE MENU
    // ============================================================
    private static void aquacultureZoneMenu() {
        while (true) {
            printHeader("AQUACULTURE ZONE MANAGEMENT");
            System.out.println("┌────────────────────────────────────────────────────┐");
            System.out.println("│  1. 🐟 Create New Aquaculture Zone                 │");
            System.out.println("│  2. 📋 List All Aquaculture Zones                  │");
            System.out.println("│  3. 📊 Generate Aquaculture Report                 │");
            System.out.println("│  4. ⏸️  Suspend/Activate Zone                       │");
            System.out.println("│  5. 🔙 Back to Main Menu                           │");
            System.out.println("└────────────────────────────────────────────────────┘");

            int choice = getIntInput("\n👉 Choose an option: ", 1, 5);

            switch (choice) {
                case 1 -> createAquacultureZone();
                case 2 -> listAquacultureZones();
                case 3 -> generateAquacultureReport();
                case 4 -> toggleZoneStatus(aquacultureZones, "AQUACULTURE");
                case 5 -> { return; }
            }
        }
    }

    private static void createAquacultureZone() {
        printSubHeader("CREATE NEW AQUACULTURE ZONE");
        String code = getStringInput("Zone Code (e.g., AZ001): ");
        String name = getStringInput("Zone Name: ");

        AquacultureZone zone = new AquacultureZone(code, name);

        int numSpecies = getIntInput("Number of Species: ", 1, 5);
        for (int i = 0; i < numSpecies; i++) {
            String species = getStringInput("Species " + (i+1) + ": ");
            zone.addSpecies(species);
        }

        int count = getIntInput("Total Animal Count: ", 1, 10000);
        zone.setAnimalCount(count);

        String feedType = getStringInput("Feed Type: ");
        double quantity = getDoubleInput("Quantity per Meal (kg): ");
        int meals = getIntInput("Meals per Day: ", 1, 10);
        zone.setFeedingProgram(new FeedingProgram(feedType, quantity, meals));

        aquacultureZones.add(zone);
        System.out.println("\n✅ Aquaculture Zone '" + name + "' created successfully!");
        waitForEnter();
    }

    private static void listAquacultureZones() {
        if (aquacultureZones.isEmpty()) {
            System.out.println("\n📭 No aquaculture zones found.");
            waitForEnter();
            return;
        }

        printSubHeader("AQUACULTURE ZONES LIST");
        System.out.printf("%-15s %-25s %-12s %-10s%n", "Code", "Name", "Status", "Animals");
        System.out.println("─".repeat(65));
        for (AquacultureZone z : aquacultureZones) {
            System.out.printf("%-15s %-25s %-12s %-10d%n",
                    z.getCode(), z.getName(), z.getStatus(), z.getAnimalCount());
        }
        waitForEnter();
    }

    private static void generateAquacultureReport() {
        AquacultureZone zone = selectAquacultureZone();
        if (zone == null) return;

        printSubHeader("AQUACULTURE REPORT - " + zone.getName());

        System.out.println("\n┌─────────────────────────────────────────┐");
        System.out.printf("│ 🌊 Species:                              │%n");
        for (String s : zone.getSpecies()) {
            System.out.printf("│    • %-35s│%n", s);
        }
        System.out.printf("│                                           │%n");
        System.out.printf("│ 📊 Animal Count: %-25d│%n", zone.getAnimalCount());
        System.out.printf("│                                           │%n");
        System.out.printf("│ 🍽️  Feeding Program:                      │%n");
        if (zone.getFeedingProgram() != null) {
            System.out.printf("│    Feed: %-33s│%n", zone.getFeedingProgram().getFeedType());
            System.out.printf("│    Daily: %.1f kg %-23s│%n", zone.getFeedingProgram().getDailyQuantity(), "");
        }
        System.out.println("└─────────────────────────────────────────┘");
        waitForEnter();
    }

    // ============================================================
    // REPORTS MENU
    // ============================================================
    private static void reportsMenu() {
        while (true) {
            printHeader("REPORTS & ANALYTICS");
            System.out.println("┌────────────────────────────────────────────────────┐");
            System.out.println("│  1. 📊 Farm Overview                               │");
            System.out.println("│  2. 🌾 Crop Production Report                      │");
            System.out.println("│  3. 🐄 Livestock Production Report                 │");
            System.out.println("│  4. 📈 Sensor Summary                              │");
            System.out.println("│  5. 🔙 Back to Main Menu                           │");
            System.out.println("└────────────────────────────────────────────────────┘");

            int choice = getIntInput("\n👉 Choose an option: ", 1, 5);

            switch (choice) {
                case 1 -> showFarmOverview();
                case 2 -> showCropProductionReport();
                case 3 -> showLivestockProductionReport();
                case 4 -> showSensorSummary();
                case 5 -> { return; }
            }
        }
    }

    private static void showFarmOverview() {
        printHeader("FARM OVERVIEW");

        int totalCrops = cropZones.stream().mapToInt(Zone::getEntityCount).sum();
        int totalAnimals = livestockZones.stream().mapToInt(Zone::getEntityCount).sum();
        int totalAquatic = aquacultureZones.stream().mapToInt(AquacultureZone::getAnimalCount).sum();
        int totalZones = cropZones.size() + livestockZones.size() + aquacultureZones.size();

        System.out.println("\n┌─────────────────────────────────────────┐");
        System.out.println("│         📊 STATISTICS SUMMARY          │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.printf("│ 🌾 Crop Zones:     %-20d│%n", cropZones.size());
        System.out.printf("│ 🐄 Livestock Zones: %-20d│%n", livestockZones.size());
        System.out.printf("│ 🐟 Aquaculture Zones: %-18d│%n", aquacultureZones.size());
        System.out.printf("│                                           │%n");
        System.out.printf("│ Total Zones:      %-20d│%n", totalZones);
        System.out.printf("│ Total Crops:      %-20d│%n", totalCrops);
        System.out.printf("│ Total Animals:    %-20d│%n", totalAnimals);
        System.out.printf("│ Total Aquatic:    %-20d│%n", totalAquatic);
        System.out.printf("│                                           │%n");
        System.out.printf("│ ⚠️  Active Alerts:  %-20d│%n", activeAlerts.size());
        System.out.println("└─────────────────────────────────────────┘");
        waitForEnter();
    }

    private static void showCropProductionReport() {
        printSubHeader("CROP PRODUCTION REPORT");

        if (cropZones.isEmpty()) {
            System.out.println("\n📭 No crop zones available.");
            waitForEnter();
            return;
        }

        for (CropZone zone : cropZones) {
            System.out.println("\n┌─────────────────────────────────────────┐");
            System.out.printf("│ 🌾 %-35s│%n", zone.getName());
            System.out.println("├─────────────────────────────────────────┤");
            for (Crop c : zone.getCrops()) {
                String status = c.getGrowthStage().toString();
                System.out.printf("│ • %-15s : %-20s│%n", c.getName(), status);
            }
            System.out.println("└─────────────────────────────────────────┘");
        }
        waitForEnter();
    }

    private static void showLivestockProductionReport() {
        printSubHeader("LIVESTOCK PRODUCTION REPORT");

        double totalMilk = 0;
        int totalEggs = 0;

        for (LivestockZone zone : livestockZones) {
            for (Animal a : zone.getAnimals()) {
                if (a instanceof Ruminant) totalMilk += ((Ruminant) a).getMilkYield();
                if (a instanceof Poultry) totalEggs += ((Poultry) a).getEggCount();
            }
        }

        System.out.println("\n┌─────────────────────────────────────────┐");
        System.out.println("│         🥛 MILK PRODUCTION               │");
        System.out.printf("│   Total: %.1f L %-22s│%n", totalMilk, "");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│         🥚 EGG PRODUCTION                │");
        System.out.printf("│   Total: %d eggs %-24s│%n", totalEggs, "");
        System.out.println("└─────────────────────────────────────────┘");
        waitForEnter();
    }

    private static void showSensorSummary() {
        printSubHeader("SENSOR SUMMARY");

        int totalSensors = 0;
        int activeSensors = 0;

        List<Zone> allZones = new ArrayList<>();
        allZones.addAll(cropZones);
        allZones.addAll(livestockZones);
        allZones.addAll(aquacultureZones);

        for (Zone z : allZones) {
            totalSensors += z.getSensors().size();
            for (Sensor s : z.getSensors()) {
                if (s.getStatus() == SensorStatus.ACTIVE) activeSensors++;
            }
        }

        System.out.println("\n┌─────────────────────────────────────────┐");
        System.out.println("│         📡 SENSOR STATUS                 │");
        System.out.printf("│   Total Sensors:  %-20d│%n", totalSensors);
        System.out.printf("│   Active:         %-20d│%n", activeSensors);
        System.out.printf("│   Inactive:       %-20d│%n", totalSensors - activeSensors);
        System.out.println("└─────────────────────────────────────────┘");
        waitForEnter();
    }

    // ============================================================
    // ALERTS MENU
    // ============================================================
    private static void alertsMenu() {
        while (true) {
            printHeader("ALERT MANAGEMENT");
            System.out.println("┌────────────────────────────────────────────────────┐");
            System.out.println("│  1. ⚠️  View Active Alerts                          │");
            System.out.println("│  2. ✅ Acknowledge Alert                            │");
            System.out.println("│  3. ❌ Dismiss Alert                                │");
            System.out.println("│  4. 📜 View Alert History                           │");
            System.out.println("│  5. 🔍 Filter Alerts by Severity                    │");
            System.out.println("│  6. 🔙 Back to Main Menu                           │");
            System.out.println("└────────────────────────────────────────────────────┘");

            int choice = getIntInput("\n👉 Choose an option: ", 1, 6);

            switch (choice) {
                case 1 -> viewActiveAlerts();
                case 2 -> acknowledgeAlert();
                case 3 -> dismissAlert();
                case 4 -> viewAlertHistory();
                case 5 -> filterAlertsBySeverity();
                case 6 -> { return; }
            }
        }
    }

    private static void viewActiveAlerts() {
        if (activeAlerts.isEmpty()) {
            System.out.println("\n✅ No active alerts! All systems normal.");
            waitForEnter();
            return;
        }

        printSubHeader("ACTIVE ALERTS");
        System.out.printf("%-12s %-15s %-12s %-20s %-10s%n", "ID", "Sensor", "Value", "Threshold", "Severity");
        System.out.println("─".repeat(70));

        for (Alert a : activeAlerts) {
            System.out.printf("%-12s %-15s %-12.1f [%.1f-%.1f] %-10s%n",
                    a.getId(), a.getSensorCode(), a.getReadingValue(),
                    a.getThresholdMin(), a.getThresholdMax(), a.getSeverity());
        }
        waitForEnter();
    }

    private static void acknowledgeAlert() {
        if (activeAlerts.isEmpty()) {
            System.out.println("\n✅ No active alerts to acknowledge.");
            waitForEnter();
            return;
        }

        viewActiveAlerts();
        String id = getStringInput("\nEnter Alert ID to acknowledge: ");

        for (Alert a : activeAlerts) {
            if (a.getId().equalsIgnoreCase(id)) {
                a.acknowledge();
                System.out.println("\n✅ Alert " + id + " acknowledged!");
                waitForEnter();
                return;
            }
        }
        System.out.println("\n❌ Alert not found!");
        waitForEnter();
    }

    private static void dismissAlert() {
        if (activeAlerts.isEmpty()) {
            System.out.println("\n✅ No active alerts to dismiss.");
            waitForEnter();
            return;
        }

        viewActiveAlerts();
        String id = getStringInput("\nEnter Alert ID to dismiss: ");

        for (int i = 0; i < activeAlerts.size(); i++) {
            if (activeAlerts.get(i).getId().equalsIgnoreCase(id)) {
                activeAlerts.get(i).dismiss();
                activeAlerts.remove(i);
                System.out.println("\n✅ Alert " + id + " dismissed!");
                waitForEnter();
                return;
            }
        }
        System.out.println("\n❌ Alert not found!");
        waitForEnter();
    }

    private static void viewAlertHistory() {
        if (alertHistory.isEmpty()) {
            System.out.println("\n📭 No alert history.");
            waitForEnter();
            return;
        }

        printSubHeader("ALERT HISTORY");
        for (Alert a : alertHistory) {
            System.out.printf("%s | %s | %s | Acknowledged: %s | Dismissed: %s%n",
                    a.getId(), a.getSensorCode(), a.getSeverity(), a.isAcknowledged(), a.isDismissed());
        }
        waitForEnter();
    }

    private static void filterAlertsBySeverity() {
        System.out.println("\nFilter by Severity:");
        System.out.println("  1. WARNING");
        System.out.println("  2. CRITICAL");
        int choice = getIntInput("Choice: ", 1, 2);
        SeverityLevel level = choice == 1 ? SeverityLevel.WARNING : SeverityLevel.CRITICAL;

        List<Alert> filtered = new ArrayList<>();
        for (Alert a : alertHistory) {
            if (a.getSeverity() == level) filtered.add(a);
        }

        if (filtered.isEmpty()) {
            System.out.println("\n📭 No " + level + " alerts found.");
        } else {
            printSubHeader(level + " ALERTS");
            for (Alert a : filtered) {
                System.out.println(a.getId() + " | " + a.getSensorCode() + " | Value: " + a.getReadingValue());
            }
        }
        waitForEnter();
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================
    private static void toggleZoneStatus(List<? extends Zone> zones, String typeName) {
        if (zones.isEmpty()) {
            System.out.println("\n📭 No " + typeName + " zones available.");
            waitForEnter();
            return;
        }

        System.out.println("\nSelect Zone:");
        for (int i = 0; i < zones.size(); i++) {
            System.out.println("  " + (i+1) + ". " + zones.get(i).getName() + " (Status: " + zones.get(i).getStatus() + ")");
        }

        Zone zone = zones.get(getIntInput("Choice: ", 1, zones.size()) - 1);

        System.out.println("\nCurrent Status: " + zone.getStatus());
        System.out.println("  1. Suspend");
        System.out.println("  2. Activate");

        int action = getIntInput("Choice: ", 1, 2);

        if (action == 1) {
            zone.suspend();
            System.out.println("\n⏸️ Zone suspended. All sensors deactivated.");
        } else {
            zone.activate();
            System.out.println("\n▶️ Zone activated. All sensors restored.");
        }
        waitForEnter();
    }

    private static CropZone selectCropZone() {
        if (cropZones.isEmpty()) return null;

        System.out.println("\nSelect Crop Zone:");
        for (int i = 0; i < cropZones.size(); i++) {
            System.out.println("  " + (i+1) + ". " + cropZones.get(i).getName());
        }
        return cropZones.get(getIntInput("Choice: ", 1, cropZones.size()) - 1);
    }

    private static LivestockZone selectLivestockZone() {
        if (livestockZones.isEmpty()) return null;

        System.out.println("\nSelect Livestock Zone:");
        for (int i = 0; i < livestockZones.size(); i++) {
            System.out.println("  " + (i+1) + ". " + livestockZones.get(i).getName());
        }
        return livestockZones.get(getIntInput("Choice: ", 1, livestockZones.size()) - 1);
    }

    private static AquacultureZone selectAquacultureZone() {
        if (aquacultureZones.isEmpty()) return null;

        System.out.println("\nSelect Aquaculture Zone:");
        for (int i = 0; i < aquacultureZones.size(); i++) {
            System.out.println("  " + (i+1) + ". " + aquacultureZones.get(i).getName());
        }
        return aquacultureZones.get(getIntInput("Choice: ", 1, aquacultureZones.size()) - 1);
    }

    private static void printHeader(String title) {
        System.out.println("\n" + "=".repeat(55));
        System.out.println("  " + title);
        System.out.println("=".repeat(55));
    }

    private static void printSubHeader(String title) {
        System.out.println("\n📌 " + title);
        System.out.println("─".repeat(50));
    }

    private static void waitForEnter() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static int getIntInput(String prompt, int min, int max) {
        while (true) {
            try {
                System.out.print(prompt);
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) return value;
                System.out.println("❌ Please enter a number between " + min + " and " + max);
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid input. Please enter a number.");
            }
        }
    }

    private static double getDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid input. Please enter a number.");
            }
        }
    }

    private static LocalDate getDateInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return LocalDate.parse(scanner.nextLine().trim(), DATE_FORMATTER);
            } catch (Exception e) {
                System.out.println("❌ Invalid date. Please use YYYY-MM-DD format.");
            }
        }
    }

    private static void loadSampleData() {
        // Crop Zone with sample crops
        CropZone cropZone = new CropZone("CZ001", "North Valley Farm");
        cropZone.addCrop(new Crop("Winter Wheat", CropFamily.CEREALS,
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 7, 15), 6.0, 7.5, 20.0, 30.0));
        cropZone.addCrop(new Crop("Cherry Tomato", CropFamily.VEGETABLES,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30), 6.2, 6.8, 25.0, 35.0));
        cropZones.add(cropZone);

        // Livestock Zone with sample animals
        LivestockZone livestockZone = new LivestockZone("LZ001", "East Pasture");
        livestockZone.setFeedingProgram(new FeedingProgram("Organic Hay Mix", 5.5, 3));
        Ruminant cow = new Ruminant("R1001", "Holstein Friesian", 4, 650.0);
        cow.addMilkYield(125.5);
        livestockZone.addAnimal(cow);
        livestockZone.addAnimal(new Poultry("P1001", "Rhode Island Red", 1, 2.5));
        livestockZones.add(livestockZone);

        // Aquaculture Zone
        AquacultureZone aquaZone = new AquacultureZone("AZ001", "West Pond");
        aquaZone.addSpecies("Nile Tilapia");
        aquaZone.addSpecies("African Catfish");
        aquaZone.setAnimalCount(1250);
        aquaZone.setFeedingProgram(new FeedingProgram("Protein Pellets", 3.5, 4));
        aquacultureZones.add(aquaZone);

        // Sample sensor with reading
        EnvironmentSensor sensor = new EnvironmentSensor("SENS101", "CZ001", 10.0, 35.0, "temperature");
        cropZone.addSensor(sensor);
        sensor.addReading(new Reading("SENS101", 23.5, "°C", LocalDateTime.now()));

        System.out.println("\n📦 Sample data loaded successfully!");
    }
}