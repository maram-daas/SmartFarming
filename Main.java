import javafx.beans.property.SimpleDoubleProperty;
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

import model.entities.Alert;
import model.utils.DataManager;
import model.zones.*;
import model.sensors.*;
import model.crops.*;
import model.animals.*;
import model.entities.*;
import model.enums.*;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.awt.Color.white;

public class Main extends Application {
    // Data storage
    private static ObservableList<CropZone> cropZones = FXCollections.observableArrayList();
    private static ObservableList<LivestockZone> livestockZones = FXCollections.observableArrayList();
    private static ObservableList<AquacultureZone> aquacultureZones = FXCollections.observableArrayList();
    private static ObservableList<model.entities.Alert> activeAlerts = FXCollections.observableArrayList();
    private static ObservableList<model.entities.Alert> alertHistory = FXCollections.observableArrayList();
    private static int alertCounter = 100;
    private static int zoneCounter = 1; //fatima zahra
    private static int sensorCounter = 1;
    private static int cropCounter = 1;

    private BorderPane mainLayout;
    private VBox sidebar;
    private StackPane contentArea;
    private Label currentPageTitle;

    // Color palette
    private final String PRIMARY_COLOR = "#2e7d32";
    private final String SECONDARY_COLOR = "#f5f5f5";
    private final String SIDEBAR_COLOR = "#708238";
    private final String DANGER_COLOR = "#c62828";
    private final String WARNING_COLOR = "#ff9800";
    private final String SUCCESS_COLOR = "#4caf50";

    private Timer readingCheckerTimer;
    private boolean isAutoCheckEnabled = true;
    private int checkIntervalSeconds = 30;

    // ==================== HELPER METHODS ====================

