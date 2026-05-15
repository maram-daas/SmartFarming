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
     * @return              list of newly generated alerts (subset added to both lists above)
     */
    public static List<Alert> process(List<Zone> allZones,
                                      List<Alert> activeAlerts,
                                      List<Alert> alertHistory) {
        List<Alert> newAlerts = new ArrayList<>();
        File file = new File(READINGS_FILE);
        if (!file.exists() || file.length() == 0) {
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
                    System.out.println("Skipping malformed reading line: " + trimmed);
                    processedLines.add(trimmed); // discard bad lines; don't retry indefinitely
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
                    System.out.println("Sensor inactive, skipping: " + sensorCode);
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
                } else {
                    retryLines.add(trimmed);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading sensor_readings.txt: " + e.getMessage());
            return newAlerts;
        }

        // Archive processed lines, put retry lines back
        archiveProcessed(processedLines);
        rewrite(file, retryLines);

        System.out.println("Readings processed: " + processedLines.size()
                + ", kept for retry: " + retryLines.size());
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
                    Alert alert = createAlert(sensor.getCode(), 0,
                            sensor.getThresholdMin(), sensor.getThresholdMax(),
                            SeverityLevel.CRITICAL, ts);
                    newAlerts.add(alert);
                    activeAlerts.add(alert);
                    alertHistory.add(alert);
                    System.out.println("GPS Alert: animal left zone – " + sensor.getCode());
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

            if (value < sensor.getThresholdMin() || value > sensor.getThresholdMax()) {
                SeverityLevel severity =
                        (value < sensor.getThresholdMin() * 0.7 || value > sensor.getThresholdMax() * 1.3)
                                ? SeverityLevel.CRITICAL
                                : SeverityLevel.WARNING;

                Alert alert = createAlert(sensor.getCode(), value,
                        sensor.getThresholdMin(), sensor.getThresholdMax(),
                        severity, ts);
                newAlerts.add(alert);
                activeAlerts.add(alert);
                alertHistory.add(alert);
                System.out.println("Threshold Alert: " + sensor.getCode() + " = " + value
                        + " [" + sensor.getThresholdMin() + ", " + sensor.getThresholdMax() + "]");
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
        dir.mkdirs();
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File archive = new File(dir, "readings_" + stamp + ".txt");
        try (PrintWriter w = new PrintWriter(new FileWriter(archive))) {
            for (String l : lines) w.println(l);
        } catch (IOException e) {
            System.err.println("Error writing archive: " + e.getMessage());
        }
    }

    /** Rewrites the readings file with only the lines that still need retrying. */
    private static void rewrite(File file, List<String> retryLines) {
        try (PrintWriter w = new PrintWriter(new FileWriter(file))) {
            for (String l : retryLines) w.println(l);
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
    // DATE / ALERT HELPERS
    // -------------------------------------------------------------------------

    private static LocalDateTime parseDateTime(String raw) {
        try {
            return LocalDateTime.parse(raw, DT_FMT);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(raw, DT_FMT_NO_SECS);
        }
    }

    private static Alert createAlert(String sensorCode, double value,
                                     double min, double max,
                                     SeverityLevel severity, LocalDateTime ts) {
        String id = "ALT" + ts.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + new Random().nextInt(10000);
        return new Alert(id, sensorCode, value, min, max, severity, ts);
    }
}
