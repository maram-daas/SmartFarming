package model.utils;

import model.enums.*;
import model.zones.*;
import model.sensors.*;
import model.crops.*;
import model.animals.*;
import model.entities.*;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataManager {
    private static final String DATA_FILE = "data/farm_data.txt";
    private static final String READINGS_FILE = "data/sensor_readings.txt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static void saveAllData(List<CropZone> cropZones, List<LivestockZone> livestockZones,
                                   List<AquacultureZone> aquacultureZones, List<Alert> alertHistory) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
            writer.println("#CROP_ZONES");
            for (CropZone zone : cropZones) {
                writer.printf("ZONE|%s|%s|%s|%f|%f|%f|%f|%s%n",
                        zone.getCode(), zone.getName(), zone.getStatus(),
                        zone.getBoundNorth(), zone.getBoundSouth(),
                        zone.getBoundEast(), zone.getBoundWest(),
                        zone.getAllowedCropFamily());
                for (Crop crop : zone.getCrops()) {
                    writer.printf("CROP|%s|%s|%s|%s|%s|%s|%f|%f|%f|%f%n",
                            zone.getCode(), crop.getName(), crop.getFamily(),
                            crop.getPlantingDate(), crop.getExpectedHarvestDate(),
                            crop.getGrowthStage(),
                            crop.getOptimalPHMin(), crop.getOptimalPHMax(),
                            crop.getOptimalMoistureMin(), crop.getOptimalMoistureMax());
                }
                for (Sensor sensor : zone.getSensors()) {
                    saveSensor(writer, zone.getCode(), sensor);
                }
            }
            writer.println("#LIVESTOCK_ZONES");
            for (LivestockZone zone : livestockZones) {
                writer.printf("ZONE|%s|%s|%s|%f|%f|%f|%f|%s%n",
                        zone.getCode(), zone.getName(), zone.getStatus(),
                        zone.getBoundNorth(), zone.getBoundSouth(),
                        zone.getBoundEast(), zone.getBoundWest(),
                        zone.getAllowedAnimalType());
                if (zone.getFeedingProgram() != null) {
                    writer.printf("FEED|%s|%s|%f|%d%n",
                            zone.getCode(), zone.getFeedingProgram().getFeedType(),
                            zone.getFeedingProgram().getQuantityPerMeal(),
                            zone.getFeedingProgram().getMealsPerDay());
                }
                for (Animal animal : zone.getAnimals()) {
                    writer.printf("ANIMAL|%s|%s|%s|%d|%f|%s",
                            zone.getCode(), animal.getId(), animal.getSpecies(),
                            animal.getAge(), animal.getWeight(), animal.getHealthStatus());
                    if (animal instanceof Ruminant) {
                        writer.printf("|RUMINANT|%f%n", ((Ruminant) animal).getMilkYield());
                    } else if (animal instanceof Poultry) {
                        writer.printf("|POULTRY|%d%n", ((Poultry) animal).getEggCount());
                    }
                    for (String event : animal.getHealthEvents()) {
                        writer.printf("HEALTH_EVENT|%s|%s%n", animal.getId(), event);
                    }
                }
                for (Sensor sensor : zone.getSensors()) {
                    saveSensor(writer, zone.getCode(), sensor);
                }
            }
            writer.println("#AQUACULTURE_ZONES");
            for (AquacultureZone zone : aquacultureZones) {
                writer.printf("ZONE|%s|%s|%s|%f|%f|%f|%f%n",
                        zone.getCode(), zone.getName(), zone.getStatus(),
                        zone.getBoundNorth(), zone.getBoundSouth(),
                        zone.getBoundEast(), zone.getBoundWest());
                writer.printf("AQUA_COUNT|%s|%d%n", zone.getCode(), zone.getAnimalCount());
                for (String species : zone.getSpecies()) {
                    writer.printf("AQUA_SPECIES|%s|%s%n", zone.getCode(), species);
                }
                if (zone.getFeedingProgram() != null) {
                    writer.printf("FEED|%s|%s|%f|%d%n",
                            zone.getCode(), zone.getFeedingProgram().getFeedType(),
                            zone.getFeedingProgram().getQuantityPerMeal(),
                            zone.getFeedingProgram().getMealsPerDay());
                }
                for (Sensor sensor : zone.getSensors()) {
                    saveSensor(writer, zone.getCode(), sensor);
                }
            }
            writer.println("#ALERT_HISTORY");
            for (Alert alert : alertHistory) {
                writer.printf("ALERT|%s|%s|%f|%f|%f|%s|%s|%b|%b%n",
                        alert.getId(), alert.getSensorCode(), alert.getReadingValue(),
                        alert.getThresholdMin(), alert.getThresholdMax(),
                        alert.getSeverity(), alert.getTimestamp(),
                        alert.isAcknowledged(), alert.isDismissed());
            }
            System.out.println("Data saved to " + DATA_FILE);
        } catch (IOException e) {
            System.err.println("Error saving: " + e.getMessage());
        }
    }

    public static List<Alert> processReadingsFile(List<Zone> allZones, List<Alert> activeAlerts, List<Alert> alertHistory) {
        List<Alert> newAlerts = new ArrayList<>();
        File file = new File(READINGS_FILE);
        if (!file.exists()) {
            return newAlerts;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            List<String> processedLines = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.trim().split("\\|");
                if (parts.length < 2) continue;

                String sensorCode = parts[0];
                Sensor sensor = findSensorByCode(allZones, sensorCode);

                if (sensor == null) {
                    System.out.println("Sensor not found: " + sensorCode);
                    processedLines.add(line);
                    continue;
                }

                if (sensor.getStatus() != SensorStatus.ACTIVE) {
                    System.out.println("Sensor " + sensorCode + " is not active");
                    processedLines.add(line);
                    continue;
                }

                try {
                    // GPS Sensor: format = sensorCode|latitude|longitude|timestamp
                    if (sensor instanceof GPSSensor && parts.length >= 4) {
                        double latitude = Double.parseDouble(parts[1]);
                        double longitude = Double.parseDouble(parts[2]);
                        LocalDateTime timestamp = LocalDateTime.parse(parts[3], DATETIME_FORMATTER);

                        Position pos = new Position(latitude, longitude);
                        Reading reading = new Reading(sensorCode, 0, sensor.getUnit(), timestamp, pos);
                        ((GPSSensor) sensor).setLastPosition(pos);
                        sensor.addReading(reading);

                        Zone zone = findZoneForSensor(allZones, sensor);
                        if (zone != null) {
                            ((GPSSensor) sensor).setAssignedZone(zone);
                            if (!((GPSSensor) sensor).isWithinZoneBounds()) {
                                Alert alert = new Alert(
                                        "ALT" + System.currentTimeMillis(),
                                        sensorCode, 0, sensor.getThresholdMin(), sensor.getThresholdMax(),
                                        SeverityLevel.CRITICAL, timestamp
                                );
                                newAlerts.add(alert);
                                activeAlerts.add(alert);
                                alertHistory.add(alert);
                                System.out.println("GPS Alert: Animal left zone!");
                            }
                        }
                    }
                    // Regular Sensor: format = sensorCode|value|timestamp
                    else if (parts.length >= 3) {
                        double value = Double.parseDouble(parts[1]);
                        LocalDateTime timestamp = LocalDateTime.parse(parts[2], DATETIME_FORMATTER);

                        Reading reading = new Reading(sensorCode, value, sensor.getUnit(), timestamp);
                        sensor.addReading(reading);

                        if (value < sensor.getThresholdMin() || value > sensor.getThresholdMax()) {
                            SeverityLevel severity = SeverityLevel.WARNING;
                            if (value < sensor.getThresholdMin() * 0.7 || value > sensor.getThresholdMax() * 1.3) {
                                severity = SeverityLevel.CRITICAL;
                            }
                            Alert alert = new Alert(
                                    "ALT" + System.currentTimeMillis() + "_" + new Random().nextInt(1000),
                                    sensorCode, value, sensor.getThresholdMin(), sensor.getThresholdMax(),
                                    severity, timestamp
                            );
                            newAlerts.add(alert);
                            activeAlerts.add(alert);
                            alertHistory.add(alert);
                            System.out.println("Threshold Alert: " + sensorCode + " = " + value);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line + " - " + e.getMessage());
                }
                processedLines.add(line);
            }

            if (!processedLines.isEmpty()) {
                File archiveDir = new File("data/archive");
                archiveDir.mkdirs();
                File processedFile = new File(archiveDir, "readings_processed_" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt");
                try (PrintWriter writer = new PrintWriter(new FileWriter(processedFile))) {
                    for (String l : processedLines) {
                        writer.println(l);
                    }
                }
                new PrintWriter(new FileWriter(READINGS_FILE)).close();
                System.out.println("Processed " + processedLines.size() + " readings");
            }
        } catch (IOException e) {
            System.err.println("Error processing readings: " + e.getMessage());
        }
        return newAlerts;
    }

    private static Sensor findSensorByCode(List<Zone> allZones, String code) {
        for (Zone zone : allZones) {
            for (Sensor sensor : zone.getSensors()) {
                if (sensor.getCode().equals(code)) return sensor;
            }
        }
        return null;
    }

    private static Zone findZoneForSensor(List<Zone> allZones, Sensor sensor) {
        for (Zone zone : allZones) {
            if (zone.getSensors().contains(sensor)) return zone;
        }
        return null;
    }

    private static void saveSensor(PrintWriter writer, String zoneCode, Sensor sensor) {
        String type = sensor.getClass().getSimpleName();
        writer.printf("SENSOR|%s|%s|%s|%f|%f|%s",
                zoneCode, sensor.getCode(), type, sensor.getThresholdMin(),
                sensor.getThresholdMax(), sensor.getStatus());
        if (sensor instanceof EnvironmentSensor) {
            writer.printf("|%s%n", ((EnvironmentSensor) sensor).getMeasurementType());
        } else if (sensor instanceof SoilSensor) {
            writer.printf("|%s%n", ((SoilSensor) sensor).getMeasurementType());
        } else if (sensor instanceof BiometricSensor) {
            writer.printf("|%s|%s%n", ((BiometricSensor) sensor).getAnimalId(),
                    ((BiometricSensor) sensor).getMeasurementType());
        } else if (sensor instanceof WaterSensor) {
            writer.printf("|%s%n", ((WaterSensor) sensor).getMeasurementType());
        } else if (sensor instanceof GPSSensor) {
            writer.printf("|%s%n", ((GPSSensor) sensor).getAnimalId());
        } else {
            writer.println();
        }
        for (Reading reading : sensor.getReadings()) {
            writer.printf("READING|%s|%f|%s|%s%n",
                    sensor.getCode(), reading.getValue(), reading.getUnit(),
                    reading.getTimestamp().format(DATETIME_FORMATTER));
        }
    }

    public static void loadAllData(List<CropZone> cropZones, List<LivestockZone> livestockZones,
                                   List<AquacultureZone> aquacultureZones, List<Alert> alertHistory) {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            System.out.println("Data file not found, will use sample data");
            return;
        }

        // Clear existing data
        cropZones.clear();
        livestockZones.clear();
        aquacultureZones.clear();
        alertHistory.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String currentSection = "";
            CropZone currentCropZone = null;
            LivestockZone currentLivestockZone = null;
            AquacultureZone currentAquaZone = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    currentSection = line.trim();
                    System.out.println("Loading section: " + currentSection);
                    continue;
                }
                if (line.trim().isEmpty()) continue;

                String[] parts = line.trim().split("\\|");

                switch (currentSection) {
                    case "#CROP_ZONES":
                        if (parts[0].equals("ZONE")) {
                            currentCropZone = new CropZone(parts[1], parts[2]);
                            if (parts[3].equals("ACTIVE")) currentCropZone.activate();
                            if (parts.length > 7) {
                                currentCropZone.setBounds(
                                        Double.parseDouble(parts[4]), Double.parseDouble(parts[5]),
                                        Double.parseDouble(parts[6]), Double.parseDouble(parts[7]));
                            }
                            if (parts.length > 8 && !parts[8].equals("null")) {
                                currentCropZone.setAllowedCropFamily(CropFamily.valueOf(parts[8]));
                            }
                            cropZones.add(currentCropZone);
                            System.out.println("  Loaded crop zone: " + parts[1] + " - " + parts[2]);
                        }
                        else if (parts[0].equals("CROP") && currentCropZone != null) {
                            Crop crop = new Crop(
                                    parts[2], // name
                                    CropFamily.valueOf(parts[3]), // family
                                    LocalDate.parse(parts[4], DATE_FORMATTER), // planting date
                                    LocalDate.parse(parts[5], DATE_FORMATTER), // harvest date
                                    Double.parseDouble(parts[7]), // pH min
                                    Double.parseDouble(parts[8]), // pH max
                                    Double.parseDouble(parts[9]), // moisture min
                                    Double.parseDouble(parts[10])  // moisture max
                            );
                            crop.setGrowthStage(GrowthStage.valueOf(parts[6]));
                            currentCropZone.addCrop(crop);
                            System.out.println("    Loaded crop: " + parts[2]);
                        }
                        else if (parts[0].equals("SENSOR") && currentCropZone != null) {
                            Sensor sensor = createSensorFromData(parts);
                            if (sensor != null) {
                                currentCropZone.addSensor(sensor);
                                System.out.println("    Loaded sensor: " + parts[2]);
                            }
                        }
                        else if (parts[0].equals("READING") && currentCropZone != null) {
                            addReadingToZone(currentCropZone, parts);
                        }
                        break;

                    case "#LIVESTOCK_ZONES":
                        if (parts[0].equals("ZONE")) {
                            currentLivestockZone = new LivestockZone(parts[1], parts[2]);
                            if (parts[3].equals("ACTIVE")) currentLivestockZone.activate();
                            if (parts.length > 7) {
                                currentLivestockZone.setBounds(
                                        Double.parseDouble(parts[4]), Double.parseDouble(parts[5]),
                                        Double.parseDouble(parts[6]), Double.parseDouble(parts[7]));
                            }
                            if (parts.length > 8 && !parts[8].equals("null")) {
                                currentLivestockZone.setAllowedAnimalType(AnimalType.valueOf(parts[8]));
                            }
                            livestockZones.add(currentLivestockZone);
                            System.out.println("  Loaded livestock zone: " + parts[1] + " - " + parts[2]);
                        }
                        else if (parts[0].equals("FEED") && currentLivestockZone != null) {
                            FeedingProgram fp = new FeedingProgram(parts[2], Double.parseDouble(parts[3]), Integer.parseInt(parts[4]));
                            currentLivestockZone.setFeedingProgram(fp);
                            System.out.println("    Loaded feeding program: " + parts[2]);
                        }
                        else if (parts[0].equals("ANIMAL") && currentLivestockZone != null) {
                            Animal animal;
                            if (parts[7].equals("RUMINANT")) {
                                animal = new Ruminant(parts[2], parts[3], Integer.parseInt(parts[4]), Double.parseDouble(parts[5]));
                                if (parts.length > 8) {
                                    ((Ruminant) animal).addMilkYield(Double.parseDouble(parts[8]));
                                }
                            } else {
                                animal = new Poultry(parts[2], parts[3], Integer.parseInt(parts[4]), Double.parseDouble(parts[5]));
                                if (parts.length > 8) {
                                    ((Poultry) animal).addEggs(Integer.parseInt(parts[8]));
                                }
                            }
                            animal.setHealthStatus(HealthStatus.valueOf(parts[6]));
                            currentLivestockZone.addAnimal(animal);
                            System.out.println("    Loaded animal: " + parts[2] + " - " + parts[3]);
                        }
                        else if (parts[0].equals("HEALTH_EVENT") && currentLivestockZone != null) {
                            for (Animal animal : currentLivestockZone.getAnimals()) {
                                if (animal.getId().equals(parts[1])) {
                                    animal.logHealthEvent(parts[2]);
                                    break;
                                }
                            }
                        }
                        else if (parts[0].equals("SENSOR") && currentLivestockZone != null) {
                            Sensor sensor = createSensorFromData(parts);
                            if (sensor != null) {
                                currentLivestockZone.addSensor(sensor);
                                System.out.println("    Loaded sensor: " + parts[2]);
                            }
                        }
                        else if (parts[0].equals("READING") && currentLivestockZone != null) {
                            addReadingToZone(currentLivestockZone, parts);
                        }
                        break;

                    case "#AQUACULTURE_ZONES":
                        if (parts[0].equals("ZONE")) {
                            currentAquaZone = new AquacultureZone(parts[1], parts[2]);
                            if (parts[3].equals("ACTIVE")) currentAquaZone.activate();
                            if (parts.length > 7) {
                                currentAquaZone.setBounds(
                                        Double.parseDouble(parts[4]), Double.parseDouble(parts[5]),
                                        Double.parseDouble(parts[6]), Double.parseDouble(parts[7]));
                            }
                            aquacultureZones.add(currentAquaZone);
                            System.out.println("  Loaded aquaculture zone: " + parts[1] + " - " + parts[2]);
                        }
                        else if (parts[0].equals("AQUA_COUNT") && currentAquaZone != null) {
                            currentAquaZone.setAnimalCount(Integer.parseInt(parts[2]));
                            System.out.println("    Loaded animal count: " + parts[2]);
                        }
                        else if (parts[0].equals("AQUA_SPECIES") && currentAquaZone != null) {
                            currentAquaZone.addSpecies(parts[2]);
                            System.out.println("    Loaded species: " + parts[2]);
                        }
                        else if (parts[0].equals("FEED") && currentAquaZone != null) {
                            FeedingProgram fp = new FeedingProgram(parts[2], Double.parseDouble(parts[3]), Integer.parseInt(parts[4]));
                            currentAquaZone.setFeedingProgram(fp);
                            System.out.println("    Loaded feeding program: " + parts[2]);
                        }
                        else if (parts[0].equals("SENSOR") && currentAquaZone != null) {
                            Sensor sensor = createSensorFromData(parts);
                            if (sensor != null) {
                                currentAquaZone.addSensor(sensor);
                                System.out.println("    Loaded sensor: " + parts[2]);
                            }
                        }
                        else if (parts[0].equals("READING") && currentAquaZone != null) {
                            addReadingToZone(currentAquaZone, parts);
                        }
                        break;

                    case "#ALERT_HISTORY":
                        if (parts[0].equals("ALERT")) {
                            Alert alert = new Alert(
                                    parts[1], parts[2], Double.parseDouble(parts[3]),
                                    Double.parseDouble(parts[4]), Double.parseDouble(parts[5]),
                                    SeverityLevel.valueOf(parts[6]), LocalDateTime.parse(parts[7], DATETIME_FORMATTER)
                            );
                            if (Boolean.parseBoolean(parts[8])) alert.acknowledge();
                            if (parts.length > 9 && Boolean.parseBoolean(parts[9])) alert.dismiss();
                            alertHistory.add(alert);
                            System.out.println("  Loaded alert: " + parts[1]);
                        }
                        break;
                }
            }
            System.out.println("Data loaded successfully from " + DATA_FILE);
            System.out.println("  Crop Zones: " + cropZones.size());
            System.out.println("  Livestock Zones: " + livestockZones.size());
            System.out.println("  Aquaculture Zones: " + aquacultureZones.size());
            System.out.println("  Alerts: " + alertHistory.size());
        } catch (IOException e) {
            System.err.println("Error loading: " + e.getMessage());
        }
    }

    private static Sensor createSensorFromData(String[] parts) {
        try {
            String zoneCode = parts[1];
            String code = parts[2];
            String type = parts[3];
            double min = Double.parseDouble(parts[4]);
            double max = Double.parseDouble(parts[5]);
            SensorStatus status = SensorStatus.valueOf(parts[6]);

            Sensor sensor = null;
            switch (type) {
                case "EnvironmentSensor":
                    String envType = parts.length > 7 ? parts[7] : "temperature";
                    sensor = new EnvironmentSensor(code, zoneCode, min, max, envType);
                    break;
                case "SoilSensor":
                    String soilType = parts.length > 7 ? parts[7] : "ph";
                    sensor = new SoilSensor(code, zoneCode, min, max, soilType);
                    break;
                case "BiometricSensor":
                    String animalId = parts.length > 7 ? parts[7] : "UNKNOWN";
                    String bioType = parts.length > 8 ? parts[8] : "temperature";
                    sensor = new BiometricSensor(code, zoneCode, min, max, animalId, bioType);
                    break;
                case "WaterSensor":
                    String waterType = parts.length > 7 ? parts[7] : "temperature";
                    sensor = new WaterSensor(code, zoneCode, min, max, waterType);
                    break;
                case "GPSSensor":
                    String gpsAnimalId = parts.length > 7 ? parts[7] : "UNKNOWN";
                    sensor = new GPSSensor(code, zoneCode, min, max, gpsAnimalId);
                    break;
            }
            if (sensor != null) {
                sensor.setStatus(status);
            }
            return sensor;
        } catch (Exception e) {
            System.err.println("Error creating sensor: " + e.getMessage());
            return null;
        }
    }

    private static void addReadingToZone(Zone zone, String[] parts) {
        try {
            String sensorCode = parts[1];
            double value = Double.parseDouble(parts[2]);
            String unit = parts[3];
            LocalDateTime timestamp = LocalDateTime.parse(parts[4], DATETIME_FORMATTER);

            for (Sensor sensor : zone.getSensors()) {
                if (sensor.getCode().equals(sensorCode)) {
                    Reading reading = new Reading(sensorCode, value, unit, timestamp);
                    sensor.addReading(reading);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error adding reading: " + e.getMessage());
        }
    }

    public static void exportReportToCSV(List<CropZone> cropZones, List<LivestockZone> livestockZones,
                                         List<AquacultureZone> aquacultureZones, List<Alert> alertHistory,
                                         String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("FARM MANAGEMENT REPORT");
            writer.println("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER));
            writer.println();
            writer.println("CROP REPORT");
            writer.println("Zone,Code,Crop Name,Family,Growth Stage,Planting Date,Harvest Date");
            for (CropZone zone : cropZones) {
                for (Crop crop : zone.getCrops()) {
                    writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                            zone.getName(), zone.getCode(), crop.getName(), crop.getFamily(),
                            crop.getGrowthStage(), crop.getPlantingDate(), crop.getExpectedHarvestDate());
                }
            }
            writer.println();
            writer.println("LIVESTOCK REPORT");
            writer.println("Zone,Animal ID,Species,Type,Age,Weight,Health,Milk/Eggs");
            for (LivestockZone zone : livestockZones) {
                for (Animal animal : zone.getAnimals()) {
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
                        alert.getTimestamp(), alert.isAcknowledged());
            }
            System.out.println("Report exported to " + filePath);
        } catch (IOException e) {
            System.err.println("Error exporting: " + e.getMessage());
        }
    }
}