    private void showInfoDialog(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorDialog(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarningDialog(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showDeleteConfirmation(String itemType, String itemName, Runnable onConfirm) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete " + itemType);
        alert.setContentText("Are you sure you want to delete " + itemName + "? This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            onConfirm.run();
        }
    }

    private void startPeriodicReadingCheck() {
        readingCheckerTimer = new Timer(true);
        readingCheckerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isAutoCheckEnabled) {
                    javafx.application.Platform.runLater(() -> checkReadingsFile());
                }
            }
        }, 0, checkIntervalSeconds * 1000);
    }

    private void checkReadingsFile() {
        List<Zone> allZones = new ArrayList<>();
        allZones.addAll(cropZones);
        allZones.addAll(livestockZones);
        allZones.addAll(aquacultureZones);

        List<Alert> newAlerts = DataManager.processReadingsFile(allZones, activeAlerts, alertHistory);

        if (!newAlerts.isEmpty()) {
            showWarningDialog("New Alerts", newAlerts.size() + " new alerts were generated from sensor readings!");
            showDashboard();
        }
    }

    private void loadDataFromFile() {
        DataManager.loadAllData(cropZones, livestockZones, aquacultureZones, alertHistory);
        updateCountersFromData();
        syncAlerts();
    }

    private void syncAlerts() {
        Iterator<model.entities.Alert> it = activeAlerts.iterator();
        while (it.hasNext()) {
            model.entities.Alert a = it.next();
            if (a.isAcknowledged() || a.isDismissed()) {
                it.remove();
                if (!alertHistory.contains(a)) {
                    alertHistory.add(a);
                }
            }
        }
        saveAllData();
    }

    private void updateCountersFromData() {
        int maxZoneNum = 0;
        for (CropZone z : cropZones) {
            String code = z.getCode();
            if (code.length() >= 3) {
                try {
                    int num = Integer.parseInt(code.substring(2));
                    maxZoneNum = Math.max(maxZoneNum, num);
                } catch (NumberFormatException e) {}
            }
        }
        for (LivestockZone z : livestockZones) {
            String code = z.getCode();
            if (code.length() >= 3) {
                try {
                    int num = Integer.parseInt(code.substring(2));
                    maxZoneNum = Math.max(maxZoneNum, num);
                } catch (NumberFormatException e) {}
            }
        }
        for (AquacultureZone z : aquacultureZones) {
            String code = z.getCode();
            if (code.length() >= 3) {
                try {
                    int num = Integer.parseInt(code.substring(2));
                    maxZoneNum = Math.max(maxZoneNum, num);
                } catch (NumberFormatException e) {}
            }
        }
        zoneCounter = Math.max(zoneCounter, maxZoneNum + 1);

        int maxSensorNum = 0;
        for (Sensor s : getAllSensors()) {
            String code = s.getCode();
            if (code.length() >= 4) {
                try {
                    int num = Integer.parseInt(code.substring(4));
                    maxSensorNum = Math.max(maxSensorNum, num);
                } catch (NumberFormatException e) {}
            }
        }
        sensorCounter = Math.max(sensorCounter, maxSensorNum + 1);

        int maxAlertNum = 100;
        for (model.entities.Alert a : alertHistory) {
            String id = a.getId();
            if (id.length() >= 3) {
                try {
                    int num = Integer.parseInt(id.substring(3));
                    maxAlertNum = Math.max(maxAlertNum, num);
                } catch (NumberFormatException e) {}
            }
        }
        alertCounter = maxAlertNum + 1;

        int maxCropNum = 0;
        for (CropZone z : cropZones) {
            for (Crop c : z.getCrops()) {
                String name = c.getName();
                // Extract number from crop name if present
            }
        }
        cropCounter = Math.max(cropCounter, maxCropNum + 1);
    }

    private void saveAllData() {
        DataManager.saveAllData(cropZones, livestockZones, aquacultureZones, alertHistory);
    }

    // PAGINATED EXCEL REPORT WITH MULTIPLE SHEETS
    private void exportReport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialFileName("farm_report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // ==================== SHEET 1: CROP ZONES ====================
                writer.println("===== SHEET 1: CROP ZONES REPORT =====");
                writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println();
                writer.println("ZONE CODE,ZONE NAME,STATUS,ALLOWED FAMILY,CROP NAME,CROP FAMILY,GROWTH STAGE,PLANTING DATE,HARVEST DATE,pH MIN,pH MAX,MOISTURE MIN %,MOISTURE MAX %");

                for (CropZone zone : cropZones) {
                    if (zone.getCrops().isEmpty()) {
                        writer.printf("%s,%s,%s,%s,No crops,,,,,,,%n",
                                zone.getCode(), zone.getName(), zone.getStatus(),
                                zone.getAllowedCropFamily());
                    } else {
                        for (Crop crop : zone.getCrops()) {
                            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%.2f,%.2f,%.2f,%.2f%n",
                                    zone.getCode(), zone.getName(), zone.getStatus(),
                                    zone.getAllowedCropFamily(),
                                    crop.getName(), crop.getFamily(), crop.getGrowthStage(),
                                    crop.getPlantingDate(), crop.getExpectedHarvestDate(),
                                    crop.getOptimalPHMin(), crop.getOptimalPHMax(),
                                    crop.getOptimalMoistureMin(), crop.getOptimalMoistureMax());
                        }
                    }
                }

                // ==================== SHEET 2: LIVESTOCK ZONES ====================
                writer.println();
                writer.println("===== SHEET 2: LIVESTOCK ZONES REPORT =====");
                writer.println();
                writer.println("ZONE CODE,ZONE NAME,STATUS,ALLOWED TYPE,ANIMAL ID,SPECIES,TYPE,AGE,YEARS,WEIGHT KG,HEALTH STATUS,PRODUCTION");

                for (LivestockZone zone : livestockZones) {
                    if (zone.getAnimals().isEmpty()) {
                        writer.printf("%s,%s,%s,%s,No animals,,,,,,,%n",
                                zone.getCode(), zone.getName(), zone.getStatus(),
                                zone.getAllowedAnimalType());
                    } else {
                        for (Animal animal : zone.getAnimals()) {
                            String production = "";
                            if (animal instanceof Ruminant) production = ((Ruminant) animal).getMilkYield() + " L";
                            else if (animal instanceof Poultry) production = ((Poultry) animal).getEggCount() + " eggs";
                            writer.printf("%s,%s,%s,%s,%s,%s,%s,%d,%.1f,%s,%s%n",
                                    zone.getCode(), zone.getName(), zone.getStatus(),
                                    zone.getAllowedAnimalType(),
                                    animal.getId(), animal.getSpecies(), animal.getAnimalType(),
                                    animal.getAge(), animal.getWeight(), animal.getHealthStatus(), production);
                        }
                    }
                    if (zone.getFeedingProgram() != null) {
                        writer.printf("FEEDING PROGRAM,%s,%s,%.1f kg/meal,%d meals/day,%.1f kg/day%n",
                                zone.getCode(), zone.getFeedingProgram().getFeedType(),
                                zone.getFeedingProgram().getQuantityPerMeal(),
                                zone.getFeedingProgram().getMealsPerDay(),
                                zone.getFeedingProgram().getDailyQuantity());
                    }
                }

                // ==================== SHEET 3: AQUACULTURE ZONES ====================
                writer.println();
                writer.println("===== SHEET 3: AQUACULTURE ZONES REPORT =====");
                writer.println();
                writer.println("ZONE CODE,ZONE NAME,STATUS,FISH COUNT,SPECIES,FEED TYPE,DAILY FEED KG");

                for (AquacultureZone zone : aquacultureZones) {
                    String speciesList = String.join("; ", zone.getSpecies());
                    String feedInfo = "";
                    if (zone.getFeedingProgram() != null) {
                        feedInfo = zone.getFeedingProgram().getFeedType() + "," + zone.getFeedingProgram().getDailyQuantity();
                    }
                    writer.printf("%s,%s,%s,%d,%s,%s%n",
                            zone.getCode(), zone.getName(), zone.getStatus(),
                            zone.getAnimalCount(), speciesList, feedInfo);
                }

                // ==================== SHEET 4: SENSORS ====================
                writer.println();
                writer.println("===== SHEET 4: SENSORS REPORT =====");
                writer.println();
                writer.println("SENSOR CODE,ZONE CODE,TYPE,STATUS,MIN THRESHOLD,MAX THRESHOLD,UNIT,LAST READING,LAST READING TIME");

                for (Sensor s : getAllSensors()) {
                    String lastReading = "No readings";
                    String lastTime = "";
                    if (!s.getReadings().isEmpty()) {
                        Reading last = s.getReadings().get(s.getReadings().size() - 1);
                        lastReading = last.getValue() + " " + s.getUnit();
                        lastTime = last.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                    writer.printf("%s,%s,%s,%s,%.2f,%.2f,%s,%s,%s%n",
                            s.getCode(), s.getZoneCode(), s.getClass().getSimpleName(),
                            s.getStatus(), s.getThresholdMin(), s.getThresholdMax(),
                            s.getUnit(), lastReading, lastTime);
                }

                // ==================== SHEET 5: ALERT HISTORY ====================
                writer.println();
                writer.println("===== SHEET 5: ALERT HISTORY =====");
                writer.println();
                writer.println("ALERT ID,SENSOR CODE,READING VALUE,MIN THRESHOLD,MAX THRESHOLD,SEVERITY,TIMESTAMP,ACKNOWLEDGED,DISMISSED");

                for (Alert alert : alertHistory) {
                    writer.printf("%s,%s,%.2f,%.2f,%.2f,%s,%s,%s,%s%n",
                            alert.getId(), alert.getSensorCode(), alert.getReadingValue(),
                            alert.getThresholdMin(), alert.getThresholdMax(), alert.getSeverity(),
                            alert.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            alert.isAcknowledged(), alert.isDismissed());
                }

                // ==================== SHEET 6: ACTIVE ALERTS ====================
                writer.println();
                writer.println("===== SHEET 6: ACTIVE ALERTS =====");
                writer.println();
                writer.println("ALERT ID,SENSOR CODE,READING VALUE,MIN THRESHOLD,MAX THRESHOLD,SEVERITY,TIMESTAMP");

                for (Alert alert : activeAlerts) {
                    writer.printf("%s,%s,%.2f,%.2f,%.2f,%s,%s%n",
                            alert.getId(), alert.getSensorCode(), alert.getReadingValue(),
                            alert.getThresholdMin(), alert.getThresholdMax(), alert.getSeverity(),
                            alert.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }

                // ==================== SHEET 7: SUMMARY STATISTICS ====================
                writer.println();
                writer.println("===== SHEET 7: SUMMARY STATISTICS =====");
                writer.println();
                writer.println("METRIC,VALUE");
                writer.printf("Total Crop Zones,%d%n", cropZones.size());
                writer.printf("Total Livestock Zones,%d%n", livestockZones.size());
                writer.printf("Total Aquaculture Zones,%d%n", aquacultureZones.size());
                writer.printf("Total Zones,%d%n", cropZones.size() + livestockZones.size() + aquacultureZones.size());
                writer.printf("Total Crops,%d%n", cropZones.stream().mapToInt(CropZone::getEntityCount).sum());
                writer.printf("Total Animals,%d%n", livestockZones.stream().mapToInt(LivestockZone::getEntityCount).sum());
                writer.printf("Total Fish,%d%n", aquacultureZones.stream().mapToInt(AquacultureZone::getAnimalCount).sum());
                writer.printf("Total Sensors,%d%n", getAllSensors().size());
                writer.printf("Active Alerts,%d%n", activeAlerts.size());
                writer.printf("Alert History,%d%n", alertHistory.size());

                int criticalCount = (int) alertHistory.stream().filter(a -> a.getSeverity() == SeverityLevel.CRITICAL).count();
                int warningCount = alertHistory.size() - criticalCount;
                writer.printf("Critical Alerts,%d%n", criticalCount);
                writer.printf("Warning Alerts,%d%n", warningCount);
                writer.printf("Acknowledged Alerts,%d%n", alertHistory.stream().filter(Alert::isAcknowledged).count());

                showInfoDialog("Export Complete", "Report saved to: " + file.getAbsolutePath());
            } catch (Exception e) {
                showErrorDialog("Export Error", "Failed to export report: " + e.getMessage());
            }
        }
    }

    private List<Sensor> getAllSensors() {
        List<Sensor> all = new ArrayList<>();
        for (CropZone z : cropZones) all.addAll(z.getSensors());
        for (LivestockZone z : livestockZones) all.addAll(z.getSensors());
        for (AquacultureZone z : aquacultureZones) all.addAll(z.getSensors());
        return all;
    }

    private Sensor findSensorByCode(String code) {
        for (Sensor s : getAllSensors()) {
            if (s.getCode().equals(code)) return s;
        }
        return null;
    }

    private void addSensorToZone(Sensor sensor) {
        for (CropZone z : cropZones) {
            if (z.getCode().equals(sensor.getZoneCode())) {
                z.addSensor(sensor);
                saveAllData();
                return;
            }
        }
        for (LivestockZone z : livestockZones) {
            if (z.getCode().equals(sensor.getZoneCode())) {
                z.addSensor(sensor);
                saveAllData();
                return;
            }
        }
        for (AquacultureZone z : aquacultureZones) {
            if (z.getCode().equals(sensor.getZoneCode())) {
                z.addSensor(sensor);
                saveAllData();
                return;
            }
        }
    }

    private void removeSensor(Sensor sensor) {
        for (CropZone z : cropZones) {
            if (z.getSensors().remove(sensor)) {
                saveAllData();
                return;
            }
        }
        for (LivestockZone z : livestockZones) {
            if (z.getSensors().remove(sensor)) {
                saveAllData();
                return;
            }
        }
        for (AquacultureZone z : aquacultureZones) {
            if (z.getSensors().remove(sensor)) {
                saveAllData();
                return;
            }
        }
    }

    private void showSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Automatic Reading Checker Settings");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        CheckBox enableCheck = new CheckBox("Enable automatic reading checks");
        enableCheck.setSelected(isAutoCheckEnabled);

        TextField intervalField = new TextField(String.valueOf(checkIntervalSeconds));
        intervalField.setPromptText("Interval in seconds");

        Button applyBtn = new Button("Apply");
        applyBtn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 15;");
        applyBtn.setOnAction(e -> {
            isAutoCheckEnabled = enableCheck.isSelected();
            try {
                int newInterval = Integer.parseInt(intervalField.getText());
                if (newInterval > 0) {
                    checkIntervalSeconds = newInterval;
                    if (readingCheckerTimer != null) {
                        readingCheckerTimer.cancel();
                    }
                    startPeriodicReadingCheck();
                    showInfoDialog("Settings Updated", "Checker will run every " + checkIntervalSeconds + " seconds");
                }
            } catch (NumberFormatException ex) {
                showErrorDialog("Error", "Invalid interval");
            }
            dialog.close();
        });

        grid.add(enableCheck, 0, 0, 2, 1);
        grid.add(new Label("Check interval (seconds):"), 0, 1);
        grid.add(intervalField, 1, 1);
        grid.add(applyBtn, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait();
    }

    // ==================== UI METHODS ====================

    @Override
    public void start(Stage primaryStage) {
        loadDataFromFile();
        startPeriodicReadingCheck();

        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: " + SECONDARY_COLOR + ";");

        sidebar = createSidebar();
        mainLayout.setLeft(sidebar);

        contentArea = new StackPane();
        contentArea.setPadding(new Insets(25));
        contentArea.setStyle("-fx-background-color: " + SECONDARY_COLOR + ";");
        mainLayout.setCenter(contentArea);

        showDashboard();

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double screenWidth = screenBounds.getWidth() * 0.9;
        double screenHeight = screenBounds.getHeight() * 0.85;

        Scene scene = new Scene(mainLayout, screenWidth, screenHeight);
        primaryStage.setMaximized(true);
        primaryStage.setTitle("Smart Farming System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createSidebar() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(25, 15, 25, 15));
        box.setPrefWidth(260);
        box.setStyle("-fx-background-color: " + SIDEBAR_COLOR + ";");

        Label title = new Label("SMART FARM");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.WHITE);
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setPadding(new Insets(0, 0, 20, 0));
        box.getChildren().add(title);

        box.getChildren().addAll(
                createNavButton("Dashboard", this::showDashboard),
                createNavButton("Crop Zones", () -> showZones("crop")),
                createNavButton("Livestock Zones", () -> showZones("livestock")),
                createNavButton("Aquaculture Zones", () -> showZones("aquaculture")),
                createNavButton("Sensors", this::showSensors),
                createNavButton("Alerts", this::showAlerts),
                createNavButton("Reports", this::showReports),
                new Separator(),
                createNavButton("New Crop Zone", this::showCreateCropZoneDialog),
                createNavButton("New Livestock Zone", this::showCreateLivestockZoneDialog),
                createNavButton("New Aquaculture Zone", this::showCreateAquacultureZoneDialog),
                createNavButton("New Sensor", this::showCreateSensorMenu),
                createNavButton("Settings", this::showSettingsDialog)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        box.getChildren().add(spacer);

        Label stats = new Label(String.format("Zones: %d | Sensors: %d | Alerts: %d",
                cropZones.size() + livestockZones.size() + aquacultureZones.size(),
                getAllSensors().size(), activeAlerts.size()));
        stats.setTextFill(Color.WHITE);
        stats.setFont(Font.font("System", 11));
        stats.setPadding(new Insets(10, 0, 0, 0));
        box.getChildren().add(stats);

        Button quitBtn = createNavButton("Quit", () -> {
            saveAllData();
            System.exit(0);
        });
        quitBtn.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 10 15;");
        quitBtn.setOnMouseEntered(e -> quitBtn.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 10 15;"));
        quitBtn.setOnMouseExited(e -> quitBtn.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 10 15;"));
        box.getChildren().add(quitBtn);

        return box;
    }

    private Button createNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 15, 10, 15));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 5;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #283593; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 5;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 5;"));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void setPageTitle(String title) {
        if (currentPageTitle == null) {
            currentPageTitle = new Label(title);
            currentPageTitle.setFont(Font.font("System", FontWeight.BOLD, 26));
            currentPageTitle.setTextFill(Color.web(PRIMARY_COLOR));
            currentPageTitle.setPadding(new Insets(0, 0, 20, 0));
        } else {
            currentPageTitle.setText(title);
        }
    }

    private void showDashboard() {
        VBox container = new VBox(20);
        setPageTitle("Farm Dashboard");
        container.getChildren().add(currentPageTitle);

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15);
        statsGrid.setVgap(15);
        statsGrid.setAlignment(Pos.CENTER);

        int totalZones = cropZones.size() + livestockZones.size() + aquacultureZones.size();
        int totalCrops = cropZones.stream().mapToInt(CropZone::getEntityCount).sum();
        int totalAnimals = livestockZones.stream().mapToInt(LivestockZone::getEntityCount).sum() +
                aquacultureZones.stream().mapToInt(AquacultureZone::getAnimalCount).sum();

        statsGrid.add(createStatCard("Total Zones", String.valueOf(totalZones), "#1976d2"), 0, 0);
        statsGrid.add(createStatCard("Active Alerts", String.valueOf(activeAlerts.size()), DANGER_COLOR), 1, 0);
        statsGrid.add(createStatCard("Total Crops", String.valueOf(totalCrops), "#388e3c"), 2, 0);
        statsGrid.add(createStatCard("Total Animals", String.valueOf(totalAnimals), "#fbc02d"), 3, 0);
        statsGrid.add(createStatCard("Total Sensors", String.valueOf(getAllSensors().size()), "#ff9800"), 0, 1);
        statsGrid.add(createStatCard("Critical Alerts", String.valueOf(activeAlerts.stream().filter(a -> a.getSeverity() == SeverityLevel.CRITICAL).count()), DANGER_COLOR), 1, 1);

        container.getChildren().add(statsGrid);

        Label alertsLabel = new Label("Recent Alerts");
        alertsLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        container.getChildren().add(alertsLabel);

        VBox alertContainer = createAlertTableView(activeAlerts, true);
        alertContainer.setPrefHeight(250);
        container.getChildren().add(alertContainer);

        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 0, 0));
        actions.getChildren().addAll(
                createActionButton("Generate Alert", "#ff9800", this::showManualAlertDialog),
                createActionButton("Export Report", SUCCESS_COLOR, this::exportReport),
                createActionButton("Refresh", "#607d8b", this::showDashboard)
        );
        container.getChildren().add(actions);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        contentArea.getChildren().setAll(scrollPane);
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.setPrefSize(160, 90);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        card.setAlignment(Pos.CENTER);

        Label titleLbl = new Label(title);
        titleLbl.setTextFill(Color.GRAY);
        titleLbl.setFont(Font.font("System", 11));

        Label valueLbl = new Label(value);
        valueLbl.setFont(Font.font("System", FontWeight.BOLD, 28));
        valueLbl.setTextFill(Color.web(color));

        card.getChildren().addAll(titleLbl, valueLbl);
        return card;
    }

    private Button createActionButton(String text, String color, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-cursor: hand; -fx-background-radius: 5;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void showZones(String type) {
        VBox container = new VBox(15);

        if (type.equals("crop")) {
            setPageTitle("Crop Zones");
            container.getChildren().add(currentPageTitle);
            container.getChildren().add(createCropZoneTable());
        } else if (type.equals("livestock")) {
            setPageTitle("Livestock Zones");
            container.getChildren().add(currentPageTitle);
            container.getChildren().add(createLivestockZoneTable());
        } else {
            setPageTitle("Aquaculture Zones");
            container.getChildren().add(currentPageTitle);
            container.getChildren().add(createAquacultureZoneTable());
        }

        contentArea.getChildren().setAll(container);
    }

    private VBox createCropZoneTable() {
        VBox wrapper = new VBox(10);

        TableView<CropZone> table = new TableView<>();
        table.setItems(cropZones);
        table.setPrefHeight(400);

        TableColumn<CropZone, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(100);

        TableColumn<CropZone, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(180);

        TableColumn<CropZone, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        statusCol.setPrefWidth(80);

        TableColumn<CropZone, Integer> countCol = new TableColumn<>("Crops");
        countCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getEntityCount()).asObject());
        countCol.setPrefWidth(80);

        TableColumn<CropZone, String> allowedCol = new TableColumn<>("Family");
        allowedCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAllowedCropFamily().toString()));
        allowedCol.setPrefWidth(100);

        TableColumn<CropZone, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(280);
        actionsCol.setCellFactory(col -> new TableCell<CropZone, Void>() {
            private final Button viewBtn = new Button("View Details");
            private final Button editBtn = new Button("Edit Zone");
            private final Button cropsBtn = new Button("Manage Crops");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(5, viewBtn, editBtn, cropsBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                cropsBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                deleteBtn.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");

                viewBtn.setOnAction(e -> {
                    CropZone zone = getTableView().getItems().get(getIndex());
                    showCropZoneDetails(zone);
                });

                editBtn.setOnAction(e -> {
                    CropZone zone = getTableView().getItems().get(getIndex());
                    showEditCropZoneDialog(zone);
                });

                cropsBtn.setOnAction(e -> {
                    CropZone zone = getTableView().getItems().get(getIndex());
                    showManageCropsDialog(zone);
                });

                deleteBtn.setOnAction(e -> {
                    CropZone zone = getTableView().getItems().get(getIndex());
                    showDeleteConfirmation("Zone", zone.getName(), () -> {
                        cropZones.remove(zone);
                        saveAllData();
                        showZones("crop");
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(codeCol, nameCol, statusCol, countCol, allowedCol, actionsCol);
        wrapper.getChildren().add(table);

        Button addBtn = createActionButton("Add New Crop Zone", PRIMARY_COLOR, this::showCreateCropZoneDialog);
        wrapper.getChildren().add(addBtn);

        return wrapper;
    }

    // NEW: Manage Crops Dialog - Add, Edit, Delete crops within a zone
    private void showManageCropsDialog(CropZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Manage Crops - " + zone.getName());
        dialog.setResizable(true);
        dialog.setWidth(800);
        dialog.setHeight(600);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label infoLabel = new Label("Zone: " + zone.getName() + " | Allowed Family: " + zone.getAllowedCropFamily());
        infoLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        infoLabel.setTextFill(Color.web(PRIMARY_COLOR));

        TableView<Crop> table = new TableView<>();
        ObservableList<Crop> crops = FXCollections.observableArrayList(zone.getCrops());
        table.setItems(crops);
        table.setPrefHeight(250);

        TableColumn<Crop, String> nameCol = new TableColumn<>("Crop Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(120);

        TableColumn<Crop, String> familyCol = new TableColumn<>("Family");
        familyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFamily().toString()));
        familyCol.setPrefWidth(100);

        TableColumn<Crop, String> stageCol = new TableColumn<>("Growth Stage");
        stageCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGrowthStage().toString()));
        stageCol.setPrefWidth(100);

        TableColumn<Crop, String> plantingCol = new TableColumn<>("Planted");
        plantingCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPlantingDate().toString()));
        plantingCol.setPrefWidth(100);

        TableColumn<Crop, String> harvestCol = new TableColumn<>("Harvest");
        harvestCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getExpectedHarvestDate().toString()));
        harvestCol.setPrefWidth(100);

        TableColumn<Crop, String> phCol = new TableColumn<>("pH Range");
        phCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getOptimalPHMin() + " - " + data.getValue().getOptimalPHMax()));
        phCol.setPrefWidth(100);

        TableColumn<Crop, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(100);
        actionsCol.setCellFactory(col -> new TableCell<Crop, Void>() {
            private final Button removeBtn = new Button("Remove");
            {
                removeBtn.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                removeBtn.setOnAction(e -> {
                    Crop crop = getTableView().getItems().get(getIndex());
                    zone.getCrops().remove(crop);
                    table.setItems(FXCollections.observableArrayList(zone.getCrops()));
                    saveAllData();
                    showInfoDialog("Removed", "Crop '" + crop.getName() + "' removed");
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });

        table.getColumns().addAll(nameCol, familyCol, stageCol, plantingCol, harvestCol, phCol, actionsCol);

        // Add Crop Form
        TitledPane addCropPane = new TitledPane();
        addCropPane.setText("Add New Crop (Family must match: " + zone.getAllowedCropFamily() + ")");
        addCropPane.setExpanded(true);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(15);
        formGrid.setPadding(new Insets(15));

        TextField cropNameField = new TextField();
        cropNameField.setPromptText("Crop name");

        DatePicker plantingDate = new DatePicker(LocalDate.now());
        DatePicker harvestDate = new DatePicker(LocalDate.now().plusMonths(3));

        TextField phMinField = new TextField("6.0");
        TextField phMaxField = new TextField("7.5");
        TextField moistureMinField = new TextField("20.0");
        TextField moistureMaxField = new TextField("30.0");

        ComboBox<GrowthStage> stageBox = new ComboBox<>();
        stageBox.getItems().addAll(GrowthStage.values());
        stageBox.setValue(GrowthStage.SOWING);

        formGrid.add(new Label("Crop Name:*"), 0, 0);
        formGrid.add(cropNameField, 1, 0);
        formGrid.add(new Label("Planting Date:"), 0, 1);
        formGrid.add(plantingDate, 1, 1);
        formGrid.add(new Label("Expected Harvest:"), 0, 2);
        formGrid.add(harvestDate, 1, 2);
        formGrid.add(new Label("pH Range (min-max):"), 0, 3);
        formGrid.add(new HBox(10, phMinField, new Label("-"), phMaxField), 1, 3);
        formGrid.add(new Label("Moisture % (min-max):"), 0, 4);
        formGrid.add(new HBox(10, moistureMinField, new Label("-"), moistureMaxField), 1, 4);
        formGrid.add(new Label("Growth Stage:"), 0, 5);
        formGrid.add(stageBox, 1, 5);

        Button addBtn = new Button("Add Crop");
        addBtn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        addBtn.setOnAction(e -> {
            if (!cropNameField.getText().isEmpty()) {
                Crop newCrop = new Crop(
                        cropNameField.getText(),
                        zone.getAllowedCropFamily(),  // Force the zone's allowed family
                        plantingDate.getValue(),
                        harvestDate.getValue(),
                        Double.parseDouble(phMinField.getText()),
                        Double.parseDouble(phMaxField.getText()),
                        Double.parseDouble(moistureMinField.getText()),
                        Double.parseDouble(moistureMaxField.getText())
                );
                newCrop.setGrowthStage(stageBox.getValue());
                zone.addCrop(newCrop);
                table.setItems(FXCollections.observableArrayList(zone.getCrops()));
                cropNameField.clear();
                saveAllData();
                showInfoDialog("Success", "Crop '" + newCrop.getName() + "' added successfully!");
            } else {
                showWarningDialog("Missing Name", "Please enter a crop name");
            }
        });

        formGrid.add(addBtn, 0, 6, 2, 1);
        addCropPane.setContent(formGrid);

        // Edit Growth Stage Section
        TitledPane editStagePane = new TitledPane();
        editStagePane.setText("Update Crop Growth Stage");
        editStagePane.setExpanded(false);

        GridPane stageGrid = new GridPane();
        stageGrid.setHgap(15);
        stageGrid.setVgap(15);
        stageGrid.setPadding(new Insets(15));

        ComboBox<Crop> cropSelectBox = new ComboBox<>(crops);
        cropSelectBox.setPromptText("Select crop");
        cropSelectBox.setPrefWidth(200);

        ComboBox<GrowthStage> newStageBox = new ComboBox<>();
        newStageBox.getItems().addAll(GrowthStage.values());

        Button updateStageBtn = new Button("Update Stage");
        updateStageBtn.setStyle("-fx-background-color: " + WARNING_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        updateStageBtn.setOnAction(e -> {
            Crop selected = cropSelectBox.getValue();
            if (selected != null && newStageBox.getValue() != null) {
                selected.setGrowthStage(newStageBox.getValue());
                table.refresh();
                saveAllData();
                showInfoDialog("Updated", "Growth stage updated to " + newStageBox.getValue());
            }
        });

        stageGrid.add(new Label("Select Crop:"), 0, 0);
        stageGrid.add(cropSelectBox, 1, 0);
        stageGrid.add(new Label("New Growth Stage:"), 0, 1);
        stageGrid.add(newStageBox, 1, 1);
        stageGrid.add(updateStageBtn, 0, 2, 2, 1);
        editStagePane.setContent(stageGrid);

        content.getChildren().addAll(infoLabel, new Label("Current Crops:"), table, addCropPane, editStagePane);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();

        showZones("crop");
    }

    private void showCropZoneDetails(CropZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Crop Zone Details - " + zone.getName());
        dialog.setResizable(true);
        dialog.setWidth(650);
        dialog.setHeight(550);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        TextArea details = new TextArea();
        details.setEditable(false);
        StringBuilder sb = new StringBuilder();
        sb.append("ZONE INFORMATION\n");
        sb.append("================\n");
        sb.append("Code: ").append(zone.getCode()).append("\n");
        sb.append("Name: ").append(zone.getName()).append("\n");
        sb.append("Status: ").append(zone.getStatus()).append("\n");
        sb.append("Allowed Crop Family: ").append(zone.getAllowedCropFamily()).append("\n");
        sb.append("\nBOUNDARIES\n");
        sb.append("==========\n");
        sb.append("North: ").append(zone.getBoundNorth()).append("\n");
        sb.append("South: ").append(zone.getBoundSouth()).append("\n");
        sb.append("East: ").append(zone.getBoundEast()).append("\n");
        sb.append("West: ").append(zone.getBoundWest()).append("\n");
        sb.append("\nCROPS (").append(zone.getCrops().size()).append(" crops)\n");
        sb.append("=====\n");
        if (zone.getCrops().isEmpty()) {
            sb.append("No crops in this zone.\n");
        } else {
            for (Crop crop : zone.getCrops()) {
                sb.append("\n- ").append(crop.getName()).append("\n");
                sb.append("  Family: ").append(crop.getFamily()).append("\n");
                sb.append("  Growth Stage: ").append(crop.getGrowthStage()).append("\n");
                sb.append("  Planting Date: ").append(crop.getPlantingDate()).append("\n");
                sb.append("  Expected Harvest: ").append(crop.getExpectedHarvestDate()).append("\n");
                sb.append("  Optimal pH: ").append(crop.getOptimalPHMin()).append(" - ").append(crop.getOptimalPHMax()).append("\n");
                sb.append("  Optimal Moisture: ").append(crop.getOptimalMoistureMin()).append("% - ").append(crop.getOptimalMoistureMax()).append("%\n");
            }
        }
        sb.append("\nSENSORS (").append(zone.getSensors().size()).append(" sensors)\n");
        sb.append("=======\n");
        if (zone.getSensors().isEmpty()) {
            sb.append("No sensors in this zone.\n");
        } else {
            for (Sensor s : zone.getSensors()) {
                sb.append("- ").append(s.getCode()).append(" (").append(s.getClass().getSimpleName()).append(")\n");
                sb.append("  Status: ").append(s.getStatus()).append("\n");
                sb.append("  Threshold: [").append(s.getThresholdMin()).append(" - ").append(s.getThresholdMax()).append("] ").append(s.getUnit()).append("\n");
            }
        }

        details.setText(sb.toString());
        details.setPrefHeight(400);
        content.getChildren().add(details);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private VBox createLivestockZoneTable() {
        VBox wrapper = new VBox(10);

        TableView<LivestockZone> table = new TableView<>();
        table.setItems(livestockZones);
        table.setPrefHeight(400);

        TableColumn<LivestockZone, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(100);

        TableColumn<LivestockZone, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(180);

        TableColumn<LivestockZone, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        statusCol.setPrefWidth(80);

        TableColumn<LivestockZone, Integer> countCol = new TableColumn<>("Animals");
        countCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getEntityCount()).asObject());
        countCol.setPrefWidth(80);

        TableColumn<LivestockZone, String> allowedCol = new TableColumn<>("Type");
        allowedCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAllowedAnimalType().toString()));
        allowedCol.setPrefWidth(100);

        TableColumn<LivestockZone, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(250);
        actionsCol.setCellFactory(col -> new TableCell<LivestockZone, Void>() {
            private final Button editBtn = new Button("Edit Zone");
            private final Button deleteBtn = new Button("Delete");
            private final Button animalsBtn = new Button("Manage Animals");
            private final Button feedBtn = new Button("Feeding");
            private final HBox pane = new HBox(5, editBtn, deleteBtn, animalsBtn, feedBtn);

            {
                editBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                deleteBtn.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                animalsBtn.setStyle("-fx-background-color: " + WARNING_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                feedBtn.setStyle("-fx-background-color: #9c27b0; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");

                editBtn.setOnAction(e -> {
                    LivestockZone zone = getTableView().getItems().get(getIndex());
                    showEditLivestockZoneDialog(zone);
                });

                deleteBtn.setOnAction(e -> {
                    LivestockZone zone = getTableView().getItems().get(getIndex());
                    showDeleteConfirmation("Zone", zone.getName(), () -> {
                        livestockZones.remove(zone);
                        saveAllData();
                        showZones("livestock");
                    });
                });

                animalsBtn.setOnAction(e -> {
                    LivestockZone zone = getTableView().getItems().get(getIndex());
                    showManageAnimalsDialog(zone);
                });

                feedBtn.setOnAction(e -> {
                    LivestockZone zone = getTableView().getItems().get(getIndex());
                    showSetFeedingDialog(zone);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(codeCol, nameCol, statusCol, countCol, allowedCol, actionsCol);
        wrapper.getChildren().add(table);

        Button addBtn = createActionButton("Add Livestock Zone", PRIMARY_COLOR, this::showCreateLivestockZoneDialog);
        wrapper.getChildren().add(addBtn);

        return wrapper;
    }

    private VBox createAquacultureZoneTable() {
        VBox wrapper = new VBox(10);

        TableView<AquacultureZone> table = new TableView<>();
        table.setItems(aquacultureZones);
        table.setPrefHeight(400);

        TableColumn<AquacultureZone, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(100);

        TableColumn<AquacultureZone, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(180);

        TableColumn<AquacultureZone, Integer> countCol = new TableColumn<>("Fish");
        countCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getAnimalCount()).asObject());
        countCol.setPrefWidth(80);

        TableColumn<AquacultureZone, String> speciesCol = new TableColumn<>("Species");
        speciesCol.setCellValueFactory(data -> new SimpleStringProperty(String.join(", ", data.getValue().getSpecies())));
        speciesCol.setPrefWidth(180);

        TableColumn<AquacultureZone, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(250);
        actionsCol.setCellFactory(col -> new TableCell<AquacultureZone, Void>() {
            private final Button editBtn = new Button("Edit Zone");
            private final Button deleteBtn = new Button("Delete");
            private final Button detailsBtn = new Button("Details");
            private final Button feedBtn = new Button("Feeding");
            private final HBox pane = new HBox(5, editBtn, deleteBtn, detailsBtn, feedBtn);

            {
                editBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                deleteBtn.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                detailsBtn.setStyle("-fx-background-color: #009688; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                feedBtn.setStyle("-fx-background-color: #9c27b0; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");

                editBtn.setOnAction(e -> {
                    AquacultureZone zone = getTableView().getItems().get(getIndex());
                    showEditAquacultureZoneDialog(zone);
                });

                deleteBtn.setOnAction(e -> {
                    AquacultureZone zone = getTableView().getItems().get(getIndex());
                    showDeleteConfirmation("Zone", zone.getName(), () -> {
                        aquacultureZones.remove(zone);
                        saveAllData();
                        showZones("aquaculture");
                    });
                });

                detailsBtn.setOnAction(e -> {
                    AquacultureZone zone = getTableView().getItems().get(getIndex());
                    showAquacultureDetails(zone);
                });

                feedBtn.setOnAction(e -> {
                    AquacultureZone zone = getTableView().getItems().get(getIndex());
                    showAquacultureFeedingDialog(zone);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(codeCol, nameCol, countCol, speciesCol, actionsCol);
        wrapper.getChildren().add(table);

        Button addBtn = createActionButton("Add Aquaculture Zone", PRIMARY_COLOR, this::showCreateAquacultureZoneDialog);
        wrapper.getChildren().add(addBtn);

        return wrapper;
    }

    private void showAquacultureFeedingDialog(AquacultureZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Feeding Program - " + zone.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField feedTypeField = new TextField();
        TextField quantityField = new TextField();
        TextField mealsField = new TextField();

        if (zone.getFeedingProgram() != null) {
            feedTypeField.setText(zone.getFeedingProgram().getFeedType());
            quantityField.setText(String.valueOf(zone.getFeedingProgram().getQuantityPerMeal()));
            mealsField.setText(String.valueOf(zone.getFeedingProgram().getMealsPerDay()));
        }

        grid.add(new Label("Feed Type:"), 0, 0);
        grid.add(feedTypeField, 1, 0);
        grid.add(new Label("kg per Meal:"), 0, 1);
        grid.add(quantityField, 1, 1);
        grid.add(new Label("Meals per Day:"), 0, 2);
        grid.add(mealsField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    double quantity = Double.parseDouble(quantityField.getText());
                    int meals = Integer.parseInt(mealsField.getText());
                    zone.setFeedingProgram(new FeedingProgram(feedTypeField.getText(), quantity, meals));
                    saveAllData();
                    showInfoDialog("Success", "Feeding program updated!");
                    showZones("aquaculture");
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number format");
                }
            }
        });
    }

    private void showSensors() {
        VBox container = new VBox(15);
        setPageTitle("Sensors");
        container.getChildren().add(currentPageTitle);

        TableView<Sensor> table = new TableView<>();
        ObservableList<Sensor> sensors = FXCollections.observableArrayList(getAllSensors());
        table.setItems(sensors);
        table.setPrefHeight(400);

        TableColumn<Sensor, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(100);

        TableColumn<Sensor, String> zoneCol = new TableColumn<>("Zone");
        zoneCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getZoneCode()));
        zoneCol.setPrefWidth(100);

        TableColumn<Sensor, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getClass().getSimpleName()));
        typeCol.setPrefWidth(130);

        TableColumn<Sensor, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        statusCol.setPrefWidth(80);

        TableColumn<Sensor, String> thresholdCol = new TableColumn<>("Threshold");
        thresholdCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("[%.1f - %.1f]", data.getValue().getThresholdMin(), data.getValue().getThresholdMax())));
        thresholdCol.setPrefWidth(150);

        TableColumn<Sensor, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(200);
        actionsCol.setCellFactory(col -> new TableCell<Sensor, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Button historyBtn = new Button("History");
            private final HBox pane = new HBox(5, editBtn, deleteBtn, historyBtn);

            {
                editBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                deleteBtn.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                historyBtn.setStyle("-fx-background-color: " + WARNING_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");

                editBtn.setOnAction(e -> {
                    Sensor sensor = getTableView().getItems().get(getIndex());
                    showEditSensorDialog(sensor);
                });

                deleteBtn.setOnAction(e -> {
                    Sensor sensor = getTableView().getItems().get(getIndex());
                    showDeleteConfirmation("Sensor", sensor.getCode(), () -> {
                        removeSensor(sensor);
                        showSensors();
                    });
                });

                historyBtn.setOnAction(e -> {
                    Sensor sensor = getTableView().getItems().get(getIndex());
                    showReadingHistory(sensor);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(codeCol, zoneCol, typeCol, statusCol, thresholdCol, actionsCol);
        container.getChildren().add(table);

        HBox actions = new HBox(10);
        actions.getChildren().addAll(
                createActionButton("Add Reading", "#2196f3", this::showAddReadingDialog),
                createActionButton("New Sensor", PRIMARY_COLOR, this::showCreateSensorMenu),
                createActionButton("Refresh", "#607d8b", this::showSensors)
        );
        container.getChildren().add(actions);

        contentArea.getChildren().setAll(container);
    }

    private void showAlerts() {
        VBox container = new VBox(15);
        setPageTitle("Alerts Center");
        container.getChildren().add(currentPageTitle);

        TabPane tabs = new TabPane();

        Tab activeTab = new Tab("Active Alerts (" + activeAlerts.size() + ")");
        activeTab.setContent(createAlertTableView(activeAlerts, true));
        activeTab.setClosable(false);

        Tab historyTab = new Tab("Alert History (" + alertHistory.size() + ")");
        historyTab.setContent(createAlertTableView(alertHistory, false));
        historyTab.setClosable(false);

        tabs.getTabs().addAll(activeTab, historyTab);
        container.getChildren().add(tabs);

        contentArea.getChildren().setAll(container);
    }

    private VBox createAlertTableView(ObservableList<model.entities.Alert> alerts, boolean showActions) {
        VBox container = new VBox(10);

        TableView<model.entities.Alert> table = new TableView<>();
        table.setItems(alerts);
        table.setPrefHeight(400);

        TableColumn<model.entities.Alert, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        idCol.setPrefWidth(80);

        TableColumn<model.entities.Alert, String> sensorCol = new TableColumn<>("Sensor");
        sensorCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSensorCode()));
        sensorCol.setPrefWidth(100);

        TableColumn<model.entities.Alert, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getReadingValue())));
        valueCol.setPrefWidth(80);

        TableColumn<model.entities.Alert, String> thresholdCol = new TableColumn<>("Threshold");
        thresholdCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("[%.1f - %.1f]", data.getValue().getThresholdMin(), data.getValue().getThresholdMax())));
        thresholdCol.setPrefWidth(150);

        TableColumn<model.entities.Alert, String> severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeverity().toString()));
        severityCol.setPrefWidth(80);
        severityCol.setCellFactory(col -> new TableCell<model.entities.Alert, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("CRITICAL")) {
                        setStyle("-fx-text-fill: " + DANGER_COLOR + "; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: " + WARNING_COLOR + "; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<model.entities.Alert, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isAcknowledged() ? "Acknowledged" : (data.getValue().isDismissed() ? "Dismissed" : "Pending")));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> new TableCell<model.entities.Alert, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Acknowledged")) {
                        setStyle("-fx-text-fill: " + SUCCESS_COLOR + "; -fx-font-weight: bold;");
                    } else if (item.equals("Dismissed")) {
                        setStyle("-fx-text-fill: #9e9e9e;");
                    } else {
                        setStyle("-fx-text-fill: " + WARNING_COLOR + "; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<model.entities.Alert, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        timeCol.setPrefWidth(170);

        table.getColumns().addAll(idCol, sensorCol, valueCol, thresholdCol, severityCol, statusCol, timeCol);

        container.getChildren().add(table);

        if (showActions) {
            HBox actions = new HBox(15);
            actions.setPadding(new Insets(10, 0, 0, 0));

            Button acknowledgeBtn = createActionButton("Acknowledge Selected", SUCCESS_COLOR, () -> {
                model.entities.Alert selected = table.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.isAcknowledged()) {
                    selected.acknowledge();
                    syncAlerts();
                    showAlerts();
                    showInfoDialog("Acknowledged", "Alert " + selected.getId() + " has been acknowledged");
                } else if (selected != null && selected.isAcknowledged()) {
                    showWarningDialog("Already Acknowledged", "This alert has already been acknowledged");
                } else {
                    showWarningDialog("No Selection", "Please select an alert to acknowledge");
                }
            });

            Button dismissBtn = createActionButton("Dismiss Selected", DANGER_COLOR, () -> {
                model.entities.Alert selected = table.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.isDismissed()) {
                    selected.dismiss();
                    syncAlerts();
                    showAlerts();
                    showInfoDialog("Dismissed", "Alert " + selected.getId() + " has been dismissed");
                } else if (selected != null && selected.isDismissed()) {
                    showWarningDialog("Already Dismissed", "This alert has already been dismissed");
                } else {
                    showWarningDialog("No Selection", "Please select an alert to dismiss");
                }
            });

            Button refreshBtn = createActionButton("Refresh", "#607d8b", this::showAlerts);

            actions.getChildren().addAll(acknowledgeBtn, dismissBtn, refreshBtn);
            container.getChildren().add(actions);
        }

        return container;
    }

    private void showReports() {
        VBox container = new VBox(15);
        setPageTitle("Reports");
        container.getChildren().add(currentPageTitle);

        Button exportBtn = createActionButton("Export Excel Report (7 Sheets)", SUCCESS_COLOR, this::exportReport);
        exportBtn.setPrefWidth(300);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(
                new Label("Click the button below to generate and export a comprehensive report."),
                new Label("The report will include 7 paginated sheets:"),
                new Label("  1. Crop Zones - All crops with their details"),
                new Label("  2. Livestock Zones - All animals and feeding programs"),
                new Label("  3. Aquaculture Zones - Species and fish counts"),
                new Label("  4. Sensors - All sensors with thresholds and last readings"),
                new Label("  5. Alert History - All past alerts with status"),
                new Label("  6. Active Alerts - Current alerts requiring attention"),
                new Label("  7. Summary Statistics - Farm-wide metrics"),
                exportBtn
        );

        container.getChildren().add(content);
        contentArea.getChildren().setAll(container);
    }

    // ==================== DIALOG METHODS ====================

    private void showCreateCropZoneDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Crop Zone");
        dialog.setHeaderText("Enter crop zone details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField codeField = new TextField("CZ" + String.format("%03d", zoneCounter));
        TextField nameField = new TextField();
        nameField.setPromptText("Zone name");

        // REQUIRED - not optional
        ComboBox<CropFamily> familyBox = new ComboBox<>();
        familyBox.getItems().addAll(CropFamily.values());
        familyBox.setPromptText("Select Crop Family (REQUIRED)");

        grid.add(new Label("Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Name:*"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Crop Family:*"), 0, 2);
        grid.add(familyBox, 1, 2);

        Label requiredLabel = new Label("* Required fields");
        requiredLabel.setTextFill(Color.web(DANGER_COLOR));
        requiredLabel.setFont(Font.font("System", 10));
        grid.add(requiredLabel, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && !nameField.getText().isEmpty() && familyBox.getValue() != null) {
                CropZone zone = new CropZone(codeField.getText(), nameField.getText());
                zone.setAllowedCropFamily(familyBox.getValue());  // REQUIRED
                cropZones.add(zone);
                zoneCounter++;
                saveAllData();
                showInfoDialog("Success", "Crop zone created! Family: " + familyBox.getValue());
                showZones("crop");
            } else if (familyBox.getValue() == null) {
                showErrorDialog("Missing Family", "Please select a crop family for this zone.");
            }
        });
    }

    private void showEditCropZoneDialog(CropZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Crop Zone - " + zone.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(zone.getName());
        ComboBox<ZoneStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(ZoneStatus.values()));
        statusBox.setValue(zone.getStatus());

        // Family is REQUIRED and CANNOT be changed (display only)
        Label familyLabel = new Label(zone.getAllowedCropFamily().toString());
        familyLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        familyLabel.setTextFill(Color.web(PRIMARY_COLOR));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);
        grid.add(new Label("Crop Family (fixed):"), 0, 2);
        grid.add(familyLabel, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                zone.setName(nameField.getText());
                if (statusBox.getValue() == ZoneStatus.SUSPENDED) {
                    zone.suspend();
                } else {
                    zone.activate();
                }
                saveAllData();
                showInfoDialog("Success", "Zone updated!");
                showZones("crop");
            }
        });
    }

    private void showCreateLivestockZoneDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Livestock Zone");
        dialog.setHeaderText("Enter livestock zone details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField codeField = new TextField("LZ" + String.format("%03d", zoneCounter));
        TextField nameField = new TextField();
        nameField.setPromptText("Zone name");

        // REQUIRED
        ComboBox<AnimalType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(AnimalType.values());
        typeBox.setPromptText("Select Animal Type (REQUIRED)");

        grid.add(new Label("Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Name:*"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Animal Type:*"), 0, 2);
        grid.add(typeBox, 1, 2);

        Label requiredLabel = new Label("* Required fields");
        requiredLabel.setTextFill(Color.web(DANGER_COLOR));
        requiredLabel.setFont(Font.font("System", 10));
        grid.add(requiredLabel, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && !nameField.getText().isEmpty() && typeBox.getValue() != null) {
                LivestockZone zone = new LivestockZone(codeField.getText(), nameField.getText());
                zone.setAllowedAnimalType(typeBox.getValue());
                livestockZones.add(zone);
                zoneCounter++;
                saveAllData();
                showInfoDialog("Success", "Livestock zone created! Type: " + typeBox.getValue());
                showZones("livestock");
            } else if (typeBox.getValue() == null) {
                showErrorDialog("Missing Type", "Please select an animal type for this zone.");
            }
        });
    }

    private void showEditLivestockZoneDialog(LivestockZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Livestock Zone");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(zone.getName());
        ComboBox<ZoneStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(ZoneStatus.values()));
        statusBox.setValue(zone.getStatus());

        Label typeLabel = new Label(zone.getAllowedAnimalType().toString());
        typeLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        typeLabel.setTextFill(Color.web(PRIMARY_COLOR));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);
        grid.add(new Label("Animal Type (fixed):"), 0, 2);
        grid.add(typeLabel, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                zone.setName(nameField.getText());
                if (statusBox.getValue() == ZoneStatus.SUSPENDED) {
                    zone.suspend();
                } else {
                    zone.activate();
                }
                saveAllData();
                showInfoDialog("Success", "Zone updated!");
                showZones("livestock");
            }
        });
    }

    private void showCreateAquacultureZoneDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Aquaculture Zone");
        dialog.setHeaderText("Enter aquaculture zone details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField codeField = new TextField("AZ" + String.format("%03d", zoneCounter));
        TextField nameField = new TextField();
        nameField.setPromptText("Zone name");

        grid.add(new Label("Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Name:*"), 0, 1);
        grid.add(nameField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && !nameField.getText().isEmpty()) {
                AquacultureZone zone = new AquacultureZone(codeField.getText(), nameField.getText());
                aquacultureZones.add(zone);
                zoneCounter++;
                saveAllData();
                showInfoDialog("Success", "Aquaculture zone created!");
                showAquacultureSetupDialog(zone);
            }
        });
    }

    private void showEditAquacultureZoneDialog(AquacultureZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Aquaculture Zone");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(zone.getName());
        TextField countField = new TextField(String.valueOf(zone.getAnimalCount()));
        ComboBox<ZoneStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(ZoneStatus.values()));
        statusBox.setValue(zone.getStatus());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Fish Count:"), 0, 1);
        grid.add(countField, 1, 1);
        grid.add(new Label("Status:"), 0, 2);
        grid.add(statusBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                zone.setName(nameField.getText());
                try {
                    zone.setAnimalCount(Integer.parseInt(countField.getText()));
                } catch (NumberFormatException e) {}
                if (statusBox.getValue() == ZoneStatus.SUSPENDED) {
                    zone.suspend();
                } else {
                    zone.activate();
                }
                saveAllData();
                showInfoDialog("Success", "Zone updated!");
                showZones("aquaculture");
            }
        });
    }

    private void showAquacultureSetupDialog(AquacultureZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Setup Aquaculture Zone - " + zone.getName());
        dialog.setHeaderText("Configure species and feeding");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField speciesField = new TextField();
        speciesField.setPromptText("Species (comma separated)");
        TextField countField = new TextField();
        countField.setPromptText("Number of fish");
        TextField feedTypeField = new TextField();
        feedTypeField.setPromptText("Feed type");
        TextField quantityField = new TextField();
        quantityField.setPromptText("kg per meal");
        TextField mealsField = new TextField();
        mealsField.setPromptText("Meals per day");

        grid.add(new Label("Species:"), 0, 0);
        grid.add(speciesField, 1, 0);
        grid.add(new Label("Fish Count:*"), 0, 1);
        grid.add(countField, 1, 1);
        grid.add(new Label("Feed Type:"), 0, 2);
        grid.add(feedTypeField, 1, 2);
        grid.add(new Label("kg/Meal:"), 0, 3);
        grid.add(quantityField, 1, 3);
        grid.add(new Label("Meals/Day:"), 0, 4);
        grid.add(mealsField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String[] species = speciesField.getText().split(",");
                    for (String s : species) {
                        zone.addSpecies(s.trim());
                    }
                    zone.setAnimalCount(Integer.parseInt(countField.getText()));
                    if (!feedTypeField.getText().isEmpty()) {
                        zone.setFeedingProgram(new FeedingProgram(
                                feedTypeField.getText(),
                                Double.parseDouble(quantityField.getText()),
                                Integer.parseInt(mealsField.getText())
                        ));
                    }
                    saveAllData();
                    showInfoDialog("Success", "Aquaculture zone configured!");
                    showZones("aquaculture");
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number");
                }
            }
        });
    }

    private void showAquacultureDetails(AquacultureZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Aquaculture Details - " + zone.getName());
        dialog.setResizable(true);
        dialog.setWidth(500);
        dialog.setHeight(400);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        TextArea info = new TextArea();
        info.setEditable(false);
        StringBuilder sb = new StringBuilder();
        sb.append("Zone: ").append(zone.getName()).append("\n");
        sb.append("Code: ").append(zone.getCode()).append("\n");
        sb.append("Status: ").append(zone.getStatus()).append("\n");
        sb.append("Fish Count: ").append(zone.getAnimalCount()).append("\n");
        sb.append("Species: ").append(String.join(", ", zone.getSpecies())).append("\n");

        if (zone.getFeedingProgram() != null) {
            sb.append("\nFeeding Program:\n");
            sb.append("  Feed: ").append(zone.getFeedingProgram().getFeedType()).append("\n");
            sb.append("  Daily: ").append(zone.getFeedingProgram().getDailyQuantity()).append(" kg/day\n");
        }

        info.setText(sb.toString());
        content.getChildren().add(info);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showSetFeedingDialog(LivestockZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Feeding Program - " + zone.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField feedTypeField = new TextField();
        TextField quantityField = new TextField();
        TextField mealsField = new TextField();

        if (zone.getFeedingProgram() != null) {
            feedTypeField.setText(zone.getFeedingProgram().getFeedType());
            quantityField.setText(String.valueOf(zone.getFeedingProgram().getQuantityPerMeal()));
            mealsField.setText(String.valueOf(zone.getFeedingProgram().getMealsPerDay()));
        }

        grid.add(new Label("Feed Type:"), 0, 0);
        grid.add(feedTypeField, 1, 0);
        grid.add(new Label("kg per Meal:"), 0, 1);
        grid.add(quantityField, 1, 1);
        grid.add(new Label("Meals per Day:"), 0, 2);
        grid.add(mealsField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    double quantity = Double.parseDouble(quantityField.getText());
                    int meals = Integer.parseInt(mealsField.getText());
                    zone.setFeedingProgram(new FeedingProgram(feedTypeField.getText(), quantity, meals));
                    saveAllData();
                    showInfoDialog("Success", "Feeding program updated!");
                    showZones("livestock");
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number format");
                }
            }
        });
    }

    private void showManageAnimalsDialog(LivestockZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Manage Animals - " + zone.getName());
        dialog.setResizable(true);
        dialog.setWidth(700);
        dialog.setHeight(600);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        TableView<Animal> table = new TableView<>();
        ObservableList<Animal> animals = FXCollections.observableArrayList(zone.getAnimals());
        table.setItems(animals);
        table.setPrefHeight(250);

        TableColumn<Animal, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        idCol.setPrefWidth(80);

        TableColumn<Animal, String> speciesCol = new TableColumn<>("Species");
        speciesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSpecies()));
        speciesCol.setPrefWidth(120);

        TableColumn<Animal, Integer> ageCol = new TableColumn<>("Age");
        ageCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getAge()).asObject());
        ageCol.setPrefWidth(60);

        TableColumn<Animal, Double> weightCol = new TableColumn<>("Weight");
        weightCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getWeight()).asObject());
        weightCol.setPrefWidth(80);

        TableColumn<Animal, String> healthCol = new TableColumn<>("Health");
        healthCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getHealthStatus().toString()));
        healthCol.setPrefWidth(100);

        TableColumn<Animal, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(150);
        actionsCol.setCellFactory(col -> new TableCell<Animal, Void>() {
            private final Button removeBtn = new Button("Remove");
            private final Button eventsBtn = new Button("Events");
            private final HBox pane = new HBox(5, eventsBtn, removeBtn);

            {
                removeBtn.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                eventsBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");

                removeBtn.setOnAction(e -> {
                    Animal animal = getTableView().getItems().get(getIndex());
                    zone.getAnimals().remove(animal);
                    table.setItems(FXCollections.observableArrayList(zone.getAnimals()));
                    saveAllData();
                    showInfoDialog("Removed", "Animal removed");
                });

                eventsBtn.setOnAction(e -> {
                    Animal animal = getTableView().getItems().get(getIndex());
                    showAnimalHealthEvents(animal);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(idCol, speciesCol, ageCol, weightCol, healthCol, actionsCol);

        // Add animal form
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));

        TextField idField = new TextField();
        idField.setPromptText("ID");
        TextField speciesField = new TextField();
        speciesField.setPromptText("Species");
        TextField ageField = new TextField();
        ageField.setPromptText("Age");
        TextField weightField = new TextField();
        weightField.setPromptText("Weight");

        // Type is REQUIRED and matches zone's allowed type
        Label typeLabel = new Label("Type: " + zone.getAllowedAnimalType());
        typeLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        typeLabel.setTextFill(Color.web(PRIMARY_COLOR));

        form.add(new Label("ID:"), 0, 0);
        form.add(idField, 1, 0);
        form.add(new Label("Species:"), 0, 1);
        form.add(speciesField, 1, 1);
        form.add(new Label("Age:"), 0, 2);
        form.add(ageField, 1, 2);
        form.add(new Label("Weight:"), 0, 3);
        form.add(weightField, 1, 3);
        form.add(new Label("Type:"), 0, 4);
        form.add(typeLabel, 1, 4);

        Button addBtn = createActionButton("Add Animal", PRIMARY_COLOR, () -> {
            if (!idField.getText().isEmpty() && !speciesField.getText().isEmpty()) {
                try {
                    Animal animal;
                    int age = Integer.parseInt(ageField.getText());
                    double weight = Double.parseDouble(weightField.getText());

                    if (zone.getAllowedAnimalType() == AnimalType.RUMINANT) {
                        animal = new Ruminant(idField.getText(), speciesField.getText(), age, weight);
                    } else {
                        animal = new Poultry(idField.getText(), speciesField.getText(), age, weight);
                    }
                    zone.addAnimal(animal);
                    table.setItems(FXCollections.observableArrayList(zone.getAnimals()));
                    saveAllData();
                    idField.clear();
                    speciesField.clear();
                    ageField.clear();
                    weightField.clear();
                    showInfoDialog("Success", "Animal added!");
                } catch (NumberFormatException ex) {
                    showErrorDialog("Error", "Invalid number");
                }
            } else {
                showWarningDialog("Missing Fields", "Please fill in all fields");
            }
        });

        content.getChildren().addAll(new Label("Animals in " + zone.getName()), table, new TitledPane("Add New Animal", form), addBtn);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();

        showZones("livestock");
    }

    private void showAnimalHealthEvents(Animal animal) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Health Events - " + animal.getId());
        dialog.setResizable(true);
        dialog.setWidth(500);
        dialog.setHeight(400);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label info = new Label(animal.getSpecies() + " | Age: " + animal.getAge() + " | Weight: " + animal.getWeight() + "kg");
        info.setFont(Font.font("System", FontWeight.BOLD, 14));

        ListView<String> eventsList = new ListView<>();
        eventsList.getItems().addAll(animal.getHealthEvents());
        eventsList.setPrefHeight(200);

        HBox addBox = new HBox(10);
        TextField eventField = new TextField();
        eventField.setPromptText("New event");
        eventField.setPrefWidth(300);
        Button addBtn = createActionButton("Add Event", PRIMARY_COLOR, () -> {
            if (!eventField.getText().isEmpty()) {
                animal.logHealthEvent(eventField.getText());
                eventsList.getItems().add(eventField.getText());
                saveAllData();
                eventField.clear();
            }
        });
        addBox.getChildren().addAll(eventField, addBtn);

        content.getChildren().addAll(info, new Label("Health History:"), eventsList, addBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showCreateSensorMenu() {
        VBox container = new VBox(15);
        setPageTitle("Create Sensor");
        container.getChildren().add(currentPageTitle);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(30));

        grid.add(createSensorCard("Environment Sensor", "#2196f3", () -> showCreateSensorDialog("EnvironmentSensor")), 0, 0);
        grid.add(createSensorCard("Soil Sensor", "#4caf50", () -> showCreateSensorDialog("SoilSensor")), 1, 0);
        grid.add(createSensorCard("Biometric Sensor", "#ff9800", () -> showCreateSensorDialog("BiometricSensor")), 2, 0);
        grid.add(createSensorCard("Water Sensor", "#009688", () -> showCreateSensorDialog("WaterSensor")), 0, 1);
        grid.add(createSensorCard("GPS Sensor", "#9c27b0", () -> showCreateSensorDialog("GPSSensor")), 1, 1);

        container.getChildren().add(grid);
        contentArea.getChildren().setAll(container);
    }

    private VBox createSensorCard(String type, String color, Runnable action) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setPrefSize(180, 100);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0); -fx-cursor: hand;");
        card.setAlignment(Pos.CENTER);

        Label titleLbl = new Label(type);
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLbl.setTextFill(Color.web(color));

        card.getChildren().add(titleLbl);
        card.setOnMouseClicked(e -> action.run());

        return card;
    }

    private void showCreateSensorDialog(String sensorType) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create " + sensorType);
        dialog.setHeaderText("Configure sensor");
        dialog.setResizable(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField codeField = new TextField(sensorType.substring(0, 3).toUpperCase() + String.format("%03d", sensorCounter));
        TextField minField = new TextField("0");
        TextField maxField = new TextField("100");

        ComboBox<String> zoneBox = new ComboBox<>();
        for (CropZone z : cropZones) zoneBox.getItems().add(z.getCode() + " - " + z.getName());
        for (LivestockZone z : livestockZones) zoneBox.getItems().add(z.getCode() + " - " + z.getName());
        for (AquacultureZone z : aquacultureZones) zoneBox.getItems().add(z.getCode() + " - " + z.getName());

        ComboBox<String> measureBox = new ComboBox<>();
        if (sensorType.equals("EnvironmentSensor")) measureBox.getItems().addAll("temperature", "humidity", "rainfall");
        else if (sensorType.equals("SoilSensor")) measureBox.getItems().addAll("ph", "moisture", "nitrogen");
        else if (sensorType.equals("WaterSensor")) measureBox.getItems().addAll("temperature", "dissolved_oxygen");
        else if (sensorType.equals("BiometricSensor")) measureBox.getItems().addAll("temperature", "activity");

        int row = 0;
        grid.add(new Label("Code:"), 0, row);
        grid.add(codeField, 1, row++);
        grid.add(new Label("Zone:"), 0, row);
        grid.add(zoneBox, 1, row++);
        grid.add(new Label("Min Threshold:"), 0, row);
        grid.add(minField, 1, row++);
        grid.add(new Label("Max Threshold:"), 0, row);
        grid.add(maxField, 1, row++);

        if (measureBox.getItems().size() > 0) {
            grid.add(new Label("Measurement Type:"), 0, row);
            grid.add(measureBox, 1, row++);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && zoneBox.getValue() != null) {
                try {
                    double min = Double.parseDouble(minField.getText());
                    double max = Double.parseDouble(maxField.getText());
                    String zoneInfo = zoneBox.getValue();
                    String zoneCode = zoneInfo.split(" - ")[0];
                    String measure = measureBox.getValue();

                    Sensor sensor = null;
                    switch (sensorType) {
                        case "EnvironmentSensor":
                            sensor = new EnvironmentSensor(codeField.getText(), zoneCode, min, max, measure);
                            break;
                        case "SoilSensor":
                            sensor = new SoilSensor(codeField.getText(), zoneCode, min, max, measure);
                            break;
                        case "WaterSensor":
                            sensor = new WaterSensor(codeField.getText(), zoneCode, min, max, measure);
                            break;
                        case "BiometricSensor":
                            sensor = new BiometricSensor(codeField.getText(), zoneCode, min, max, "ANIMAL_ID", measure);
                            break;
                        case "GPSSensor":
                            sensor = new GPSSensor(codeField.getText(), zoneCode, min, max, "ANIMAL_ID");
                            break;
                    }

                    if (sensor != null) {
                        addSensorToZone(sensor);
                        sensorCounter++;
                        saveAllData();
                        showInfoDialog("Success", "Sensor created!");
                        showSensors();
                    }
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid threshold values");
                }
            }
        });
    }

    private void showEditSensorDialog(Sensor sensor) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Sensor - " + sensor.getCode());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField minField = new TextField(String.valueOf(sensor.getThresholdMin()));
        TextField maxField = new TextField(String.valueOf(sensor.getThresholdMax()));
        ComboBox<SensorStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(SensorStatus.values()));
        statusBox.setValue(sensor.getStatus());

        grid.add(new Label("Min Threshold:"), 0, 0);
        grid.add(minField, 1, 0);
        grid.add(new Label("Max Threshold:"), 0, 1);
        grid.add(maxField, 1, 1);
        grid.add(new Label("Status:"), 0, 2);
        grid.add(statusBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    sensor.setThresholdMin(Double.parseDouble(minField.getText()));
                    sensor.setThresholdMax(Double.parseDouble(maxField.getText()));
                    sensor.setStatus(statusBox.getValue());
                    saveAllData();
                    showInfoDialog("Success", "Sensor updated!");
                    showSensors();
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number");
                }
            }
        });
    }

    private void showReadingHistory(Sensor sensor) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reading History - " + sensor.getCode());
        dialog.setResizable(true);
        dialog.setWidth(800);
        dialog.setHeight(500);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        TableView<Reading> table = new TableView<>();
        ObservableList<Reading> readings = FXCollections.observableArrayList(sensor.getReadings());
        table.setItems(readings);
        table.setPrefHeight(300);

        TableColumn<Reading, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        timeCol.setPrefWidth(200);

        TableColumn<Reading, Double> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getValue()).asObject());
        valueCol.setPrefWidth(150);

        TableColumn<Reading, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnit()));
        unitCol.setPrefWidth(100);

        table.getColumns().addAll(timeCol, valueCol, unitCol);

        // Add chart if there are readings
        if (!readings.isEmpty()) {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Reading Number");
            yAxis.setLabel("Value (" + sensor.getUnit() + ")");

            LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
            chart.setTitle("Sensor Readings Trend");
            chart.setPrefHeight(200);

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(sensor.getCode());
            for (int i = 0; i < readings.size(); i++) {
                series.getData().add(new XYChart.Data<>(i + 1, readings.get(i).getValue()));
            }
            chart.getData().add(series);
            content.getChildren().addAll(new Label("Reading Chart:"), chart);
        }

        content.getChildren().add(0, new Label("Readings for " + sensor.getCode()));
        content.getChildren().add(1, table);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showAddReadingDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Sensor Reading");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<Sensor> sensorBox = new ComboBox<>(FXCollections.observableArrayList(getAllSensors()));
        sensorBox.setPromptText("Select sensor");

        // Only show sensors that exist
        if (sensorBox.getItems().isEmpty()) {
            showWarningDialog("No Sensors", "Please create a sensor first before adding readings.");
            return;
        }

        TextField valueField = new TextField();
        valueField.setPromptText("Reading value");

        grid.add(new Label("Sensor:"), 0, 0);
        grid.add(sensorBox, 1, 0);
        grid.add(new Label("Value:"), 0, 1);
        grid.add(valueField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && sensorBox.getValue() != null) {
                try {
                    Sensor sensor = sensorBox.getValue();
                    double value = Double.parseDouble(valueField.getText());

                    Reading reading = new Reading(sensor.getCode(), value, sensor.getUnit(), LocalDateTime.now());
                    sensor.addReading(reading);

                    if (value < sensor.getThresholdMin() || value > sensor.getThresholdMax()) {
                        SeverityLevel severity = SeverityLevel.WARNING;
                        if (value < sensor.getThresholdMin() * 0.7 || value > sensor.getThresholdMax() * 1.3) {
                            severity = SeverityLevel.CRITICAL;
                        }
                        Alert alert = new Alert(
                                "ALT" + alertCounter++,
                                sensor.getCode(), value,
                                sensor.getThresholdMin(), sensor.getThresholdMax(),
                                severity, LocalDateTime.now()
                        );
                        activeAlerts.add(alert);
                        alertHistory.add(alert);
                        showWarningDialog("Alert Triggered", "Reading out of range! Severity: " + severity);
                    }

                    saveAllData();
                    showInfoDialog("Success", "Reading added!");
                    showSensors();
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number");
                }
            }
        });
    }

    private void showManualAlertDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Generate Manual Alert");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField sensorField = new TextField();
        sensorField.setPromptText("Sensor code");
        TextField valueField = new TextField();
        valueField.setPromptText("Value");
        TextField minField = new TextField("0");
        TextField maxField = new TextField("100");
        ComboBox<SeverityLevel> severityBox = new ComboBox<>(FXCollections.observableArrayList(SeverityLevel.values()));

        grid.add(new Label("Sensor:"), 0, 0);
        grid.add(sensorField, 1, 0);
        grid.add(new Label("Value:"), 0, 1);
        grid.add(valueField, 1, 1);
        grid.add(new Label("Min Threshold:"), 0, 2);
        grid.add(minField, 1, 2);
        grid.add(new Label("Max Threshold:"), 0, 3);
        grid.add(maxField, 1, 3);
        grid.add(new Label("Severity:"), 0, 4);
        grid.add(severityBox, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Alert alert = new Alert(
                            "ALT" + alertCounter++,
                            sensorField.getText(),
                            Double.parseDouble(valueField.getText()),
                            Double.parseDouble(minField.getText()),
                            Double.parseDouble(maxField.getText()),
                            severityBox.getValue(),
                            LocalDateTime.now()
                    );
                    activeAlerts.add(alert);
                    alertHistory.add(alert);
                    saveAllData();
                    showInfoDialog("Alert Generated", "Alert created!");
                    showDashboard();
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid value");
                }
            }
        });
    }

    private void loadSampleData() {
        // Only load if no data exists
        if (!cropZones.isEmpty() || !livestockZones.isEmpty() || !aquacultureZones.isEmpty()) {
            return;
        }

        // Create sample crop zone with REQUIRED family
        CropZone cropZone = new CropZone("CZ001", "North Valley Farm");
        cropZone.setAllowedCropFamily(CropFamily.CEREALS);
        cropZone.addCrop(new Crop("Winter Wheat", CropFamily.CEREALS,
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 7, 15), 6.0, 7.5, 20.0, 30.0));
        cropZones.add(cropZone);

        // Create sample livestock zone with REQUIRED type
        LivestockZone livestockZone = new LivestockZone("LZ001", "East Pasture");
        livestockZone.setAllowedAnimalType(AnimalType.RUMINANT);
        livestockZone.setFeedingProgram(new FeedingProgram("Organic Hay", 5.5, 3));
        Ruminant cow = new Ruminant("R1001", "Holstein Friesian", 4, 650.0);
        cow.addMilkYield(125.5);
        livestockZone.addAnimal(cow);
        livestockZones.add(livestockZone);

        // Create sample aquaculture zone
        AquacultureZone aquaZone = new AquacultureZone("AZ001", "West Pond");
        aquaZone.addSpecies("Tilapia");
        aquaZone.setAnimalCount(1250);
        aquaZone.setFeedingProgram(new FeedingProgram("Pellets", 3.5, 4));
        aquacultureZones.add(aquaZone);

        // Create sample sensors
        EnvironmentSensor sensor1 = new EnvironmentSensor("SENS101", "CZ001", 10.0, 35.0, "temperature");
        cropZone.addSensor(sensor1);
        sensor1.addReading(new Reading("SENS101", 23.5, "C", LocalDateTime.now()));

        // Create sample alert
        Alert alert = new Alert("ALT001", "SENS101", 36.5, 10.0, 35.0, SeverityLevel.CRITICAL, LocalDateTime.now());
        activeAlerts.add(alert);
        alertHistory.add(alert);

        zoneCounter = 2;
        sensorCounter = 102;

        saveAllData();
    }

    public static void main(String[] args) {
        launch(args);
    }
}