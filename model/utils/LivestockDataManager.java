package model.utils;

import model.entities.FeedingProgram;
import model.enums.*;
import model.interfaces.Producing;
import model.zones.*;
import model.sensors.*;
import model.animals.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handles loading and saving of LivestockZone data to/from data/livestock_zones.txt
 */
public class LivestockDataManager {

    static final String LIVESTOCK_FILE = "data/livestock_zones.txt";
    static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // -------------------------------------------------------------------------
    // SAVE
    // -------------------------------------------------------------------------

    public static void save(List<LivestockZone> livestockZones) {
        try (PrintWriter w = new PrintWriter(new FileWriter(LIVESTOCK_FILE))) {
            w.println("#LIVESTOCK_ZONES");
            for (LivestockZone zone : livestockZones) {
                w.printf(Locale.US, "ZONE|%s|%s|%s|%f|%f|%f|%f|%s%n",
                        zone.getCode(), zone.getName(), zone.getStatus(),
                        zone.getBoundNorth(), zone.getBoundSouth(),
                        zone.getBoundEast(), zone.getBoundWest(),
                        zone.getAllowedAnimalType());

                if (zone.getFeedingProgram() != null) {
                    w.printf(Locale.US, "FEED|%s|%s|%f|%d%n",
                            zone.getCode(), zone.getFeedingProgram().getFeedType(),
                            zone.getFeedingProgram().getQuantityPerMeal(),
                            zone.getFeedingProgram().getMealsPerDay());
                }

                for (Animal animal : zone.getAnimals()) {
                    w.printf(Locale.US, "ANIMAL|%s|%s|%s|%d|%f|%s",
                            zone.getCode(), animal.getId(), animal.getSpecies(),
                            animal.getAge(), animal.getWeight(), animal.getHealthStatus());
                    if (animal instanceof Ruminant) {
                        w.printf(Locale.US, "|RUMINANT|%f%n", ((Ruminant) animal).getMilkYield());
                    } else if (animal instanceof Poultry) {
                        w.printf(Locale.US, "|POULTRY|%d%n", ((Poultry) animal).getEggCount());
                    } else {
                        w.println();
                    }
                    if (animal instanceof Producing producing) {
                        for (var entry : producing.getProductionRecord().getEntries()) {
                            w.printf(Locale.US, "PRODUCTION|%s|%f|%s%n",
                                    animal.getId(), entry.getAmount(),
                                    entry.getRecordedAt().format(DT_FMT));
                        }
                    }
                    for (String event : animal.getHealthEvents()) {
                        w.printf("HEALTH_EVENT|%s|%s%n", animal.getId(), event);
                    }
                }

                for (Sensor sensor : zone.getSensors()) {
                    SensorSerializer.write(w, zone.getCode(), sensor, DT_FMT);
                }
            }
            System.out.println("Livestock zones saved to " + LIVESTOCK_FILE);
        } catch (IOException e) {
            System.err.println("Error saving livestock zones: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // LOAD
    // -------------------------------------------------------------------------

    public static void load(List<LivestockZone> livestockZones) {
        File file = new File(LIVESTOCK_FILE);
        if (!file.exists()) {
            System.out.println("Livestock data file not found: " + LIVESTOCK_FILE);
            return;
        }

        livestockZones.clear();
        LivestockZone currentZone = null;
        Map<String, Double> legacyProductionTotals = new HashMap<>();

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] p = line.split("\\|", -1);

                switch (p[0]) {
                    case "ZONE":
                        currentZone = new LivestockZone(p[1], p[2]);
                        if ("ACTIVE".equals(p[3])) {
                            currentZone.activate();
                        } else if ("SUSPENDED".equals(p[3])) {
                            currentZone.suspend();
                        }
                        currentZone.setBounds(
                                FileNumbers.parse(p[4]), FileNumbers.parse(p[5]),
                                FileNumbers.parse(p[6]), FileNumbers.parse(p[7]));
                        if (p.length > 8 && !p[8].isEmpty() && !"null".equals(p[8]))
                            currentZone.setAllowedAnimalType(AnimalType.valueOf(p[8]));
                        livestockZones.add(currentZone);
                        System.out.println("  Loaded livestock zone: " + p[1]);
                        break;

                    case "FEED":
                        if (currentZone == null) break;
                        FeedingProgram fp = new FeedingProgram(
                                p[2], FileNumbers.parse(p[3]), Integer.parseInt(p[4]));
                        currentZone.setFeedingProgram(fp);
                        System.out.println("    Loaded feeding program: " + p[2]);
                        break;

                    case "ANIMAL":
                        if (currentZone == null) break;
                        Animal animal;
                        if ("RUMINANT".equals(p[7])) {
                            animal = new Ruminant(p[2], p[3],
                                    Integer.parseInt(p[4]), FileNumbers.parse(p[5]));
                        } else {
                            animal = new Poultry(p[2], p[3],
                                    Integer.parseInt(p[4]), FileNumbers.parse(p[5]));
                        }
                        animal.setHealthStatus(HealthStatus.valueOf(p[6]));
                        if (p.length > 8 && !p[8].isEmpty()) {
                            legacyProductionTotals.put(p[2], FileNumbers.parse(p[8]));
                        }
                        currentZone.addAnimal(animal);
                        System.out.println("    Loaded animal: " + p[2] + " - " + p[3]);
                        break;

                    case "PRODUCTION":
                        if (currentZone == null) break;
                        for (Animal a : currentZone.getAnimals()) {
                            if (a.getId().equals(p[1])) {
                                if (a instanceof Producing producing) {
                                    double amount = FileNumbers.parse(p[2]);
                                    LocalDateTime recordedAt = p.length > 3 && !p[3].isEmpty()
                                            ? LocalDateTime.parse(p[3], DT_FMT)
                                            : LocalDateTime.now();
                                    producing.recordProduction(amount, recordedAt);
                                }
                                break;
                            }
                        }
                        break;

                    case "HEALTH_EVENT":
                        if (currentZone == null) break;
                        for (Animal a : currentZone.getAnimals()) {
                            if (a.getId().equals(p[1])) {
                                a.logHealthEvent(p[2]);
                                break;
                            }
                        }
                        break;

                    case "SENSOR":
                        if (currentZone == null) break;
                        Sensor sensor = SensorSerializer.create(p);
                        if (sensor != null) {
                            currentZone.addSensor(sensor);
                            System.out.println("    Loaded sensor: " + p[2]);
                        }
                        break;

                    case "READING":
                        if (currentZone == null) break;
                        SensorSerializer.addReading(currentZone.getSensors(), p, DT_FMT);
                        break;

                    default:
                        System.out.println("  Unknown line type in livestock file: " + p[0]);
                }
            }
            for (LivestockZone zone : livestockZones) {
                for (Animal a : zone.getAnimals()) {
                    if (a instanceof Producing producing && producing.getProductionRecord().getProductions().isEmpty()) {
                        Double legacy = legacyProductionTotals.get(a.getId());
                        if (legacy != null && legacy > 0) {
                            producing.recordProduction(legacy, LocalDateTime.now());
                        }
                    }
                }
            }
            System.out.println("Livestock zones loaded: " + livestockZones.size());
        } catch (IOException e) {
            System.err.println("Error loading livestock zones: " + e.getMessage());
        }
    }
}
