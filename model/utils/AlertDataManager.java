package model.utils;

import model.entities.*;
import model.enums.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Handles loading and saving of Alert history to/from data/alert_history.txt
 */
public class AlertDataManager {

    static final String ALERT_FILE = "data/alert_history.txt";

    /**
     * Primary formatter: full seconds (what we always write).
     * Fallback formatter: no-seconds variant (e.g. "2026-01-14T15:30") that
     * older data or LocalDateTime.toString() may have produced.
     */
    static final DateTimeFormatter DT_FMT          = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    static final DateTimeFormatter DT_FMT_NO_SECS  = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // -------------------------------------------------------------------------
    // SAVE
    // -------------------------------------------------------------------------

    public static void save(List<Alert> alertHistory) {
        try (PrintWriter w = new PrintWriter(new FileWriter(ALERT_FILE))) {
            w.println("#ALERT_HISTORY");
            for (Alert alert : alertHistory) {
                // Always format with DT_FMT so the file is consistent and re-parseable.
                w.printf("ALERT|%s|%s|%f|%f|%f|%s|%s|%b|%b%n",
                        alert.getId(),
                        alert.getSensorCode(),
                        alert.getReadingValue(),
                        alert.getThresholdMin(),
                        alert.getThresholdMax(),
                        alert.getSeverity(),
                        alert.getTimestamp().format(DT_FMT),   // BUG FIX: was .toString()
                        alert.isAcknowledged(),
                        alert.isDismissed());
            }
            System.out.println("Alert history saved to " + ALERT_FILE);
        } catch (IOException e) {
            System.err.println("Error saving alert history: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // LOAD
    // -------------------------------------------------------------------------

    public static void load(List<Alert> alertHistory) {
        File file = new File(ALERT_FILE);
        if (!file.exists()) {
            System.out.println("Alert history file not found: " + ALERT_FILE);
            return;
        }

        alertHistory.clear();

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] p = line.split("\\|", -1);
                if (!"ALERT".equals(p[0]) || p.length < 10) {
                    System.out.println("  Skipping malformed alert line: " + line);
                    continue;
                }

                try {
                    LocalDateTime ts = parseDateTime(p[7]);
                    Alert alert = new Alert(
                            p[1], p[2],
                            Double.parseDouble(p[3]),
                            Double.parseDouble(p[4]),
                            Double.parseDouble(p[5]),
                            SeverityLevel.valueOf(p[6]),
                            ts
                    );
                    if (Boolean.parseBoolean(p[8])) alert.acknowledge();
                    if (Boolean.parseBoolean(p[9])) alert.dismiss();
                    alertHistory.add(alert);
                    System.out.println("  Loaded alert: " + p[1]);
                } catch (Exception e) {
                    System.err.println("  Error parsing alert line: " + line + " – " + e.getMessage());
                }
            }
            System.out.println("Alerts loaded: " + alertHistory.size());
        } catch (IOException e) {
            System.err.println("Error loading alert history: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    /** Tries full-seconds format first, falls back to no-seconds format. */
    static LocalDateTime parseDateTime(String raw) {
        try {
            return LocalDateTime.parse(raw, DT_FMT);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(raw, DT_FMT_NO_SECS);
        }
    }
}
