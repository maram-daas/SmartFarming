package model.utils;

import model.entities.*;
import model.enums.*;
import model.sensors.*;
import model.zones.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Processes incoming sensor readings from data/sensor_readings.txt.
 *
 * Expected line format (one reading per line, NO header keyword):
 *
 *   Regular sensor:  sensorCode|value|timestamp
 *   GPS sensor:      sensorCode|latitude|longitude|timestamp
 *
 * Readings that trigger threshold violations become Alerts.
 * Successfully processed lines are archived; lines with unknown/inactive
 * sensors are left in the file so they can be retried next cycle.
 */
public class SensorReadingsProcessor {

    static final String READINGS_FILE = "data/sensor_readings.txt";
    static final String ARCHIVE_DIR   = "data/archive";

    static final DateTimeFormatter DT_FMT         = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    static final DateTimeFormatter DT_FMT_NO_SECS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // -------------------------------------------------------------------------
    // PUBLIC ENTRY POINT
    // -------------------------------------------------------------------------

    /**
     * Reads sensor_readings.txt, attaches readings to sensors, generates alerts.
     *
     * @param allZones      all zones (crop + livestock + aquaculture cast to Zone)
     * @param activeAlerts  running list of active alerts (new alerts are appended)
     * @param alertHistory  full alert history (new alerts are appended)
     * @return              list of newly generated alerts
     */
    public static List<Alert> process(List<Zone> allZones,
                                      List<Alert> activeAlerts,
                                      List<Alert> alertHistory) {
        List<Alert> newAlerts = new ArrayList<>();
        File file = new File(READINGS_FILE);
        if (!file.exists() || file.length() == 0) {
            System.out.println("No readings file or file is empty");
            return newAlerts;
        }

        List<String> processedLines = new ArrayList<>();  // successfully handled → archive
        List<String> retryLines     = new ArrayList<>();  // unknown/inactive sensor → keep

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                String[] parts = trimmed.split("\\|", -1);
                if (parts.length < 3) {
                    System.out.println("Skipping malformed reading line (need at least 3 fields): " + trimmed);
                    processedLines.add(trimmed);
                    continue;
                }

                String sensorCode = parts[0];
                Sensor sensor = findSensor(allZones, sensorCode);

                if (sensor == null) {
                    System.out.println("Sensor not found, will retry: " + sensorCode);
                    retryLines.add(trimmed);
                    continue;
                }

                if (sensor.getStatus() != SensorStatus.ACTIVE) {
                    System.out.println("Sensor not active (" + sensor.getStatus() + "), skipping: " + sensorCode);
                    retryLines.add(trimmed);
                    continue;
                }

                boolean handled = false;

                // GPS format: sensorCode|latitude|longitude|timestamp  (4 fields)
                if (sensor instanceof GPSSensor && parts.length >= 4) {
                    handled = handleGPS((GPSSensor) sensor, parts,
                            allZones, newAlerts, activeAlerts, alertHistory);
                }
                // Regular value format: sensorCode|value|timestamp  (3 fields)
                else if (!(sensor instanceof GPSSensor) && parts.length >= 3) {
                    handled = handleValue(sensor, parts,
                            newAlerts, activeAlerts, alertHistory);
                } else {
                    System.err.println("Wrong field count for sensor " + sensorCode
                            + " (type=" + sensor.getClass().getSimpleName()
                            + ", fields=" + parts.length + ")");
                }

                if (handled) {
                    processedLines.add(trimmed);
                    System.out.println("  Processed reading for: " + sensorCode);
                } else {
                    retryLines.add(trimmed);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading sensor_readings.txt: " + e.getMessage());
            return newAlerts;
        }

        // Archive processed lines, put retry lines back
        if (!processedLines.isEmpty()) {
            archiveProcessed(processedLines);
            System.out.println("Archived " + processedLines.size() + " processed readings");
        }
        rewrite(file, retryLines);

        if (!retryLines.isEmpty()) {
            System.out.println("Kept " + retryLines.size() + " readings for retry");
        }

        if (!newAlerts.isEmpty()) {
            System.out.println("Generated " + newAlerts.size() + " new alerts!");
        }

        return newAlerts;
    }

    // -------------------------------------------------------------------------
    // HANDLERS
    // -------------------------------------------------------------------------

