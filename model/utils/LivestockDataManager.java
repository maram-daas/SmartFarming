package model.utils;

import model.entities.FeedingProgram;
import model.enums.*;
import model.zones.*;
import model.sensors.*;
import model.animals.*;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
                w.printf("ZONE|%s|%s|%s|%f|%f|%f|%f|%s%n",
                        zone.getCode(), zone.getName(), zone.getStatus(),
                        zone.getBoundNorth(), zone.getBoundSouth(),
                        zone.getBoundEast(), zone.getBoundWest(),
                        zone.getAllowedAnimalType());

                if (zone.getFeedingProgram() != null) {
                    w.printf("FEED|%s|%s|%f|%d%n",
                            zone.getCode(), zone.getFeedingProgram().getFeedType(),
                            zone.getFeedingProgram().getQuantityPerMeal(),
                            zone.getFeedingProgram().getMealsPerDay());
                }

                for (Animal animal : zone.getAnimals()) {
                    w.printf("ANIMAL|%s|%s|%s|%d|%f|%s",
                            zone.getCode(), animal.getId(), animal.getSpecies(),
                            animal.getAge(), animal.getWeight(), animal.getHealthStatus());
                    if (animal instanceof Ruminant) {
                        w.printf("|RUMINANT|%f%n", ((Ruminant) animal).getMilkYield());
                    } else if (animal instanceof Poultry) {
                        w.printf("|POULTRY|%d%n", ((Poultry) animal).getEggCount());
                    } else {
                        w.println();
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

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] p = line.split("\\|", -1);

                switch (p[0]) {
                    case "ZONE":
                        currentZone = new LivestockZone(p[1], p[2]);
                        if ("ACTIVE".equals(p[3])) currentZone.activate();
                        currentZone.setBounds(
                                Double.parseDouble(p[4]), Double.parseDouble(p[5]),
                                Double.parseDouble(p[6]), Double.parseDouble(p[7]));
                        if (p.length > 8 && !p[8].isEmpty() && !"null".equals(p[8]))
                            currentZone.setAllowedAnimalType(AnimalType.valueOf(p[8]));
                        livestockZones.add(currentZone);
                        System.out.println("  Loaded livestock zone: " + p[1]);
                        break;

                    case "FEED":
                        if (currentZone == null) break;
                        FeedingProgram fp = new FeedingProgram(
                                p[2], Double.parseDouble(p[3]), Integer.parseInt(p[4]));
                        currentZone.setFeedingProgram(fp);
                        System.out.println("    Loaded feeding program: " + p[2]);
                        break;

                    case "ANIMAL":
                        if (currentZone == null) break;
                        Animal animal;
                        if ("RUMINANT".equals(p[7])) {
                            animal = new Ruminant(p[2], p[3],
                                    Integer.parseInt(p[4]), Double.parseDouble(p[5]));
                            if (p.length > 8 && !p[8].isEmpty())
                                ((Ruminant) animal).addMilkYield(Double.parseDouble(p[8]));
                        } else {
                            animal = new Poultry(p[2], p[3],
                                    Integer.parseInt(p[4]), Double.parseDouble(p[5]));
                            if (p.length > 8 && !p[8].isEmpty())
                                ((Poultry) animal).addEggs(Integer.parseInt(p[8]));
                        }
                        animal.setHealthStatus(HealthStatus.valueOf(p[6]));
                        currentZone.addAnimal(animal);
                        System.out.println("    Loaded animal: " + p[2] + " - " + p[3]);
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
            System.out.println("Livestock zones loaded: " + livestockZones.size());
        } catch (IOException e) {
            System.err.println("Error loading livestock zones: " + e.getMessage());
        }
    }
}
