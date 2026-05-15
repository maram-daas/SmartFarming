package model.utils;

import model.enums.*;
import model.zones.*;
import model.sensors.*;
import model.crops.*;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Handles loading and saving of CropZone data to/from data/crop_zones.txt
 */
public class CropDataManager {

    static final String CROP_FILE = "data/crop_zones.txt";
    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // -------------------------------------------------------------------------
    // SAVE
    // -------------------------------------------------------------------------

    public static void save(List<CropZone> cropZones) {
        try (PrintWriter w = new PrintWriter(new FileWriter(CROP_FILE))) {
            w.println("#CROP_ZONES");
            for (CropZone zone : cropZones) {
                w.printf("ZONE|%s|%s|%s|%f|%f|%f|%f|%s%n",
                        zone.getCode(), zone.getName(), zone.getStatus(),
                        zone.getBoundNorth(), zone.getBoundSouth(),
                        zone.getBoundEast(), zone.getBoundWest(),
                        zone.getAllowedCropFamily());

                for (Crop crop : zone.getCrops()) {
                    w.printf("CROP|%s|%s|%s|%s|%s|%s|%f|%f|%f|%f%n",
                            zone.getCode(), crop.getName(), crop.getFamily(),
                            crop.getPlantingDate().format(DATE_FMT),
                            crop.getExpectedHarvestDate().format(DATE_FMT),
                            crop.getGrowthStage(),
                            crop.getOptimalPHMin(), crop.getOptimalPHMax(),
                            crop.getOptimalMoistureMin(), crop.getOptimalMoistureMax());
                }

                for (Sensor sensor : zone.getSensors()) {
                    SensorSerializer.write(w, zone.getCode(), sensor, DT_FMT);
                }
            }
            System.out.println("Crop zones saved to " + CROP_FILE);
        } catch (IOException e) {
            System.err.println("Error saving crop zones: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // LOAD
    // -------------------------------------------------------------------------

    public static void load(List<CropZone> cropZones) {
        File file = new File(CROP_FILE);
        if (!file.exists()) {
            System.out.println("Crop data file not found: " + CROP_FILE);
            return;
        }

        cropZones.clear();
        CropZone currentZone = null;

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] p = line.split("\\|", -1);

                switch (p[0]) {
                    case "ZONE":
                        currentZone = new CropZone(p[1], p[2]);
                        if ("ACTIVE".equals(p[3])) currentZone.activate();
                        currentZone.setBounds(
                                Double.parseDouble(p[4]), Double.parseDouble(p[5]),
                                Double.parseDouble(p[6]), Double.parseDouble(p[7]));
                        if (p.length > 8 && !p[8].isEmpty() && !"null".equals(p[8]))
                            currentZone.setAllowedCropFamily(CropFamily.valueOf(p[8]));
                        cropZones.add(currentZone);
                        System.out.println("  Loaded crop zone: " + p[1]);
                        break;

                    case "CROP":
                        if (currentZone == null) break;
                        Crop crop = new Crop(
                                p[2],
                                CropFamily.valueOf(p[3]),
                                LocalDate.parse(p[4], DATE_FMT),
                                LocalDate.parse(p[5], DATE_FMT),
                                Double.parseDouble(p[7]),
                                Double.parseDouble(p[8]),
                                Double.parseDouble(p[9]),
                                Double.parseDouble(p[10])
                        );
                        crop.setGrowthStage(GrowthStage.valueOf(p[6]));
                        currentZone.addCrop(crop);
                        System.out.println("    Loaded crop: " + p[2]);
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
                        System.out.println("  Unknown line type in crop file: " + p[0]);
                }
            }
            System.out.println("Crop zones loaded: " + cropZones.size());
        } catch (IOException e) {
            System.err.println("Error loading crop zones: " + e.getMessage());
        }
    }
}
