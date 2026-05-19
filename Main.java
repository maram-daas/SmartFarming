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

import model.entities.Alert;
import model.utils.DataManager;
import model.zones.*;
import model.sensors.*;
import model.crops.*;
import model.animals.*;
import model.entities.*;
import model.enums.*;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private BorderPane mainLayout;
    private VBox sidebar;
    private StackPane contentArea;
    private Label currentPageTitle;

    private final String PRIMARY_COLOR = "#2e7d32";
    private final String SECONDARY_COLOR = "#f5f5f5";
    private final String SIDEBAR_COLOR = "#2e7d32";
    private final String DANGER_COLOR = "#c62828";

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
        // Load data using the new DataManager facade
        DataManager.loadAllData(cropZones, livestockZones, aquacultureZones, alertHistory);

        // Update counters based on loaded data
        updateCountersFromData();

        System.out.println("Data loaded successfully");
        System.out.println("Crop Zones: " + cropZones.size());
        System.out.println("Livestock Zones: " + livestockZones.size());
        System.out.println("Aquaculture Zones: " + aquacultureZones.size());
        System.out.println("Active Alerts: " + activeAlerts.size());
        System.out.println("Alert History: " + alertHistory.size());
    }

    private void updateCountersFromData() {
        // Update zoneCounter to be greater than any existing zone code
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

        // Update sensorCounter
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

        // Update alertCounter
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
    }

    private void saveAllData() {
        DataManager.saveAllData(cropZones, livestockZones, aquacultureZones, alertHistory);
    }

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
            DataManager.exportReportToCSV(cropZones, livestockZones, aquacultureZones, alertHistory, file.getAbsolutePath());
            showInfoDialog("Export Complete", "Report saved to: " + file.getAbsolutePath());
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

        Scene scene = new Scene(mainLayout, 1200, 800);
        primaryStage.setMaximized(false);
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
        quitBtn.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white;");
        box.getChildren().add(quitBtn);

        return box;
    }

    private Button createNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 15, 10, 15));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e0e0e0; -fx-font-size: 14px;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #1b5e20; -fx-text-fill: white;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e0e0e0;"));
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
        statsGrid.add(createStatCard("Active Alerts", String.valueOf(activeAlerts.size()), "#d32f2f"), 1, 0);
        statsGrid.add(createStatCard("Total Crops", String.valueOf(totalCrops), "#388e3c"), 2, 0);
        statsGrid.add(createStatCard("Total Animals", String.valueOf(totalAnimals), "#fbc02d"), 3, 0);
        statsGrid.add(createStatCard("Total Sensors", String.valueOf(getAllSensors().size()), "#ff9800"), 0, 1);
        statsGrid.add(createStatCard("Critical Alerts", String.valueOf(activeAlerts.stream().filter(a -> a.getSeverity() == SeverityLevel.CRITICAL).count()), "#f44336"), 1, 1);

        container.getChildren().add(statsGrid);

        Label alertsLabel = new Label("Recent Alerts");
        alertsLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        container.getChildren().add(alertsLabel);

        TableView<model.entities.Alert> alertTable = createAlertTableView(activeAlerts);
        alertTable.setPrefHeight(250);
        container.getChildren().add(alertTable);

        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 0, 0));
        actions.getChildren().addAll(
                createActionButton("Generate Alert", "#ff9800", this::showManualAlertDialog),
                createActionButton("Export Report", "#4caf50", this::exportReport),
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
        table.setPrefHeight(500);

        TableColumn<CropZone, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(100);

        TableColumn<CropZone, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<CropZone, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        statusCol.setPrefWidth(80);

        TableColumn<CropZone, Integer> countCol = new TableColumn<>("Crops");
        countCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getEntityCount()).asObject());
        countCol.setPrefWidth(80);

        TableColumn<CropZone, String> allowedCol = new TableColumn<>("Allowed Type");
        allowedCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAllowedCropFamily() != null ? data.getValue().getAllowedCropFamily().toString() : "Any"));
        allowedCol.setPrefWidth(100);

        TableColumn<CropZone, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(200);
        actionsCol.setCellFactory(col -> new TableCell<CropZone, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");

                editBtn.setOnAction(e -> {
                    CropZone zone = getTableView().getItems().get(getIndex());
                    showEditCropZoneDialog(zone);
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

        Button addBtn = createActionButton("Add Crop Zone", PRIMARY_COLOR, this::showCreateCropZoneDialog);
        wrapper.getChildren().add(addBtn);

        return wrapper;
    }

    private VBox createLivestockZoneTable() {
        VBox wrapper = new VBox(10);

        TableView<LivestockZone> table = new TableView<>();
        table.setItems(livestockZones);
        table.setPrefHeight(500);

        TableColumn<LivestockZone, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(100);

        TableColumn<LivestockZone, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<LivestockZone, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        statusCol.setPrefWidth(80);

        TableColumn<LivestockZone, Integer> countCol = new TableColumn<>("Animals");
        countCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getEntityCount()).asObject());
        countCol.setPrefWidth(80);

        TableColumn<LivestockZone, String> allowedCol = new TableColumn<>("Allowed Type");
        allowedCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAllowedAnimalType() != null ? data.getValue().getAllowedAnimalType().toString() : "Any"));
        allowedCol.setPrefWidth(100);

        TableColumn<LivestockZone, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(250);
        actionsCol.setCellFactory(col -> new TableCell<LivestockZone, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Button animalsBtn = new Button("Animals");
            private final HBox pane = new HBox(5, editBtn, deleteBtn, animalsBtn);

            {
                editBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                animalsBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-cursor: hand;");

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
        table.setPrefHeight(500);

        TableColumn<AquacultureZone, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(100);

        TableColumn<AquacultureZone, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<AquacultureZone, Integer> countCol = new TableColumn<>("Fish");
        countCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getAnimalCount()).asObject());
        countCol.setPrefWidth(80);

        TableColumn<AquacultureZone, String> speciesCol = new TableColumn<>("Species");
        speciesCol.setCellValueFactory(data -> new SimpleStringProperty(String.join(", ", data.getValue().getSpecies())));
        speciesCol.setPrefWidth(200);

        TableColumn<AquacultureZone, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(200);
        actionsCol.setCellFactory(col -> new TableCell<AquacultureZone, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Button detailsBtn = new Button("Details");
            private final HBox pane = new HBox(5, editBtn, deleteBtn, detailsBtn);

            {
                editBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                detailsBtn.setStyle("-fx-background-color: #009688; -fx-text-fill: white; -fx-cursor: hand;");

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
                editBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                historyBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-cursor: hand;");

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
        setPageTitle("Alerts");
        container.getChildren().add(currentPageTitle);

        TabPane tabs = new TabPane();

        Tab activeTab = new Tab("Active (" + activeAlerts.size() + ")");
        VBox activeContent = new VBox(10);
        TableView<model.entities.Alert> activeTable = createAlertTableView(activeAlerts);
        activeTable.setPrefHeight(400);

        HBox activeActions = new HBox(10);
        activeActions.getChildren().addAll(
                createActionButton("Acknowledge", "#4caf50", () -> {
                    model.entities.Alert selected = activeTable.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        selected.acknowledge();
                        saveAllData();
                        showAlerts();
                        showInfoDialog("Acknowledged", "Alert " + selected.getId() + " acknowledged");
                    }
                }),
                createActionButton("Dismiss", "#f44336", () -> {
                    model.entities.Alert selected = activeTable.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        selected.dismiss();
                        activeAlerts.remove(selected);
                        saveAllData();
                        showAlerts();
                        showInfoDialog("Dismissed", "Alert " + selected.getId() + " dismissed");
                    }
                })
        );

        activeContent.getChildren().addAll(activeTable, activeActions);
        activeTab.setContent(activeContent);

        Tab historyTab = new Tab("History (" + alertHistory.size() + ")");
        TableView<model.entities.Alert> historyTable = createAlertTableView(alertHistory);
        historyTable.setPrefHeight(400);

        HBox historyActions = new HBox(10);
        historyActions.getChildren().add(
                createActionButton("Clear History", "#f44336", () -> {
                    showDeleteConfirmation("Alert History", "all alerts", () -> {
                        alertHistory.clear();
                        saveAllData();
                        showAlerts();
                    });
                })
        );

        VBox historyContent = new VBox(10);
        historyContent.getChildren().addAll(historyTable, historyActions);
        historyTab.setContent(historyContent);

        tabs.getTabs().addAll(activeTab, historyTab);
        container.getChildren().add(tabs);

        contentArea.getChildren().setAll(container);
    }

    private TableView<model.entities.Alert> createAlertTableView(ObservableList<model.entities.Alert> dataList) {
        TableView<model.entities.Alert> table = new TableView<>();
        table.setItems(dataList);

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

        TableColumn<model.entities.Alert, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        timeCol.setPrefWidth(170);

        table.getColumns().addAll(idCol, sensorCol, valueCol, thresholdCol, severityCol, timeCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    private void showReports() {
        VBox container = new VBox(15);
        setPageTitle("Reports");
        container.getChildren().add(currentPageTitle);

        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setPrefHeight(500);

        StringBuilder report = new StringBuilder();
        report.append("=== FARM REPORT ===\n\n");
        report.append("Generated: ").append(LocalDateTime.now()).append("\n\n");

        report.append("--- CROP ZONES ---\n");
        for (CropZone zone : cropZones) {
            report.append("Zone: ").append(zone.getName()).append(" (").append(zone.getCode()).append(")\n");
            report.append("  Crops: ").append(zone.getEntityCount()).append("\n");
            report.append("  Status: ").append(zone.getStatus()).append("\n");
        }

        report.append("\n--- LIVESTOCK ZONES ---\n");
        for (LivestockZone zone : livestockZones) {
            report.append("Zone: ").append(zone.getName()).append(" (").append(zone.getCode()).append(")\n");
            report.append("  Animals: ").append(zone.getEntityCount()).append("\n");
            report.append("  Status: ").append(zone.getStatus()).append("\n");
        }

        report.append("\n--- AQUACULTURE ZONES ---\n");
        for (AquacultureZone zone : aquacultureZones) {
            report.append("Zone: ").append(zone.getName()).append(" (").append(zone.getCode()).append(")\n");
            report.append("  Fish: ").append(zone.getAnimalCount()).append("\n");
            report.append("  Species: ").append(String.join(", ", zone.getSpecies())).append("\n");
        }

        report.append("\n--- SENSORS ---\n");
        for (Sensor s : getAllSensors()) {
            report.append("  ").append(s.getCode()).append(" (").append(s.getClass().getSimpleName()).append(") - ")
                    .append(s.getStatus()).append("\n");
        }

        report.append("\n--- ALERTS ---\n");
        report.append("Active Alerts: ").append(activeAlerts.size()).append("\n");
        report.append("Total History: ").append(alertHistory.size()).append("\n");

        reportArea.setText(report.toString());
        container.getChildren().add(reportArea);

        container.getChildren().add(createActionButton("Export CSV", "#4caf50", this::exportReport));

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

        ComboBox<CropFamily> familyBox = new ComboBox<>();
        familyBox.getItems().addAll(CropFamily.values());
        familyBox.setPromptText("Allowed crop family (optional)");

        grid.add(new Label("Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Allowed Family:"), 0, 2);
        grid.add(familyBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && !nameField.getText().isEmpty()) {
                CropZone zone = new CropZone(codeField.getText(), nameField.getText());
                if (familyBox.getValue() != null) {
                    zone.setAllowedCropFamily(familyBox.getValue());
                }
                cropZones.add(zone);
                zoneCounter++;
                saveAllData();
                showInfoDialog("Success", "Crop zone created!");
                showZones("crop");
            }
        });
    }

    private void showEditCropZoneDialog(CropZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Crop Zone");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(zone.getName());
        ComboBox<ZoneStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(ZoneStatus.values()));
        statusBox.setValue(zone.getStatus());

        ComboBox<CropFamily> familyBox = new ComboBox<>(FXCollections.observableArrayList(CropFamily.values()));
        familyBox.setValue(zone.getAllowedCropFamily());
        familyBox.setPromptText("Allowed family (optional)");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);
        grid.add(new Label("Allowed Family:"), 0, 2);
        grid.add(familyBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                zone.setName(nameField.getText());
                zone.setAllowedCropFamily(familyBox.getValue());
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

        ComboBox<AnimalType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(AnimalType.values());
        typeBox.setPromptText("Allowed animal type (optional)");

        grid.add(new Label("Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Allowed Type:"), 0, 2);
        grid.add(typeBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && !nameField.getText().isEmpty()) {
                LivestockZone zone = new LivestockZone(codeField.getText(), nameField.getText());
                if (typeBox.getValue() != null) {
                    zone.setAllowedAnimalType(typeBox.getValue());
                }
                livestockZones.add(zone);
                zoneCounter++;
                saveAllData();
                showInfoDialog("Success", "Livestock zone created!");
                showZones("livestock");
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

        ComboBox<AnimalType> typeBox = new ComboBox<>(FXCollections.observableArrayList(AnimalType.values()));
        typeBox.setValue(zone.getAllowedAnimalType());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);
        grid.add(new Label("Allowed Type:"), 0, 2);
        grid.add(typeBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                zone.setName(nameField.getText());
                zone.setAllowedAnimalType(typeBox.getValue());
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
        grid.add(new Label("Name:"), 0, 1);
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
        grid.add(new Label("Fish Count:"), 0, 1);
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
                    zone.setFeedingProgram(new FeedingProgram(
                            feedTypeField.getText(),
                            Double.parseDouble(quantityField.getText()),
                            Integer.parseInt(mealsField.getText())
                    ));
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
                removeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                eventsBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand;");

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
        ComboBox<AnimalType> typeBox = new ComboBox<>(FXCollections.observableArrayList(AnimalType.values()));
        typeBox.setPromptText("Type");

        form.add(new Label("ID:"), 0, 0);
        form.add(idField, 1, 0);
        form.add(new Label("Species:"), 0, 1);
        form.add(speciesField, 1, 1);
        form.add(new Label("Age:"), 0, 2);
        form.add(ageField, 1, 2);
        form.add(new Label("Weight:"), 0, 3);
        form.add(weightField, 1, 3);
        form.add(new Label("Type:"), 0, 4);
        form.add(typeBox, 1, 4);

        Button addBtn = createActionButton("Add Animal", PRIMARY_COLOR, () -> {
            if (!idField.getText().isEmpty() && !speciesField.getText().isEmpty() && typeBox.getValue() != null) {
                try {
                    Animal animal;
                    int age = Integer.parseInt(ageField.getText());
                    double weight = Double.parseDouble(weightField.getText());

                    if (typeBox.getValue() == AnimalType.RUMINANT) {
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
            }
        });

        content.getChildren().addAll(new Label("Animals:"), table, new TitledPane("Add New Animal", form), addBtn);
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
        Button addBtn = createActionButton("Add", PRIMARY_COLOR, () -> {
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

        grid.add(createSensorCard("Environment", "#2196f3", () -> showCreateSensorDialog("EnvironmentSensor")), 0, 0);
        grid.add(createSensorCard("Soil", "#4caf50", () -> showCreateSensorDialog("SoilSensor")), 1, 0);
        grid.add(createSensorCard("Biometric", "#ff9800", () -> showCreateSensorDialog("BiometricSensor")), 2, 0);
        grid.add(createSensorCard("Water", "#009688", () -> showCreateSensorDialog("WaterSensor")), 0, 1);
        grid.add(createSensorCard("GPS", "#9c27b0", () -> showCreateSensorDialog("GPSSensor")), 1, 1);

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
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 16));
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
        TextField minField = new TextField();
        TextField maxField = new TextField();

        ComboBox<String> zoneBox = new ComboBox<>();
        for (CropZone z : cropZones) zoneBox.getItems().add(z.getCode());
        for (LivestockZone z : livestockZones) zoneBox.getItems().add(z.getCode());
        for (AquacultureZone z : aquacultureZones) zoneBox.getItems().add(z.getCode());

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
        grid.add(new Label("Min:"), 0, row);
        grid.add(minField, 1, row++);
        grid.add(new Label("Max:"), 0, row);
        grid.add(maxField, 1, row++);

        if (measureBox.getItems().size() > 0) {
            grid.add(new Label("Measurement:"), 0, row);
            grid.add(measureBox, 1, row++);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && zoneBox.getValue() != null) {
                try {
                    double min = Double.parseDouble(minField.getText());
                    double max = Double.parseDouble(maxField.getText());
                    String zoneCode = zoneBox.getValue();
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
        dialog.setTitle("Edit Sensor");

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

        content.getChildren().addAll(new Label("Readings for " + sensor.getCode()), table);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showAddReadingDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Reading");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<Sensor> sensorBox = new ComboBox<>(FXCollections.observableArrayList(getAllSensors()));
        sensorBox.setPromptText("Select sensor");

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
                        Alert alert = new Alert("ALT" + alertCounter++, sensor.getCode(), value,
                                sensor.getThresholdMin(), sensor.getThresholdMax(), severity, LocalDateTime.now());
                        activeAlerts.add(alert);
                        alertHistory.add(alert);
                        showWarningDialog("Alert", "Reading out of range! Severity: " + severity);
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
        dialog.setTitle("Generate Alert");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField sensorField = new TextField();
        sensorField.setPromptText("Sensor code");
        TextField valueField = new TextField();
        valueField.setPromptText("Value");
        ComboBox<SeverityLevel> severityBox = new ComboBox<>(FXCollections.observableArrayList(SeverityLevel.values()));

        grid.add(new Label("Sensor:"), 0, 0);
        grid.add(sensorField, 1, 0);
        grid.add(new Label("Value:"), 0, 1);
        grid.add(valueField, 1, 1);
        grid.add(new Label("Severity:"), 0, 2);
        grid.add(severityBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Alert alert = new Alert(
                            "ALT" + alertCounter++,
                            sensorField.getText(),
                            Double.parseDouble(valueField.getText()),
                            0, 100,
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

    private void loadSampleData() {
        // Only load if no data exists
        if (!cropZones.isEmpty() || !livestockZones.isEmpty() || !aquacultureZones.isEmpty()) {
            return;
        }

        // Create sample crop zone
        CropZone cropZone = new CropZone("CZ001", "North Valley Farm");
        cropZone.setAllowedCropFamily(CropFamily.CEREALS);
        cropZone.addCrop(new Crop("Winter Wheat", CropFamily.CEREALS,
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 7, 15), 6.0, 7.5, 20.0, 30.0));
        cropZones.add(cropZone);

        // Create sample livestock zone
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