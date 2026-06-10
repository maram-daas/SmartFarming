import javafx.beans.property.SimpleDoubleProperty;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedAreaChart;
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
import model.utils.DashboardAggregator;
import model.utils.DataManager;
import model.utils.ProductionPricing;
import model.entities.ProductionEntry;
import model.zones.*;
import model.sensors.*;
import model.crops.*;
import model.animals.*;
import model.entities.*;
import model.enums.*;
import model.interfaces.Producing;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;



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

    /** Maps sidebar page keys to nav buttons for active-state styling. */
    private final Map<String, Button> navIdToButton = new LinkedHashMap<>();
    private String activeNavId = "dashboard";

    // Color palette (Soilless Farm Lab–style dashboard: forest green + lime accents)
    private final String PRIMARY_COLOR = "#2d6a4f";
    private final String SECONDARY_COLOR = "#f0f4f2";
    private final String SIDEBAR_BG = "#1b4332";
    private final String SIDEBAR_ACTIVE = "#52b788";
    private final String SIDEBAR_HOVER = "#2d6a4f";
    private final String CHART_DARK = "#1b4332";
    private final String CHART_LIGHT = "#95d5b2";
    private final String CHART_COST_COLOR = "#c62828";
    private final String CHART_REVENUE_COLOR = "#40916c";
    private final String CARD_SHADOW = "dropshadow(three-pass-box, rgba(27,67,50,0.14), 14, 0, 0, 2)";
    private final String DANGER_COLOR = "#c62828";
    private final String WARNING_COLOR = "#ff9800";
    private final String SUCCESS_COLOR = "#40916c";

    private Timer readingCheckerTimer;
    private boolean isAutoCheckEnabled = true;
    private int checkIntervalSeconds = 30;
    private final ProductionPricing productionPricing = new ProductionPricing();

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
        productionPricing.load();
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
        productionPricing.save();
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
                            alert.getThresholdMin(), alert.getThresholdMax(), alert.getSeverity() != null ? alert.getSeverity().toString() : "",
                            alert.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            String.valueOf(alert.isAcknowledged()), String.valueOf(alert.isDismissed()));
                }

                // ==================== SHEET 6: ACTIVE ALERTS ====================
                writer.println();
                writer.println("===== SHEET 6: ACTIVE ALERTS =====");
                writer.println();
                writer.println("ALERT ID,SENSOR CODE,READING VALUE,MIN THRESHOLD,MAX THRESHOLD,SEVERITY,TIMESTAMP");

                for (Alert alert : activeAlerts) {
                    writer.printf("%s,%s,%.2f,%.2f,%.2f,%s,%s%n",
                            alert.getId(), alert.getSensorCode(), alert.getReadingValue(),
                            alert.getThresholdMin(), alert.getThresholdMax(), alert.getSeverity() != null ? alert.getSeverity().toString() : "",
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
        contentArea.setPadding(new Insets(16, 24, 24, 24));
        contentArea.setStyle("-fx-background-color: " + SECONDARY_COLOR + ";");
        mainLayout.setCenter(contentArea);

        showDashboard();

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double screenWidth = screenBounds.getWidth() * 0.9;
        double screenHeight = screenBounds.getHeight() * 0.85;

        Scene scene = new Scene(mainLayout, screenWidth, screenHeight);
        primaryStage.setMaximized(true);
        primaryStage.setTitle("Smart Farm Lab");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setActiveNavigation(String navId) {
        this.activeNavId = navId;
        for (Map.Entry<String, Button> e : navIdToButton.entrySet()) {
            styleSidebarNavButton(e.getValue(), navId.equals(e.getKey()));
        }
    }

    private void styleSidebarNavButton(Button btn, boolean active) {
        if (active) {
            btn.setStyle("-fx-background-color: " + SIDEBAR_ACTIVE + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 10 16;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 10 16;");
        }
    }

    private void wireSidebarNavHover(Button btn, String navId) {
        btn.setOnMouseEntered(e -> {
            if (!navId.equals(activeNavId)) {
                btn.setStyle("-fx-background-color: " + SIDEBAR_HOVER + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 10 16;");
            }
        });
        btn.setOnMouseExited(e -> styleSidebarNavButton(btn, navId.equals(activeNavId)));
    }

    private Button createPageNavButton(String navId, String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        styleSidebarNavButton(btn, navId.equals(activeNavId));
        wireSidebarNavHover(btn, navId);
        btn.setOnAction(e -> {
            setActiveNavigation(navId);
            action.run();
        });
        navIdToButton.put(navId, btn);
        return btn;
    }

    private Button createSidebarToolButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        String inactive = "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.78); -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 10 16;";
        String hover = "-fx-background-color: " + SIDEBAR_HOVER + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 10 16;";
        btn.setStyle(inactive);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(inactive));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private Button createLogoutButton() {
        Button btn = new Button("Log out");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 16, 12, 16));
        String base = "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.95); -fx-font-size: 14px; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.4); -fx-border-radius: 10; -fx-background-radius: 10;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setOpacity(0.88));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        btn.setOnAction(e -> {
            saveAllData();
            System.exit(0);
        });
        return btn;
    }

    private VBox createSidebar() {
        navIdToButton.clear();
        VBox box = new VBox(6);
        box.setPadding(new Insets(28, 18, 24, 18));
        box.setPrefWidth(272);
        box.setStyle("-fx-background-color: " + SIDEBAR_BG + ";");

        Label brand = new Label("🌱  Smart Farm Lab");
        brand.setFont(Font.font("System", FontWeight.BOLD, 18));
        brand.setTextFill(Color.WHITE);
        brand.setWrapText(true);
        brand.setMaxWidth(Double.MAX_VALUE);
        brand.setPadding(new Insets(0, 0, 4, 0));

        Label tag = new Label("Operations console");
        tag.setFont(Font.font("System", 11));
        tag.setTextFill(Color.color(1, 1, 1, 0.72));
        tag.setPadding(new Insets(0, 0, 20, 0));

        box.getChildren().addAll(brand, tag);

        box.getChildren().addAll(
                createPageNavButton("dashboard", "Dashboard", this::showDashboard),
                createPageNavButton("crop", "Crop zones", () -> showZones("crop")),
                createPageNavButton("livestock", "Livestock zones", () -> showZones("livestock")),
                createPageNavButton("aquaculture", "Aquaculture zones", () -> showZones("aquaculture")),
                createPageNavButton("sensors", "Sensors", this::showSensors),
                createPageNavButton("alerts", "Alerts", this::showAlerts),
                createPageNavButton("production", "Production", this::showProductionRecords),
                createPageNavButton("reports", "Reports", this::showReports));

        Separator sep = new Separator();
        sep.setPadding(new Insets(10, 0, 10, 0));
        sep.setOpacity(0.35);

        box.getChildren().add(sep);
        box.getChildren().addAll(
                createSidebarToolButton("+  New crop zone", this::showCreateCropZoneDialog),
                createSidebarToolButton("+  New livestock zone", this::showCreateLivestockZoneDialog),
                createSidebarToolButton("+  New aquaculture zone", this::showCreateAquacultureZoneDialog),
                createSidebarToolButton("+  New sensor", this::showCreateSensorMenu),
                createSidebarToolButton("⚙  Settings", this::showSettingsDialog));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        box.getChildren().add(spacer);

        Label stats = new Label(String.format("Zones %d  •  Sensors %d  •  Active alerts %d",
                cropZones.size() + livestockZones.size() + aquacultureZones.size(),
                getAllSensors().size(), activeAlerts.size()));
        stats.setTextFill(Color.color(1, 1, 1, 0.78));
        stats.setFont(Font.font("System", 11));
        stats.setWrapText(true);
        stats.setPadding(new Insets(0, 0, 14, 0));
        box.getChildren().addAll(stats, createLogoutButton());

        return box;
    }

    private void setPageTitle(String title) {
        if (currentPageTitle == null) {
            currentPageTitle = new Label(title);
            currentPageTitle.setFont(Font.font("System", FontWeight.BOLD, 26));
            currentPageTitle.setTextFill(Color.web(SIDEBAR_BG));
            currentPageTitle.setPadding(new Insets(0, 0, 20, 0));
        } else {
            currentPageTitle.setText(title);
            currentPageTitle.setTextFill(Color.web(SIDEBAR_BG));
        }
    }

    private void showInContentArea(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        contentArea.getChildren().setAll(sp);
    }

    private VBox wrapDashboardCard(String title, Node topRight, Node body) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: " + CARD_SHADOW + ";");

        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);
        if (title != null) {
            Label t = new Label(title);
            t.setFont(Font.font("System", FontWeight.BOLD, 15));
            t.setTextFill(Color.web(SIDEBAR_BG));
            head.getChildren().add(t);
        }
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        head.getChildren().add(sp);
        if (topRight != null) {
            head.getChildren().add(topRight);
        }
        card.getChildren().add(head);

        if (body != null) {
            card.getChildren().add(body);
            VBox.setVgrow(body, Priority.ALWAYS);
        }
        return card;
    }

    private VBox createDashboardMiniStat(String icon, String value, String caption) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setPrefSize(158, 120);
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: " + CARD_SHADOW + ";");
        card.setAlignment(Pos.TOP_LEFT);

        Label ic = new Label(icon);
        ic.setFont(Font.font(18));

        Label val = new Label(value);
        val.setFont(Font.font("System", FontWeight.BOLD, 26));
        val.setTextFill(Color.web(PRIMARY_COLOR));

        Label cap = new Label(caption);
        cap.setFont(Font.font("System", 11));
        cap.setTextFill(Color.web("#6c757d"));
        cap.setWrapText(true);

        card.getChildren().addAll(ic, val, cap);
        return card;
    }

    private HBox createChartLegend(String[][] items) {
        HBox legend = new HBox(18);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(10, 0, 4, 0));
        for (String[] item : items) {
            Region swatch = new Region();
            swatch.setPrefSize(14, 14);
            swatch.setStyle("-fx-background-color: " + item[0] + "; -fx-background-radius: 7;");
            Label label = new Label(item[1]);
            label.setFont(Font.font("System", 11));
            label.setTextFill(Color.web("#495057"));
            HBox row = new HBox(6, swatch, label);
            row.setAlignment(Pos.CENTER_LEFT);
            legend.getChildren().add(row);
        }
        return legend;
    }

    private void styleLineChartSeries(LineChart<String, Number> chart, String costColor, String revenueColor) {
        Platform.runLater(() -> {
            for (int i = 0; i < chart.getData().size(); i++) {
                XYChart.Series<String, Number> series = chart.getData().get(i);
                String color = i == 0 ? costColor : revenueColor;
                Node seriesNode = series.getNode();
                if (seriesNode != null) {
                    seriesNode.setStyle("-fx-stroke: " + color + ";");
                }
                for (XYChart.Data<String, Number> data : series.getData()) {
                    Node symbol = data.getNode();
                    if (symbol != null) {
                        symbol.setStyle("-fx-background-color: " + color + ", white; -fx-background-radius: 6;");
                    }
                }
            }
        });
    }

    private VBox buildCostRevenueChart() {
        List<DashboardAggregator.MonthlyFinancial> monthly = DashboardAggregator.buildMonthlyFinancials(
                cropZones, livestockZones, aquacultureZones, productionPricing);

        double maxValue = 1.0;
        for (DashboardAggregator.MonthlyFinancial point : monthly) {
            maxValue = Math.max(maxValue, Math.max(point.cost(), point.revenue()));
        }
        double axisMax = Math.ceil(maxValue * 1.2 * 10.0) / 10.0;
        double tick = axisMax <= 1 ? 0.1 : axisMax <= 5 ? 0.5 : Math.ceil(axisMax / 5.0);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, axisMax, tick);
        yAxis.setMinorTickVisible(false);
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(null);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setMinHeight(280);
        chart.setPrefHeight(300);
        chart.setVerticalGridLinesVisible(false);
        chart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> sCost = new XYChart.Series<>();
        sCost.setName("Cost");
        XYChart.Series<String, Number> sRev = new XYChart.Series<>();
        sRev.setName("Revenue");
        for (DashboardAggregator.MonthlyFinancial point : monthly) {
            sCost.getData().add(new XYChart.Data<>(point.label(), point.cost()));
            sRev.getData().add(new XYChart.Data<>(point.label(), point.revenue()));
        }
        chart.getData().addAll(sCost, sRev);
        styleLineChartSeries(chart, CHART_COST_COLOR, CHART_REVENUE_COLOR);

        VBox box = new VBox(4);
        box.getChildren().addAll(chart, createChartLegend(new String[][]{
                {CHART_COST_COLOR, "Cost — monthly operating expenses (DA)"},
                {CHART_REVENUE_COLOR, "Revenue — sales from production records (DA)"}
        }));
        return box;
    }

    private VBox buildAlertsSeverityChart() {
        Map<String, Integer> alerts = DashboardAggregator.buildAlertsBySeverity(alertHistory);
        Map<String, String> severityColors = Map.of(
                "Critical", DANGER_COLOR,
                "Warning", WARNING_COLOR,
                "Other", PRIMARY_COLOR,
                "No alerts", "#b0bec5"
        );

        int total = alerts.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            total = 1;
        }

        VBox rows = new VBox(16);
        rows.setPadding(new Insets(12, 4, 8, 4));
        rows.setMinHeight(220);

        for (Map.Entry<String, Integer> entry : alerts.entrySet()) {
            String color = severityColors.getOrDefault(entry.getKey(), CHART_DARK);
            int count = entry.getValue();
            double share = count / (double) total;

            Label label = new Label(entry.getKey() + "  —  " + count + " alert(s)");
            label.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
            label.setTextFill(Color.web("#495057"));

            ProgressBar bar = new ProgressBar(share);
            bar.setPrefHeight(12);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setStyle("-fx-accent: " + color + "; -fx-control-inner-background: #e8ece9;");

            rows.getChildren().addAll(label, bar);
        }

        List<String[]> legendItems = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : alerts.entrySet()) {
            String color = severityColors.getOrDefault(entry.getKey(), CHART_DARK);
            legendItems.add(new String[]{color, entry.getKey() + " — share of total alerts"});
        }

        VBox box = new VBox(8);
        box.getChildren().addAll(rows, createChartLegend(legendItems.toArray(new String[0][])));
        return box;
    }

    private VBox buildInventoryDonutChart(DashboardAggregator.InventoryPeriod period) {
        Map<String, Double> mix = DashboardAggregator.buildInventoryMix(
                cropZones, livestockZones, aquacultureZones, period);

        String[] pieColors = {CHART_DARK, CHART_LIGHT, PRIMARY_COLOR, SUCCESS_COLOR, WARNING_COLOR};
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        List<String[]> legendItems = new ArrayList<>();
        int colorIndex = 0;

        for (Map.Entry<String, Double> entry : mix.entrySet()) {
            String color = pieColors[colorIndex % pieColors.length];
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            legendItems.add(new String[]{color, entry.getKey() + " — " + String.format("%.1f", entry.getValue())});
            colorIndex++;
        }

        PieChart pie = new PieChart(pieData);
        pie.setLabelsVisible(false);
        pie.setLegendVisible(false);
        pie.setAnimated(false);
        pie.setMinHeight(200);
        pie.setStyle("-fx-background-color: transparent;");

        for (int i = 0; i < pie.getData().size(); i++) {
            final int idx = i;
            final String color = pieColors[idx % pieColors.length];
            PieChart.Data slice = pie.getData().get(i);
            Platform.runLater(() -> {
                if (slice.getNode() != null) {
                    slice.getNode().setStyle("-fx-pie-color: " + color + ";");
                }
            });
        }

        VBox box = new VBox(4);
        box.getChildren().addAll(pie, createChartLegend(legendItems.toArray(new String[0][])));
        return box;
    }

    private DashboardAggregator.InventoryPeriod parseInventoryPeriod(String label) {
        if ("This year".equals(label)) {
            return DashboardAggregator.InventoryPeriod.THIS_YEAR;
        }
        if ("This month".equals(label)) {
            return DashboardAggregator.InventoryPeriod.THIS_MONTH;
        }
        return DashboardAggregator.InventoryPeriod.THIS_WEEK;
    }

    private void addZoneShareRow(VBox v, String name, int pct) {
        Label l = new Label(name + "  —  " + pct + "%");
        l.setFont(Font.font("System", 12));
        l.setTextFill(Color.web("#495057"));
        ProgressBar p = new ProgressBar(Math.min(1, Math.max(0, pct / 100.0)));
        p.setPrefHeight(10);
        p.setMaxWidth(Double.MAX_VALUE);
        p.setStyle("-fx-accent: " + CHART_DARK + "; -fx-control-inner-background: #e8ece9;");
        v.getChildren().addAll(l, p);
    }

    private VBox buildZoneSharePanel() {
        VBox v = new VBox(12);
        Label mapStub = new Label("Regional footprint (illustrative map)\nDark green: accessed   ·   Light green: planned");
        mapStub.setWrapText(true);
        mapStub.setStyle("-fx-text-fill: #495057; -fx-font-size: 12px; -fx-padding: 14; -fx-background-color: #edf3ef; -fx-background-radius: 10;");
        mapStub.setMaxWidth(Double.MAX_VALUE);
        v.getChildren().add(mapStub);

        int tz = cropZones.size() + livestockZones.size() + aquacultureZones.size();
        if (tz < 1) {
            tz = 1;
        }
        int cropPct = (int) Math.round(100.0 * cropZones.size() / tz);
        int livPct = (int) Math.round(100.0 * livestockZones.size() / tz);
        int aqPct = (int) Math.round(100.0 * aquacultureZones.size() / tz);
        addZoneShareRow(v, "Crop zones", cropPct);
        addZoneShareRow(v, "Livestock zones", livPct);
        addZoneShareRow(v, "Aquaculture zones", aqPct);
        return v;
    }

    private void showDashboard() {
        showDashboard(false);
    }

    private void showDashboard(boolean reloadFromFiles) {
        if (reloadFromFiles) {
            loadDataFromFile();
        }
        setActiveNavigation("dashboard");

        VBox root = new VBox(22);
        root.setPadding(new Insets(4, 0, 32, 0));

        Label welcome = new Label("Dashboard");
        welcome.setFont(Font.font("System", FontWeight.BOLD, 26));
        welcome.setTextFill(Color.web(SIDEBAR_BG));

        int totalZones = cropZones.size() + livestockZones.size() + aquacultureZones.size();
        int totalCrops = cropZones.stream().mapToInt(CropZone::getEntityCount).sum();
        int totalAnimals = livestockZones.stream().mapToInt(LivestockZone::getEntityCount).sum() +
                aquacultureZones.stream().mapToInt(AquacultureZone::getAnimalCount).sum();
        int sensorCount = getAllSensors().size();

        HBox row1 = new HBox(20);
        row1.setAlignment(Pos.TOP_LEFT);

        VBox chartCard = wrapDashboardCard("Cost and revenue (DA)", null, buildCostRevenueChart());
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        chartCard.setMinWidth(400);

        GridPane kpis = new GridPane();
        kpis.setHgap(14);
        kpis.setVgap(14);
        kpis.add(createDashboardMiniStat("🏞", String.valueOf(totalZones), "Total zones"), 0, 0);
        kpis.add(createDashboardMiniStat("🌾", String.valueOf(totalCrops), "Total crops"), 1, 0);
        kpis.add(createDashboardMiniStat("🐟", String.valueOf(totalAnimals), "Animals & fish"), 0, 1);
        kpis.add(createDashboardMiniStat("📡", String.valueOf(sensorCount), "Sensors"), 1, 1);

        row1.getChildren().addAll(chartCard, kpis);

        HBox row2 = new HBox(20);
        row2.setAlignment(Pos.TOP_LEFT);

        VBox barCard = wrapDashboardCard("Alerts by severity", null, buildAlertsSeverityChart());
        HBox.setHgrow(barCard, Priority.ALWAYS);

        ComboBox<String> weekPick = new ComboBox<>(FXCollections.observableArrayList("This week", "This month", "This year"));
        weekPick.setValue("This month");
        weekPick.setStyle("-fx-background-radius: 8;");

        VBox donutBody = new VBox();
        VBox donutChart = buildInventoryDonutChart(parseInventoryPeriod(weekPick.getValue()));
        donutBody.getChildren().add(donutChart);
        weekPick.valueProperty().addListener((obs, oldVal, newVal) -> {
            donutBody.getChildren().set(0, buildInventoryDonutChart(parseInventoryPeriod(newVal)));
        });

        VBox donutCard = wrapDashboardCard("Inventory mix", weekPick, donutBody);
        donutCard.setPrefWidth(300);

        VBox marketCard = wrapDashboardCard("Market share", null, buildZoneSharePanel());
        marketCard.setPrefWidth(300);

        row2.getChildren().addAll(barCard, donutCard, marketCard);

        HBox tableHeader = new HBox(12);
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        Label tx = new Label("Transaction history");
        tx.setFont(Font.font("System", FontWeight.BOLD, 16));
        tx.setTextFill(Color.web(SIDEBAR_BG));
        Region rGrow = new Region();
        HBox.setHgrow(rGrow, Priority.ALWAYS);
        TextField tableSearch = new TextField();
        tableSearch.setPromptText("Search…");
        tableSearch.setPrefWidth(220);
        tableSearch.setStyle("-fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #dce5df; -fx-background-color: #f7faf8;");
        Button filt = new Button("▼");
        filt.setStyle("-fx-background-color: #eef2f0; -fx-background-radius: 8; -fx-cursor: hand;");
        filt.setOnAction(e -> showInfoDialog("Filters", "Filter controls are not wired to sample data."));
        tableHeader.getChildren().addAll(tx, rGrow, tableSearch, filt);

        VBox alertsBox = createAlertTableView(activeAlerts, true);

        VBox tableCard = new VBox(16);
        tableCard.setPadding(new Insets(20));
        tableCard.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: " + CARD_SHADOW + ";");
        tableCard.getChildren().addAll(tableHeader, alertsBox);

        HBox actions = new HBox(10);
        actions.setPadding(new Insets(4, 0, 0, 0));
        actions.getChildren().addAll(
                createActionButton("Unit prices", PRIMARY_COLOR, this::showProductionPricingDialog),
                createActionButton("Generate alert", WARNING_COLOR, this::showManualAlertDialog),
                createActionButton("Export report", SUCCESS_COLOR, this::exportReport),
                createActionButton("Refresh", "#607d8b", () -> showDashboard(true))
        );

        root.getChildren().addAll(welcome, row1, row2, createProductionRecordsCard(), tableCard, actions);
        showInContentArea(root);
    }

    private class ProductionSummaryRow {
        private final String zoneCode;
        private final String zoneName;
        private final String entityName;
        private final String entityType;
        private final String unit;
        private final int entries;
        private final double total;
        private final double average;
        private final double revenue;
        private final Producing producer;

        ProductionSummaryRow(String zoneCode, String zoneName, String entityName, String entityType,
                             Producing producer) {
            this.zoneCode = zoneCode;
            this.zoneName = zoneName;
            this.entityName = entityName;
            this.entityType = entityType;
            this.producer = producer;
            this.unit = producer.getProductionRecord().getUnit();
            this.entries = producer.getProductionRecord().getEntries().size();
            this.total = producer.getProduction();
            this.average = producer.getProductionRecord().getAverageProduction();
            this.revenue = productionPricing.calculateRevenue(producer.getProductionRecord());
        }

        public String getZoneCode() { return zoneCode; }
        public String getZoneName() { return zoneName; }
        public String getEntityName() { return entityName; }
        public String getEntityType() { return entityType; }
        public String getUnit() { return unit; }
        public int getEntries() { return entries; }
        public double getTotal() { return total; }
        public double getAverage() { return average; }
        public double getRevenue() { return revenue; }
        public Producing getProducer() { return producer; }
    }

    private List<ProductionSummaryRow> collectProductionSummaries() {
        List<ProductionSummaryRow> rows = new ArrayList<>();
        for (CropZone zone : cropZones) {
            for (Crop crop : zone.getCrops()) {
                rows.add(new ProductionSummaryRow(
                        zone.getCode(), zone.getName(), crop.getName(), "Crop", crop));
            }
        }
        for (LivestockZone zone : livestockZones) {
            for (Animal animal : zone.getAnimals()) {
                if (animal instanceof Producing producing) {
                    rows.add(new ProductionSummaryRow(
                            zone.getCode(), zone.getName(), animal.getId(),
                            animal.getAnimalType().toString(), producing));
                }
            }
        }
        return rows;
    }

    private VBox createProductionRecordsTable(boolean showActions, Runnable refreshCallback) {
        VBox container = new VBox(10);
        ObservableList<ProductionSummaryRow> items =
                FXCollections.observableArrayList(collectProductionSummaries());

        TableView<ProductionSummaryRow> table = new TableView<>(items);
        table.setPrefHeight(320);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(tv -> new TableRow<ProductionSummaryRow>() {
            @Override
            protected void updateItem(ProductionSummaryRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setStyle("");
                } else if (getIndex() % 2 == 0) {
                    setStyle("-fx-background-color: #f8faf9;");
                } else {
                    setStyle("-fx-background-color: #eef4ef;");
                }
            }
        });

        TableColumn<ProductionSummaryRow, String> zoneCol = new TableColumn<>("Zone");
        zoneCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getZoneCode()));

        TableColumn<ProductionSummaryRow, String> nameCol = new TableColumn<>("Entity");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEntityName()));

        TableColumn<ProductionSummaryRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEntityType()));

        TableColumn<ProductionSummaryRow, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUnit()));

        TableColumn<ProductionSummaryRow, Integer> entriesCol = new TableColumn<>("Entries");
        entriesCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getEntries()).asObject());

        TableColumn<ProductionSummaryRow, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.2f %s", d.getValue().getTotal(), d.getValue().getUnit())));

        TableColumn<ProductionSummaryRow, String> avgCol = new TableColumn<>("Average");
        avgCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEntries() == 0 ? "—" :
                        String.format("%.2f %s", d.getValue().getAverage(), d.getValue().getUnit())));

        TableColumn<ProductionSummaryRow, String> revenueCol = new TableColumn<>("Revenue (DA)");
        revenueCol.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.2f", d.getValue().getRevenue())));

        table.getColumns().addAll(zoneCol, nameCol, typeCol, unitCol, entriesCol, totalCol, avgCol, revenueCol);

        if (showActions) {
            TableColumn<ProductionSummaryRow, Void> actionsCol = new TableColumn<>("Actions");
            actionsCol.setPrefWidth(120);
            actionsCol.setCellFactory(col -> new TableCell<ProductionSummaryRow, Void>() {
                private final Button viewBtn = new Button("View / Add");

                {
                    viewBtn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4;");
                    viewBtn.setOnAction(e -> {
                        ProductionSummaryRow row = getTableView().getItems().get(getIndex());
                        showProductionRecordDialog(
                                row.getProducer(),
                                row.getEntityName() + " (" + row.getZoneCode() + ")",
                                () -> {
                                    items.setAll(collectProductionSummaries());
                                    if (refreshCallback != null) {
                                        refreshCallback.run();
                                    }
                                });
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : viewBtn);
                }
            });
            table.getColumns().add(actionsCol);
        }

        if (items.isEmpty()) {
            Label empty = new Label("No production records yet. Add crops or animals, then record harvest, milk, or egg production.");
            empty.setWrapText(true);
            empty.setTextFill(Color.web("#6c757d"));
            empty.setPadding(new Insets(20, 0, 0, 0));
            container.getChildren().addAll(empty, table);
        } else {
            container.getChildren().add(table);
        }
        return container;
    }

    private VBox createProductionRecordsCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: " + CARD_SHADOW + ";");

        Label title = new Label("Production records");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.web(SIDEBAR_BG));

        card.getChildren().addAll(title, createProductionRecordsTable(true, this::showDashboard));
        return card;
    }

    private void showProductionRecords() {
        setActiveNavigation("production");
        VBox container = new VBox(15);
        setPageTitle("Production records");
        container.getChildren().add(currentPageTitle);
        HBox actions = new HBox(10);
        actions.getChildren().add(createActionButton("Unit prices", PRIMARY_COLOR, this::showProductionPricingDialog));
        container.getChildren().add(actions);
        container.getChildren().add(createProductionRecordsTable(true, this::showProductionRecords));
        showInContentArea(container);
    }

    private void showProductionRecordDialog(Producing producer, String title, Runnable onUpdate) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Production — " + title);
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(520, 460);

        ProductionRecord record = producer.getProductionRecord();
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        Label stats = new Label(String.format(
                "Unit: %s   |   Entries: %d   |   Total: %.2f   |   Average: %.2f   |   Revenue: %.2f DA",
                record.getUnit(),
                record.getEntries().size(),
                record.getTotalProduction(),
                record.getAverageProduction(),
                productionPricing.calculateRevenue(record)));
        stats.setFont(Font.font("System", FontWeight.BOLD, 13));
        stats.setTextFill(Color.web(SIDEBAR_BG));

        ListView<String> history = new ListView<>();
        history.setPrefHeight(220);
        refreshProductionHistoryList(history, record);

        Label todayPriceLabel = new Label();
        todayPriceLabel.setFont(Font.font(11));
        todayPriceLabel.setTextFill(Color.web("#6c757d"));
        Runnable refreshTodayPrice = () -> {
            LocalDate today = LocalDate.now();
            double price = productionPricing.getPriceForDate(record.getUnit(), today);
            todayPriceLabel.setText(price > 0
                    ? String.format("Price for today (%s): %.2f DA / %s", today, price, record.getUnit())
                    : String.format("No price set for today (%s) — set it below before recording.", today));
        };
        refreshTodayPrice.run();

        HBox priceRow = new HBox(10);
        TextField todayPriceField = new TextField();
        todayPriceField.setPromptText("Price for today (DA/" + record.getUnit() + ")");
        todayPriceField.setPrefWidth(200);
        Button setPriceBtn = createActionButton("Set today's price", SUCCESS_COLOR, () -> {
            try {
                double price = Double.parseDouble(todayPriceField.getText().trim().replace(',', '.'));
                if (price < 0) {
                    showWarningDialog("Invalid price", "Price must be zero or positive.");
                    return;
                }
                productionPricing.setDailyPrice(record.getUnit(), LocalDate.now(), price);
                saveAllData();
                refreshTodayPrice.run();
                todayPriceField.clear();
                stats.setText(String.format(
                        "Unit: %s   |   Entries: %d   |   Total: %.2f   |   Average: %.2f   |   Revenue: %.2f DA",
                        record.getUnit(),
                        record.getEntries().size(),
                        record.getTotalProduction(),
                        record.getAverageProduction(),
                        productionPricing.calculateRevenue(record)));
                refreshProductionHistoryList(history, record);
            } catch (NumberFormatException ex) {
                showErrorDialog("Invalid price", "Please enter a valid price.");
            }
        });
        priceRow.getChildren().addAll(todayPriceField, setPriceBtn);

        HBox addRow = new HBox(10);
        TextField amountField = new TextField();
        amountField.setPromptText("Amount (" + record.getUnit() + ")");
        amountField.setPrefWidth(200);
        Button addBtn = createActionButton("Record", PRIMARY_COLOR, () -> {
            try {
                double amount = Double.parseDouble(amountField.getText().trim().replace(',', '.'));
                if (amount <= 0) {
                    showWarningDialog("Invalid amount", "Enter a value greater than zero.");
                    return;
                }
                LocalDate today = LocalDate.now();
                if (productionPricing.getPriceForDate(record.getUnit(), today) <= 0) {
                    showWarningDialog("No price for today",
                            "Set today's unit price before recording so revenue can be calculated.");
                    return;
                }
                producer.recordProduction(amount, LocalDateTime.now());
                saveAllData();
                stats.setText(String.format(
                        "Unit: %s   |   Entries: %d   |   Total: %.2f   |   Average: %.2f   |   Revenue: %.2f DA",
                        record.getUnit(),
                        record.getEntries().size(),
                        record.getTotalProduction(),
                        record.getAverageProduction(),
                        productionPricing.calculateRevenue(record)));
                refreshProductionHistoryList(history, record);
                amountField.clear();
                if (onUpdate != null) {
                    onUpdate.run();
                }
            } catch (NumberFormatException ex) {
                showErrorDialog("Invalid number", "Please enter a valid amount.");
            }
        });
        addRow.getChildren().addAll(amountField, addBtn);

        content.getChildren().addAll(stats, todayPriceLabel, priceRow, new Label("History:"), history, addRow);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void refreshProductionHistoryList(ListView<String> history, ProductionRecord record) {
        history.getItems().clear();
        List<ProductionEntry> entries = record.getEntries();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (int i = 0; i < entries.size(); i++) {
            ProductionEntry entry = entries.get(i);
            double entryRevenue = productionPricing.calculateRevenue(
                    entry.getAmount(), record.getUnit(), entry.getRecordedAt().toLocalDate());
            history.getItems().add(String.format("#%d — %.2f %s on %s (%.2f DA)",
                    i + 1, entry.getAmount(), record.getUnit(),
                    entry.getRecordedAt().format(fmt), entryRevenue));
        }
        if (entries.isEmpty()) {
            history.getItems().add("No entries recorded yet.");
        }
    }

    private void showProductionPricingDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Daily unit prices");
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(620, 480);
        dialog.setHeaderText("Set the selling price per unit for each day (DA). Revenue uses the price of the production date.");

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));

        ObservableList<ProductionPricing.DailyPriceEntry> priceRows =
                FXCollections.observableArrayList(productionPricing.getAllDailyPrices());

        TableView<ProductionPricing.DailyPriceEntry> table = new TableView<>(priceRows);
        table.setPrefHeight(220);

        TableColumn<ProductionPricing.DailyPriceEntry, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(d -> new SimpleStringProperty(formatUnitLabel(d.getValue().unit())));

        TableColumn<ProductionPricing.DailyPriceEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().date().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));

        TableColumn<ProductionPricing.DailyPriceEntry, String> priceCol = new TableColumn<>("Price (DA)");
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.2f", d.getValue().price())));

        table.getColumns().addAll(unitCol, dateCol, priceCol);

        ComboBox<String> unitBox = new ComboBox<>(FXCollections.observableArrayList("L", "eggs", "kg"));
        unitBox.setValue("L");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField priceField = new TextField();
        priceField.setPromptText("Price in DA");

        Button addBtn = createActionButton("Add / Update", PRIMARY_COLOR, () -> {
            try {
                double price = Double.parseDouble(priceField.getText().trim().replace(',', '.'));
                if (price < 0) {
                    showWarningDialog("Invalid price", "Price must be zero or positive.");
                    return;
                }
                String unit = unitBox.getValue();
                LocalDate date = datePicker.getValue();
                productionPricing.setDailyPrice(unit, date, price);
                priceRows.setAll(productionPricing.getAllDailyPrices());
                priceField.clear();
            } catch (NumberFormatException ex) {
                showErrorDialog("Invalid price", "Please enter a valid number.");
            }
        });

        Button removeBtn = createActionButton("Remove selected", DANGER_COLOR, () -> {
            ProductionPricing.DailyPriceEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showWarningDialog("No selection", "Select a price row to remove.");
                return;
            }
            productionPricing.removeDailyPrice(selected.unit(), selected.date());
            priceRows.setAll(productionPricing.getAllDailyPrices());
        });

        GridPane addGrid = new GridPane();
        addGrid.setHgap(10);
        addGrid.setVgap(10);
        addGrid.add(new Label("Unit:"), 0, 0);
        addGrid.add(unitBox, 1, 0);
        addGrid.add(new Label("Date:"), 0, 1);
        addGrid.add(datePicker, 1, 1);
        addGrid.add(new Label("Price (DA):"), 0, 2);
        addGrid.add(priceField, 1, 2);

        HBox addActions = new HBox(10, addBtn, removeBtn);
        Label hint = new Label("Each production entry uses the price defined for its date. If a day has no price, the nearest earlier price is used.");
        hint.setWrapText(true);
        hint.setTextFill(Color.web("#6c757d"));
        hint.setFont(Font.font(11));

        content.getChildren().addAll(table, addGrid, addActions, hint);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            saveAllData();
            showInfoDialog("Prices saved", "Daily unit prices saved.");
            showDashboard();
        } else {
            productionPricing.load();
        }
    }

    private String formatUnitLabel(String unit) {
        return switch (unit) {
            case "L" -> "Milk (L)";
            case "eggs" -> "Eggs";
            case "kg" -> "Harvest (kg)";
            default -> unit;
        };
    }

    private Zone findZoneByCode(String zoneCode) {
        for (CropZone zone : cropZones) {
            if (zone.getCode().equals(zoneCode)) {
                return zone;
            }
        }
        for (LivestockZone zone : livestockZones) {
            if (zone.getCode().equals(zoneCode)) {
                return zone;
            }
        }
        for (AquacultureZone zone : aquacultureZones) {
            if (zone.getCode().equals(zoneCode)) {
                return zone;
            }
        }
        return null;
    }

    private boolean canOperateSensor(Sensor sensor) {
        if (sensor.getStatus() != SensorStatus.ACTIVE) {
            return false;
        }
        Zone zone = findZoneByCode(sensor.getZoneCode());
        return zone == null || zone.getStatus() == ZoneStatus.ACTIVE;
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
        if ("crop".equals(type)) {
            setActiveNavigation("crop");
        } else if ("livestock".equals(type)) {
            setActiveNavigation("livestock");
        } else {
            setActiveNavigation("aquaculture");
        }

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

        showInContentArea(container);
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
        dialog.getDialogPane().setPrefSize(800, 600);

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

        TableColumn<Crop, String> productionCol = new TableColumn<>("Harvest (kg)");
        productionCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("%.2f", data.getValue().getProduction())));
        productionCol.setPrefWidth(90);

        TableColumn<Crop, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(180);
        actionsCol.setCellFactory(col -> new TableCell<Crop, Void>() {
            private final Button removeBtn = new Button("Remove");
            private final Button prodBtn = new Button("Production");
            private final HBox pane = new HBox(5, prodBtn, removeBtn);

            {
                removeBtn.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                prodBtn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");

                removeBtn.setOnAction(e -> {
                    Crop crop = getTableView().getItems().get(getIndex());
                    zone.getCrops().remove(crop);
                    table.setItems(FXCollections.observableArrayList(zone.getCrops()));
                    saveAllData();
                    showInfoDialog("Removed", "Crop '" + crop.getName() + "' removed");
                });

                prodBtn.setOnAction(e -> {
                    Crop crop = getTableView().getItems().get(getIndex());
                    showProductionRecordDialog(crop, crop.getName() + " @ " + zone.getCode(), () ->
                            table.setItems(FXCollections.observableArrayList(zone.getCrops())));
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(nameCol, familyCol, stageCol, plantingCol, harvestCol, phCol, productionCol, actionsCol);

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
        dialog.getDialogPane().setPrefSize(650, 550);

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
        setActiveNavigation("sensors");
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

        showInContentArea(container);
    }

    private void showAlerts() {
        setActiveNavigation("alerts");
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

        showInContentArea(container);
    }

    private VBox createAlertTableView(ObservableList<model.entities.Alert> alerts, boolean showActions) {
        VBox container = new VBox(10);

        TableView<model.entities.Alert> table = new TableView<>();
        table.setItems(alerts);
        table.setPrefHeight(400);
        table.setRowFactory(tv -> new TableRow<model.entities.Alert>() {
            @Override
            protected void updateItem(model.entities.Alert item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setStyle("");
                } else if (getIndex() % 2 == 0) {
                    setStyle("-fx-background-color: #f8faf9;");
                } else {
                    setStyle("-fx-background-color: #eef4ef;");
                }
            }
        });

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
                String.format("[%.1f - %.1f]", (double) data.getValue().getThresholdMin(), (double) data.getValue().getThresholdMax())));
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
        setActiveNavigation("reports");
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
        showInContentArea(container);
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
        dialog.getDialogPane().setPrefSize(500, 400);

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
        dialog.getDialogPane().setPrefSize(700, 600);

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

        TableColumn<Animal, String> productionCol = new TableColumn<>("Production");
        productionCol.setCellValueFactory(data -> {
            Animal animal = data.getValue();
            if (animal instanceof Producing producing) {
                String unit = producing.getProductionRecord().getUnit();
                return new SimpleStringProperty(String.format("%.2f %s", producing.getProduction(), unit));
            }
            return new SimpleStringProperty("—");
        });
        productionCol.setPrefWidth(110);

        TableColumn<Animal, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(220);
        actionsCol.setCellFactory(col -> new TableCell<Animal, Void>() {
            private final Button removeBtn = new Button("Remove");
            private final Button eventsBtn = new Button("Events");
            private final Button prodBtn = new Button("Production");
            private final HBox pane = new HBox(5, prodBtn, eventsBtn, removeBtn);

            {
                removeBtn.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                eventsBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
                prodBtn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");

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

                prodBtn.setOnAction(e -> {
                    Animal animal = getTableView().getItems().get(getIndex());
                    if (animal instanceof Producing producing) {
                        showProductionRecordDialog(producing, animal.getId() + " @ " + zone.getCode(), () ->
                                table.setItems(FXCollections.observableArrayList(zone.getAnimals())));
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(idCol, speciesCol, ageCol, weightCol, healthCol, productionCol, actionsCol);

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
        dialog.getDialogPane().setPrefSize(500, 400);

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
        showInContentArea(container);
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
                    Zone zone = findZoneByCode(sensor.getZoneCode());
                    if (zone != null && zone.getStatus() == ZoneStatus.SUSPENDED
                            && statusBox.getValue() == SensorStatus.ACTIVE) {
                        showWarningDialog("Zone suspended",
                                "Sensors stay suspended while their zone is suspended.");
                        sensor.setStatus(SensorStatus.SUSPENDED);
                    } else {
                        sensor.setStatus(statusBox.getValue());
                    }
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
        dialog.getDialogPane().setPrefSize(800, 500);

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
                    if (!canOperateSensor(sensor)) {
                        showWarningDialog("Sensor unavailable",
                                "Cannot add readings or alerts for a suspended sensor or zone.");
                        return;
                    }
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
                    Sensor sensor = findSensorByCode(sensorField.getText().trim());
                    if (sensor != null && !canOperateSensor(sensor)) {
                        showWarningDialog("Sensor unavailable",
                                "Cannot generate alerts for a suspended sensor or zone.");
                        return;
                    }
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