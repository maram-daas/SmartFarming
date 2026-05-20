package model.utils;

import model.entities.FeedingProgram;
import model.zones.*;
import model.sensors.*;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.List;


/**
 * Handles loading and saving of AquacultureZone data to/from data/aquaculture_zones.txt
 */
public class AquacultureDataManager {

    static final String AQUA_FILE = "data/aquaculture_zones.txt";
    static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // -------------------------------------------------------------------------
    // SAVE
    // -------------------------------------------------------------------------

    public static void save(List<AquacultureZone> aquacultureZones) {
        try (PrintWriter w = new PrintWriter(new FileWriter(AQUA_FILE))) {
            w.println("#AQUACULTURE_ZONES");
            for (AquacultureZone zone : aquacultureZones) {
                w.printf("ZONE|%s|%s|%s|%f|%f|%f|%f%n",
                        zone.getCode(), zone.getName(), zone.getStatus(),
                        zone.getBoundNorth(), zone.getBoundSouth(),
                        zone.getBoundEast(), zone.getBoundWest());
                w.printf("AQUA_COUNT|%s|%d%n", zone.getCode(), zone.getAnimalCount());
                for (String species : zone.getSpecies()) {
                    w.printf("AQUA_SPECIES|%s|%s%n", zone.getCode(), species);
                }
                if (zone.getFeedingProgram() != null) {
                    w.printf("FEED|%s|%s|%f|%d%n",
                            zone.getCode(), zone.getFeedingProgram().getFeedType(),
                            zone.getFeedingProgram().getQuantityPerMeal(),
                            zone.getFeedingProgram().getMealsPerDay());
                }
                for (Sensor sensor : zone.getSensors()) {
                    SensorSerializer.write(w, zone.getCode(), sensor, DT_FMT);
                }
            }
            System.out.println("Aquaculture zones saved to " + AQUA_FILE);
        } catch (IOException e) {
            System.err.println("Error saving aquaculture zones: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // LOAD
    // -------------------------------------------------------------------------

    public static void load(List<AquacultureZone> aquacultureZones) {
        File file = new File(AQUA_FILE);
        if (!file.exists()) {
            System.out.println("Aquaculture data file not found: " + AQUA_FILE);
            return;
        }

        aquacultureZones.clear();
        AquacultureZone currentZone = null;

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] p = line.split("\\|", -1);

                switch (p[0]) {
                    case "ZONE":
                        currentZone = new AquacultureZone(p[1], p[2]);
                        if ("ACTIVE".equals(p[3])) currentZone.activate();
                        currentZone.setBounds(
                                Double.parseDouble(p[4]), Double.parseDouble(p[5]),
                                Double.parseDouble(p[6]), Double.parseDouble(p[7]));
                        aquacultureZones.add(currentZone);
                        System.out.println("  Loaded aquaculture zone: " + p[1]);
                        break;

                    case "AQUA_COUNT":
                        if (currentZone == null) break;
                        // p[1] is zoneCode, p[2] is count
                        currentZone.setAnimalCount(Integer.parseInt(p[2]));
                        System.out.println("    Loaded animal count: " + p[2]);
                        break;

                    case "AQUA_SPECIES":
                        if (currentZone == null) break;
                        currentZone.addSpecies(p[2]);
                        System.out.println("    Loaded species: " + p[2]);
                        break;

                    case "FEED":
                        if (currentZone == null) break;
                        FeedingProgram fp = new FeedingProgram(
                                p[2], Double.parseDouble(p[3]), Integer.parseInt(p[4]));
                        currentZone.setFeedingProgram(fp);
                        System.out.println("    Loaded feeding program: " + p[2]);
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
                        System.out.println("  Unknown line type in aquaculture file: " + p[0]);
                }
            }
            System.out.println("Aquaculture zones loaded: " + aquacultureZones.size());
        } catch (IOException e) {
            System.err.println("Error loading aquaculture zones: " + e.getMessage());
        }
    }
}
