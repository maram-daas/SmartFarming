package model.utils;

import model.animals.Poultry;
import model.animals.Ruminant;
import model.zones.*;
import model.entities.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Facade that delegates to the individual data managers.
 *
 * Data is now split across four files instead of one monolithic file:
 *
 *   data/crop_zones.txt          – CropDataManager
 *   data/livestock_zones.txt     – LivestockDataManager
 *   data/aquaculture_zones.txt   – AquacultureDataManager
 *   data/alert_history.txt       – AlertDataManager
 *   data/sensor_readings.txt     – SensorReadingsProcessor  (incoming readings queue)
 *
 * Call DataManager.saveAllData() / DataManager.loadAllData() exactly as before;
 * the rest of your application code does not need to change.
 */
public class DataManager {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // -------------------------------------------------------------------------
    // SAVE  – writes all four domain files
    // -------------------------------------------------------------------------

    public static void saveAllData(List<CropZone> cropZones,
                                   List<LivestockZone> livestockZones,
                                   List<AquacultureZone> aquacultureZones,
                                   List<Alert> alertHistory) {
        ensureDataDirExists();
        CropDataManager.save(cropZones);
        LivestockDataManager.save(livestockZones);
        AquacultureDataManager.save(aquacultureZones);
        AlertDataManager.save(alertHistory);
        System.out.println("All data saved successfully.");
    }

    // -------------------------------------------------------------------------
    // LOAD  – reads all four domain files
    // -------------------------------------------------------------------------

    public static void loadAllData(List<CropZone> cropZones,
                                   List<LivestockZone> livestockZones,
                                   List<AquacultureZone> aquacultureZones,
                                   List<Alert> alertHistory) {
        CropDataManager.load(cropZones);
        LivestockDataManager.load(livestockZones);
        AquacultureDataManager.load(aquacultureZones);
        AlertDataManager.load(alertHistory);
        System.out.println("All data loaded. Crop=" + cropZones.size()
                + " Livestock=" + livestockZones.size()
                + " Aqua=" + aquacultureZones.size()
                + " Alerts=" + alertHistory.size());
    }

    // -------------------------------------------------------------------------
    // PROCESS READINGS  – unchanged public API, now delegated
    // -------------------------------------------------------------------------

    public static List<Alert> processReadingsFile(List<Zone> allZones,
                                                  List<Alert> activeAlerts,
                                                  List<Alert> alertHistory) {
        return SensorReadingsProcessor.process(allZones, activeAlerts, alertHistory);
    }

    // -------------------------------------------------------------------------
    // CSV EXPORT  – unchanged from original
    // -------------------------------------------------------------------------

    public static void exportReportToCSV(List<CropZone> cropZones,
                                         List<LivestockZone> livestockZones,
                                         List<AquacultureZone> aquacultureZones,
                                         List<Alert> alertHistory,
                                         String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("FARM MANAGEMENT REPORT");
            writer.println("Generated: " + LocalDateTime.now().format(DT_FMT));
            writer.println();

            writer.println("CROP REPORT");
            writer.println("Zone,Code,Crop Name,Family,Growth Stage,Planting Date,Harvest Date");
            for (CropZone zone : cropZones) {
                for (var crop : zone.getCrops()) {
                    writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                            zone.getName(), zone.getCode(), crop.getName(), crop.getFamily(),
                            crop.getGrowthStage(), crop.getPlantingDate(), crop.getExpectedHarvestDate());
                }
            }
            writer.println();

            writer.println("LIVESTOCK REPORT");
            writer.println("Zone,Animal ID,Species,Type,Age,Weight,Health,Milk/Eggs");
            for (LivestockZone zone : livestockZones) {
                for (var animal : zone.getAnimals()) {
                    String prod = "";
                    if (animal instanceof Ruminant) prod = ((Ruminant) animal).getMilkYield() + " L";
                    else if (animal instanceof Poultry) prod = ((Poultry) animal).getEggCount() + " eggs";
                    writer.printf("%s,%s,%s,%s,%d,%.1f,%s,%s%n",
                            zone.getName(), animal.getId(), animal.getSpecies(),
                            animal.getAnimalType(), animal.getAge(), animal.getWeight(),
                            animal.getHealthStatus(), prod);
                }
            }
            writer.println();

            writer.println("ALERT REPORT");
            writer.println("ID,Sensor,Value,Min,Max,Severity,Time,Acknowledged");
            for (Alert alert : alertHistory) {
                writer.printf("%s,%s,%.2f,%.2f,%.2f,%s,%s,%s%n",
                        alert.getId(), alert.getSensorCode(), alert.getReadingValue(),
                        alert.getThresholdMin(), alert.getThresholdMax(), alert.getSeverity(),
                        alert.getTimestamp().format(DT_FMT), alert.isAcknowledged());
            }
            System.out.println("Report exported to " + filePath);
        } catch (IOException e) {
            System.err.println("Error exporting report: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private static void ensureDataDirExists() {
        new File("data").mkdirs();
        new File("data/archive").mkdirs();
    }
}