    /** Returns true if the GPS reading was parsed and attached successfully. */
    private static boolean handleGPS(GPSSensor sensor, String[] parts,
                                     List<Zone> allZones,
                                     List<Alert> newAlerts,
                                     List<Alert> activeAlerts,
                                     List<Alert> alertHistory) {
        try {
            double latitude  = Double.parseDouble(parts[1]);
            double longitude = Double.parseDouble(parts[2]);
            LocalDateTime ts = parseDateTime(parts[3]);

            Position pos = new Position(latitude, longitude);
            Reading reading = new Reading(sensor.getCode(), 0, sensor.getUnit(), ts, pos);
            sensor.setLastPosition(pos);
            sensor.addReading(reading);

            Zone zone = findZoneForSensor(allZones, sensor);
            if (zone != null) {
                sensor.setAssignedZone(zone);
                if (!sensor.isWithinZoneBounds()) {
                    String alertId = "ALT" + System.currentTimeMillis() + "_" + new Random().nextInt(10000);
                    Alert alert = new Alert(alertId, sensor.getCode(), 0,
                            sensor.getThresholdMin(), sensor.getThresholdMax(),
                            SeverityLevel.CRITICAL, ts);
                    newAlerts.add(alert);
                    activeAlerts.add(alert);
                    alertHistory.add(alert);
                    System.out.println("  GPS Alert: animal left zone! Lat=" + latitude + ", Lon=" + longitude);
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error parsing GPS reading: " + e.getMessage());
            return false;
        }
    }

    /** Returns true if the value reading was parsed and attached successfully. */
    private static boolean handleValue(Sensor sensor, String[] parts,
                                       List<Alert> newAlerts,
                                       List<Alert> activeAlerts,
                                       List<Alert> alertHistory) {
        try {
            double value     = Double.parseDouble(parts[1]);
            LocalDateTime ts = parseDateTime(parts[2]);

            Reading reading = new Reading(sensor.getCode(), value, sensor.getUnit(), ts);
            sensor.addReading(reading);

            System.out.println("  Added reading to " + sensor.getCode() + ": " + value + " " + sensor.getUnit());

            // Check threshold violation
            if (value < sensor.getThresholdMin() || value > sensor.getThresholdMax()) {
                SeverityLevel severity = SeverityLevel.WARNING;
                // Critical if more than 30% outside threshold
                if (value < sensor.getThresholdMin() * 0.7 || value > sensor.getThresholdMax() * 1.3) {
                    severity = SeverityLevel.CRITICAL;
                }

                String alertId = "ALT" + System.currentTimeMillis() + "_" + new Random().nextInt(10000);
                Alert alert = new Alert(alertId, sensor.getCode(), value,
                        sensor.getThresholdMin(), sensor.getThresholdMax(),
                        severity, ts);
                newAlerts.add(alert);
                activeAlerts.add(alert);
                alertHistory.add(alert);
                System.out.println("  ALERT: " + sensor.getCode() + " = " + value
                        + " (threshold: [" + sensor.getThresholdMin() + " - " + sensor.getThresholdMax() + "])");
                System.out.println("  Severity: " + severity);
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error parsing value reading: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // FILE HELPERS
    // -------------------------------------------------------------------------

    private static void archiveProcessed(List<String> lines) {
        if (lines.isEmpty()) return;
        File dir = new File(ARCHIVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File archive = new File(dir, "readings_processed_" + stamp + ".txt");
        try (PrintWriter w = new PrintWriter(new FileWriter(archive))) {
            for (String l : lines) {
                w.println(l);
            }
            System.out.println("  Archived to: " + archive.getName());
        } catch (IOException e) {
            System.err.println("Error writing archive: " + e.getMessage());
        }
    }

    /** Rewrites the readings file with only the lines that still need retrying. */
    private static void rewrite(File file, List<String> retryLines) {
        try (PrintWriter w = new PrintWriter(new FileWriter(file))) {
            // Write header comments
            w.println("# Sensor readings file - Format depends on sensor type");
            w.println("# Regular sensors: sensorCode|value|timestamp");
            w.println("# GPS sensors: sensorCode|latitude|longitude|timestamp");
            w.println("");
            for (String l : retryLines) {
                w.println(l);
            }
        } catch (IOException e) {
            System.err.println("Error rewriting sensor_readings.txt: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // LOOKUP HELPERS
    // -------------------------------------------------------------------------

    private static Sensor findSensor(List<Zone> allZones, String code) {
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

    // -------------------------------------------------------------------------
    // DATE HELPERS
    // -------------------------------------------------------------------------

    private static LocalDateTime parseDateTime(String raw) {
        try {
            return LocalDateTime.parse(raw, DT_FMT);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(raw, DT_FMT_NO_SECS);
            } catch (DateTimeParseException e2) {
                System.err.println("Failed to parse timestamp: " + raw);
                return LocalDateTime.now();
            }
        }
    }
}