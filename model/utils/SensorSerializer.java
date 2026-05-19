package model.utils;

import model.entities.Reading;
import model.enums.*;
import model.sensors.*;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Shared helper for serialising/deserialising Sensor and Reading lines.
 * All DataManager classes delegate sensor I/O here to avoid duplication.
 */
public class SensorSerializer {

    // -------------------------------------------------------------------------
    // WRITE a sensor (and its historical readings) to an open PrintWriter
    // -------------------------------------------------------------------------

    public static void write(PrintWriter w, String zoneCode, Sensor sensor, DateTimeFormatter dtFmt) {
        String type = sensor.getClass().getSimpleName();

        // Base columns present for every sensor type
        w.printf("SENSOR|%s|%s|%s|%f|%f|%s",
                zoneCode, sensor.getCode(), type,
                sensor.getThresholdMin(), sensor.getThresholdMax(),
                sensor.getStatus());

        // Type-specific trailing columns
        if (sensor instanceof EnvironmentSensor) {
            w.printf("|%s%n", ((EnvironmentSensor) sensor).getMeasurementType());
        } else if (sensor instanceof SoilSensor) {
            w.printf("|%s%n", ((SoilSensor) sensor).getMeasurementType());
        } else if (sensor instanceof BiometricSensor) {
            w.printf("|%s|%s%n",
                    ((BiometricSensor) sensor).getAnimalId(),
                    ((BiometricSensor) sensor).getMeasurementType());
        } else if (sensor instanceof WaterSensor) {
            w.printf("|%s%n", ((WaterSensor) sensor).getMeasurementType());
        } else if (sensor instanceof GPSSensor) {
            w.printf("|%s%n", ((GPSSensor) sensor).getAnimalId());
        } else {
            w.println();
        }

        // Historical readings stored in the same file, immediately after the sensor
        for (Reading reading : sensor.getReadings()) {
            w.printf("READING|%s|%f|%s|%s%n",
                    sensor.getCode(),
                    reading.getValue(),
                    reading.getUnit(),
                    reading.getTimestamp().format(dtFmt));
        }
    }

    // -------------------------------------------------------------------------
    // CREATE a Sensor object from a parsed SENSOR line (p[0] == "SENSOR")
    // Format: SENSOR|zoneCode|code|type|min|max|status[|extra fields...]
    // -------------------------------------------------------------------------

    public static Sensor create(String[] p) {
        try {
            String zoneCode = p[1];
            String code     = p[2];
            String type     = p[3];
            double min      = Double.parseDouble(p[4]);
            double max      = Double.parseDouble(p[5]);
            SensorStatus status = SensorStatus.valueOf(p[6]);

            Sensor sensor;
            switch (type) {
                case "EnvironmentSensor":
                    sensor = new EnvironmentSensor(code, zoneCode, min, max,
                            p.length > 7 && !p[7].isEmpty() ? p[7] : "temperature");
                    break;
                case "SoilSensor":
                    sensor = new SoilSensor(code, zoneCode, min, max,
                            p.length > 7 && !p[7].isEmpty() ? p[7] : "ph");
                    break;
                case "BiometricSensor":
                    sensor = new BiometricSensor(code, zoneCode, min, max,
                            p.length > 7 && !p[7].isEmpty() ? p[7] : "UNKNOWN",
                            p.length > 8 && !p[8].isEmpty() ? p[8] : "temperature");
                    break;
                case "WaterSensor":
                    sensor = new WaterSensor(code, zoneCode, min, max,
                            p.length > 7 && !p[7].isEmpty() ? p[7] : "temperature");
                    break;
                case "GPSSensor":
                    sensor = new GPSSensor(code, zoneCode, min, max,
                            p.length > 7 && !p[7].isEmpty() ? p[7] : "UNKNOWN");
                    break;
                default:
                    System.err.println("Unknown sensor type: " + type);
                    return null;
            }
            sensor.setStatus(status);
            return sensor;

        } catch (Exception e) {
            System.err.println("Error creating sensor from line: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // ADD a reading from a parsed READING line to the matching sensor
    // Format: READING|sensorCode|value|unit|timestamp
    // -------------------------------------------------------------------------

    public static void addReading(List<Sensor> sensors, String[] p, DateTimeFormatter dtFmt) {
        if (p.length < 5) {
            System.err.println("Malformed READING line (need 5 fields): " + String.join("|", p));
            return;
        }
        try {
            String sensorCode   = p[1];
            double value        = Double.parseDouble(p[2]);
            String unit         = p[3];
            LocalDateTime ts    = LocalDateTime.parse(p[4], dtFmt);

            for (Sensor sensor : sensors) {
                if (sensor.getCode().equals(sensorCode)) {
                    sensor.addReading(new Reading(sensorCode, value, unit, ts));
                    return;
                }
            }
            System.out.println("    READING: sensor not found in zone – " + sensorCode);
        } catch (Exception e) {
            System.err.println("Error adding reading: " + e.getMessage());
        }
    }
}
