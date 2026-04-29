import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;

import model.zones.*;
import model.sensors.*;
import model.crops.*;
import model.animals.*;
import model.entities.*;
import model.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main extends Application {
    // Data storage - using fully qualified name for model Alert
    private static final List<CropZone> cropZones = new ArrayList<>();
    private static final List<LivestockZone> livestockZones = new ArrayList<>();
    private static final List<AquacultureZone> aquacultureZones = new ArrayList<>();
    private static final List<model.entities.Alert> activeAlerts = new ArrayList<>();
    private static final List<model.entities.Alert> alertHistory = new ArrayList<>();
    private static int alertCounter = 100;
    private static int readingCounter = 1;

    private BorderPane mainLayout;
    private VBox sidebar;
    private StackPane contentArea;

    private final String PRIMARY_COLOR = "#2e7d32";
    private final String SECONDARY_COLOR = "#f1f8e9";
    private final String SIDEBAR_COLOR = "#263238";

    @Override
    public void start(Stage primaryStage) {
        loadSampleData();

        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: " + SECONDARY_COLOR + ";");

        sidebar = createSidebar();
        mainLayout.setLeft(sidebar);

        contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));
        mainLayout.setCenter(contentArea);

        showDashboard();

        Scene scene = new Scene(mainLayout, 1200, 800);
        primaryStage.setTitle("Smart Farming System - Management Console");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createSidebar() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20, 10, 20, 10));
        box.setPrefWidth(220);
        box.setStyle("-fx-background-color: " + SIDEBAR_COLOR + ";");

        Label title = new Label("SMART FARM");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);
        title.setPadding(new Insets(0, 0, 20, 0));
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        box.getChildren().add(title);

        box.getChildren().addAll(
                createSidebarButton("📊 Dashboard", this::showDashboard),
                createSidebarButton("🌾 Crop Zones", this::showCropZones),
                createSidebarButton("🐄 Livestock Zones", this::showLivestockZones),
                createSidebarButton("🐟 Aquaculture", this::showAquacultureZones),
                createSidebarButton("📡 Sensors", this::showSensors),
                createSidebarButton("⚠️ Alerts Center", this::showAlerts),
                createSidebarButton("📈 Reports", this::showReports)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        box.getChildren().add(spacer);

        Button exitBtn = createSidebarButton("🚪 Exit System", () -> System.exit(0));
        exitBtn.setStyle("-fx-background-color: #c62828; -fx-text-fill: white; -fx-alignment: CENTER-LEFT; -fx-cursor: hand;");
        box.getChildren().add(exitBtn);

        return box;
    }

    private Button createSidebarButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 15, 12, 15));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cfd8dc; -fx-font-size: 14px; -fx-cursor: hand;");

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #37474f; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cfd8dc; -fx-font-size: 14px; -fx-cursor: hand;"));

        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void setPageTitle(String title, VBox container) {
        Label label = new Label(title);
        label.setFont(Font.font("System", FontWeight.BOLD, 24));
        label.setTextFill(Color.web(PRIMARY_COLOR));
        label.setPadding(new Insets(0, 0, 20, 0));
        container.getChildren().add(label);
    }

    private void showDashboard() {
        VBox container = new VBox(20);
        setPageTitle("Farm Dashboard Overview", container);

        FlowPane statsCards = new FlowPane(20, 20);
        statsCards.getChildren().addAll(
                createStatCard("Total Zones", String.valueOf(cropZones.size() + livestockZones.size() + aquacultureZones.size()), "#1976d2"),
                createStatCard("Active Alerts", String.valueOf(activeAlerts.size()), "#d32f2f"),
                createStatCard("Total Crops", String.valueOf(cropZones.stream().mapToInt(Zone::getEntityCount).sum()), "#388e3c"),
                createStatCard("Total Animals", String.valueOf(livestockZones.stream().mapToInt(Zone::getEntityCount).sum()), "#fbc02d"),
                createStatCard("Total Sensors", String.valueOf(getAllSensors().size()), "#ff9800"),
                createStatCard("Aquaculture", String.valueOf(aquacultureZones.stream().mapToInt(AquacultureZone::getAnimalCount).sum()), "#009688")
        );

        container.getChildren().add(statsCards);

        Label alertsLabel = new Label("Recent System Alerts");
        alertsLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        container.getChildren().add(alertsLabel);

        TableView<model.entities.Alert> alertTable = createAlertTableView(activeAlerts);
        alertTable.setPrefHeight(250);
        container.getChildren().add(alertTable);

        Button manualAlertBtn = new Button("⚠️ Generate Manual Alert");
        manualAlertBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10; -fx-cursor: hand;");
        manualAlertBtn.setOnAction(e -> showManualAlertDialog());
        container.getChildren().add(manualAlertBtn);

        contentArea.getChildren().setAll(container);
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(20));
        card.setPrefSize(180, 100);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        Label titleLbl = new Label(title);
        titleLbl.setTextFill(Color.GRAY);

        Label valueLbl = new Label(value);
        valueLbl.setFont(Font.font("System", FontWeight.BOLD, 28));
        valueLbl.setTextFill(Color.web(color));

        card.getChildren().addAll(titleLbl, valueLbl);
        return card;
    }

    private void showManualAlertDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Generate Manual Alert");
        dialog.setHeaderText("Create a new system alert");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField sensorField = new TextField();
        sensorField.setPromptText("Sensor ID (e.g., SENS101)");

        TextField valueField = new TextField();
        valueField.setPromptText("Reading value");

        TextField minField = new TextField();
        minField.setPromptText("Min threshold");

        TextField maxField = new TextField();
        maxField.setPromptText("Max threshold");

        ComboBox<SeverityLevel> severityBox = new ComboBox<>();
        severityBox.getItems().addAll(SeverityLevel.WARNING, SeverityLevel.CRITICAL);
        severityBox.setValue(SeverityLevel.WARNING);

        grid.add(new Label("Sensor ID:"), 0, 0);
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
                    String sensorId = sensorField.getText();
                    double value = Double.parseDouble(valueField.getText());
                    double min = Double.parseDouble(minField.getText());
                    double max = Double.parseDouble(maxField.getText());
                    SeverityLevel severity = severityBox.getValue();

                    model.entities.Alert alert = new model.entities.Alert(
                            "ALT" + String.format("%03d", alertCounter++),
                            sensorId, value, min, max, severity, LocalDateTime.now()
                    );
                    activeAlerts.add(alert);
                    alertHistory.add(alert);

                    showInfoDialog("Alert Generated", "Manual alert created with ID: " + alert.getId());
                    showDashboard();
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number format");
                }
            }
        });
    }

    private void showSensors() {
        VBox container = new VBox(15);
        setPageTitle("Sensor Management", container);

        Label listLabel = new Label("All Sensors");
        listLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        container.getChildren().add(listLabel);

        TableView<SensorWrapper> table = new TableView<>();
        ObservableList<SensorWrapper> sensorList = FXCollections.observableArrayList();

        for (Sensor s : getAllSensors()) {
            sensorList.add(new SensorWrapper(s));
        }
        table.setItems(sensorList);

        TableColumn<SensorWrapper, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(100);

        TableColumn<SensorWrapper, String> zoneCol = new TableColumn<>("Zone");
        zoneCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getZoneCode()));
        zoneCol.setPrefWidth(100);

        TableColumn<SensorWrapper, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        typeCol.setPrefWidth(120);

        TableColumn<SensorWrapper, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setPrefWidth(100);

        TableColumn<SensorWrapper, String> thresholdCol = new TableColumn<>("Threshold");
        thresholdCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getThreshold()));
        thresholdCol.setPrefWidth(150);

        TableColumn<SensorWrapper, String> lastReadingCol = new TableColumn<>("Last Reading");
        lastReadingCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLastReading()));
        lastReadingCol.setPrefWidth(150);

        table.getColumns().addAll(codeCol, zoneCol, typeCol, statusCol, thresholdCol, lastReadingCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(400);
        container.getChildren().add(table);

        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button addReadingBtn = new Button("📊 Add Reading");
        addReadingBtn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-cursor: hand;");
        addReadingBtn.setOnAction(e -> showAddReadingDialog());

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #607d8b; -fx-text-fill: white; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> showSensors());

        actions.getChildren().addAll(addReadingBtn, refreshBtn);
        container.getChildren().add(actions);

        contentArea.getChildren().setAll(container);
    }

    private void showAddReadingDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Sensor Reading");
        dialog.setHeaderText("Record a new sensor reading");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> sensorBox = new ComboBox<>();
        for (Sensor s : getAllSensors()) {
            sensorBox.getItems().add(s.getCode() + " - " + s.getClass().getSimpleName());
        }

        TextField valueField = new TextField();
        valueField.setPromptText("Reading value");

        grid.add(new Label("Select Sensor:"), 0, 0);
        grid.add(sensorBox, 1, 0);
        grid.add(new Label("Value:"), 0, 1);
        grid.add(valueField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && sensorBox.getValue() != null) {
                try {
                    String selected = sensorBox.getValue();
                    String sensorCode = selected.split(" - ")[0];
                    double value = Double.parseDouble(valueField.getText());

                    Sensor sensor = findSensorByCode(sensorCode);
                    if (sensor != null) {
                        Reading reading = new Reading(sensorCode, value, sensor.getUnit(), LocalDateTime.now());
                        sensor.addReading(reading);

                        if (value < sensor.getThresholdMin() || value > sensor.getThresholdMax()) {
                            SeverityLevel severity = SeverityLevel.WARNING;
                            if (value < sensor.getThresholdMin() * 0.7 || value > sensor.getThresholdMax() * 1.3) {
                                severity = SeverityLevel.CRITICAL;
                            }
                            model.entities.Alert alert = new model.entities.Alert(
                                    "ALT" + String.format("%03d", alertCounter++),
                                    sensorCode, value, sensor.getThresholdMin(), sensor.getThresholdMax(),
                                    severity, LocalDateTime.now()
                            );
                            activeAlerts.add(alert);
                            alertHistory.add(alert);
                            showWarningDialog("Alert Triggered", "Reading out of range! Severity: " + severity);
                        }

                        showInfoDialog("Success", "Reading recorded: " + value + " " + sensor.getUnit());
                        showSensors();
                    }
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number format");
                }
            }
        });
    }

    private void showCropZones() {
        VBox container = new VBox(15);
        setPageTitle("Crop Zone Management", container);

        TableView<CropZone> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(cropZones));

        TableColumn<CropZone, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(100);

        TableColumn<CropZone, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<CropZone, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        statusCol.setPrefWidth(100);

        TableColumn<CropZone, String> countCol = new TableColumn<>("Crops");
        countCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getEntityCount())));
        countCol.setPrefWidth(80);

        table.getColumns().addAll(codeCol, nameCol, statusCol, countCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(400);

        container.getChildren().add(table);
        contentArea.getChildren().setAll(container);
    }

    private void showLivestockZones() {
        VBox container = new VBox(15);
        setPageTitle("Livestock Management", container);

        TableView<LivestockZone> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(livestockZones));

        TableColumn<LivestockZone, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(100);

        TableColumn<LivestockZone, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<LivestockZone, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        statusCol.setPrefWidth(100);

        TableColumn<LivestockZone, String> animalsCol = new TableColumn<>("Animals");
        animalsCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getEntityCount())));
        animalsCol.setPrefWidth(80);

        table.getColumns().addAll(codeCol, nameCol, statusCol, animalsCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(400);

        container.getChildren().add(table);
        contentArea.getChildren().setAll(container);
    }

    private void showAquacultureZones() {
        VBox container = new VBox(15);
        setPageTitle("Aquaculture Management", container);

        TableView<AquacultureZone> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(aquacultureZones));

        TableColumn<AquacultureZone, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(100);

        TableColumn<AquacultureZone, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<AquacultureZone, String> countCol = new TableColumn<>("Animal Count");
        countCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getAnimalCount())));
        countCol.setPrefWidth(100);

        table.getColumns().addAll(codeCol, nameCol, countCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(400);

        container.getChildren().add(table);
        contentArea.getChildren().setAll(container);
    }

    private void showAlerts() {
        VBox container = new VBox(15);
        setPageTitle("Alerts Center", container);

        TabPane tabs = new TabPane();

        Tab activeTab = new Tab("Active Alerts (" + activeAlerts.size() + ")");
        VBox activeContent = new VBox(10);
        TableView<model.entities.Alert> activeTable = createAlertTableView(activeAlerts);
        activeTable.setPrefHeight(500);

        HBox activeActions = new HBox(10);
        Button acknowledgeBtn = new Button("✅ Acknowledge Selected");
        acknowledgeBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
        acknowledgeBtn.setOnAction(e -> {
            model.entities.Alert selected = activeTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.acknowledge();
                showInfoDialog("Acknowledged", "Alert " + selected.getId() + " acknowledged");
                showAlerts();
            }
        });

        Button dismissBtn = new Button("❌ Dismiss Selected");
        dismissBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        dismissBtn.setOnAction(e -> {
            model.entities.Alert selected = activeTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.dismiss();
                activeAlerts.remove(selected);
                showInfoDialog("Dismissed", "Alert " + selected.getId() + " dismissed");
                showAlerts();
            }
        });

        activeActions.getChildren().addAll(acknowledgeBtn, dismissBtn);
        activeContent.getChildren().addAll(activeTable, activeActions);
        activeTab.setContent(activeContent);

        Tab historyTab = new Tab("Alert History (" + alertHistory.size() + ")");
        TableView<model.entities.Alert> historyTable = createAlertTableView(alertHistory);
        historyTable.setPrefHeight(500);
        historyTab.setContent(historyTable);

        activeTab.setClosable(false);
        historyTab.setClosable(false);
        tabs.getTabs().addAll(activeTab, historyTab);

        container.getChildren().add(tabs);
        contentArea.getChildren().setAll(container);
    }

    private TableView<model.entities.Alert> createAlertTableView(List<model.entities.Alert> dataList) {
        TableView<model.entities.Alert> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(dataList));

        TableColumn<model.entities.Alert, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        idCol.setPrefWidth(80);

        TableColumn<model.entities.Alert, String> sensorCol = new TableColumn<>("Sensor");
        sensorCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSensorCode()));
        sensorCol.setPrefWidth(100);

        TableColumn<model.entities.Alert, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.1f", data.getValue().getReadingValue())));
        valueCol.setPrefWidth(80);

        TableColumn<model.entities.Alert, String> thresholdCol = new TableColumn<>("Threshold");
        thresholdCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("[%.1f - %.1f]", data.getValue().getThresholdMin(), data.getValue().getThresholdMax())));
        thresholdCol.setPrefWidth(150);

        TableColumn<model.entities.Alert, String> severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeverity().toString()));
        severityCol.setPrefWidth(100);

        TableColumn<model.entities.Alert, String> statusCol = new TableColumn<>("Acknowledged");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isAcknowledged() ? "Yes" : "No"));
        statusCol.setPrefWidth(100);

        TableColumn<model.entities.Alert, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        timeCol.setPrefWidth(100);

        table.getColumns().addAll(idCol, sensorCol, valueCol, thresholdCol, severityCol, statusCol, timeCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    private void showReports() {
        VBox container = new VBox(20);
        setPageTitle("System Reports", container);

        // Crop Report
        TitledPane cropReport = new TitledPane();
        cropReport.setText("🌾 Crop Production Report");
        VBox cropContent = new VBox(10);
        cropContent.setPadding(new Insets(10));

        if (cropZones.isEmpty()) {
            cropContent.getChildren().add(new Label("No crop zones available."));
        } else {
            for (CropZone zone : cropZones) {
                Label zoneLabel = new Label(zone.getName() + " (" + zone.getCode() + ")");
                zoneLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                cropContent.getChildren().add(zoneLabel);

                for (Crop c : zone.getCrops()) {
                    String info = "  • " + c.getName() + " | Stage: " + c.getGrowthStage() +
                            " | Harvest: " + c.getExpectedHarvestDate();
                    cropContent.getChildren().add(new Label(info));
                }
            }
        }
        cropReport.setContent(cropContent);
        cropReport.setExpanded(true);

        // Livestock Report
        TitledPane livestockReport = new TitledPane();
        livestockReport.setText("🐄 Livestock Production Report");
        VBox livestockContent = new VBox(10);
        livestockContent.setPadding(new Insets(10));

        double totalMilk = 0;
        int totalEggs = 0;

        for (LivestockZone zone : livestockZones) {
            Label zoneLabel = new Label(zone.getName() + " (" + zone.getCode() + ")");
            zoneLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            livestockContent.getChildren().add(zoneLabel);

            for (Animal a : zone.getAnimals()) {
                String info = "  • " + a.getId() + " - " + a.getSpecies() + " | Health: " + a.getHealthStatus();
                livestockContent.getChildren().add(new Label(info));

                if (a instanceof Ruminant) {
                    double milk = ((Ruminant) a).getMilkYield();
                    totalMilk += milk;
                    livestockContent.getChildren().add(new Label("     🥛 Milk: " + milk + " L"));
                }
                if (a instanceof Poultry) {
                    int eggs = ((Poultry) a).getEggCount();
                    totalEggs += eggs;
                    livestockContent.getChildren().add(new Label("     🥚 Eggs: " + eggs));
                }
            }
        }

        livestockContent.getChildren().add(new Separator());
        livestockContent.getChildren().add(new Label("📊 TOTAL MILK: " + totalMilk + " L"));
        livestockContent.getChildren().add(new Label("📊 TOTAL EGGS: " + totalEggs));
        livestockReport.setContent(livestockContent);
        livestockReport.setExpanded(true);

        // Sensor Report
        TitledPane sensorReport = new TitledPane();
        sensorReport.setText("📡 Sensor Health Report");
        VBox sensorContent = new VBox(10);
        sensorContent.setPadding(new Insets(10));

        int totalSensors = 0;
        int activeCount = 0;
        int faultyCount = 0;
        int suspendedCount = 0;

        for (Sensor s : getAllSensors()) {
            totalSensors++;
            switch (s.getStatus()) {
                case ACTIVE: activeCount++; break;
                case FAULTY: faultyCount++; break;
                case SUSPENDED: suspendedCount++; break;
            }
            String lastReading = s.getReadings().isEmpty() ? "No readings" :
                    s.getReadings().get(s.getReadings().size() - 1).getValue() + " " + s.getUnit();
            sensorContent.getChildren().add(new Label("  • " + s.getCode() + " | Status: " + s.getStatus() + " | Last: " + lastReading));
        }

        sensorContent.getChildren().add(new Separator());
        sensorContent.getChildren().add(new Label("📊 Total Sensors: " + totalSensors));
        sensorContent.getChildren().add(new Label("   ✅ Active: " + activeCount));
        sensorContent.getChildren().add(new Label("   ⚠️ Faulty: " + faultyCount));
        sensorContent.getChildren().add(new Label("   ⏸️ Suspended: " + suspendedCount));
        sensorReport.setContent(sensorContent);
        sensorReport.setExpanded(true);

        // Alert Report
        TitledPane alertReport = new TitledPane();
        alertReport.setText("⚠️ Alert System Report");
        VBox alertContent = new VBox(10);
        alertContent.setPadding(new Insets(10));

        int criticalCount = 0;
        int warningCount = 0;

        for (model.entities.Alert a : alertHistory) {
            if (a.getSeverity() == SeverityLevel.CRITICAL) criticalCount++;
            else warningCount++;
        }

        alertContent.getChildren().add(new Label("📊 Total Alerts: " + alertHistory.size()));
        alertContent.getChildren().add(new Label("   🔴 Critical: " + criticalCount));
        alertContent.getChildren().add(new Label("   🟡 Warning: " + warningCount));
        alertContent.getChildren().add(new Label("   ✅ Acknowledged: " + alertHistory.stream().filter(a -> a.isAcknowledged()).count()));
        alertContent.getChildren().add(new Label("   ❌ Dismissed: " + alertHistory.stream().filter(a -> a.isDismissed()).count()));
        alertContent.getChildren().add(new Label("   ⚠️ Active: " + activeAlerts.size()));
        alertReport.setContent(alertContent);
        alertReport.setExpanded(true);

        ScrollPane scrollPane = new ScrollPane();
        VBox reportsBox = new VBox(15);
        reportsBox.getChildren().addAll(cropReport, livestockReport, sensorReport, alertReport);
        scrollPane.setContent(reportsBox);
        scrollPane.setFitToWidth(true);

        container.getChildren().add(scrollPane);
        contentArea.getChildren().setAll(container);
    }

    // Helper methods for showing dialogs (using fully qualified name for JavaFX Alert)
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

    private void loadSampleData() {
        // Crop Zone
        CropZone cropZone = new CropZone("CZ001", "North Valley Farm");
        cropZone.addCrop(new Crop("Winter Wheat", CropFamily.CEREALS,
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 7, 15), 6.0, 7.5, 20.0, 30.0));
        cropZone.addCrop(new Crop("Cherry Tomato", CropFamily.VEGETABLES,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30), 6.2, 6.8, 25.0, 35.0));
        cropZones.add(cropZone);

        // Livestock Zone
        LivestockZone livestockZone = new LivestockZone("LZ001", "East Pasture");
        livestockZone.setFeedingProgram(new FeedingProgram("Organic Hay Mix", 5.5, 3));
        Ruminant cow = new Ruminant("R1001", "Holstein Friesian", 4, 650.0);
        cow.addMilkYield(125.5);
        livestockZone.addAnimal(cow);
        livestockZone.addAnimal(new Poultry("P1001", "Rhode Island Red", 1, 2.5));
        livestockZones.add(livestockZone);

        // Aquaculture Zone
        AquacultureZone aquaZone = new AquacultureZone("AZ001", "West Pond");
        aquaZone.addSpecies("Nile Tilapia");
        aquaZone.addSpecies("African Catfish");
        aquaZone.setAnimalCount(1250);
        aquaZone.setFeedingProgram(new FeedingProgram("Protein Pellets", 3.5, 4));
        aquacultureZones.add(aquaZone);

        // Sensors
        EnvironmentSensor sensor1 = new EnvironmentSensor("SENS101", "CZ001", 10.0, 35.0, "temperature");
        EnvironmentSensor sensor2 = new EnvironmentSensor("SENS102", "CZ001", 30.0, 80.0, "humidity");
        cropZone.addSensor(sensor1);
        cropZone.addSensor(sensor2);
        sensor1.addReading(new Reading("SENS101", 23.5, "°C", LocalDateTime.now()));
        sensor2.addReading(new Reading("SENS102", 65.0, "%", LocalDateTime.now()));

        // Sample Alert
        model.entities.Alert alert = new model.entities.Alert(
                "ALT001", "SENS101", 36.5, 10.0, 35.0, SeverityLevel.CRITICAL, LocalDateTime.now()
        );
        activeAlerts.add(alert);
        alertHistory.add(alert);
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Wrapper class for Sensor table
    public static class SensorWrapper {
        private Sensor sensor;

        public SensorWrapper(Sensor s) { this.sensor = s; }

        public String getCode() { return sensor.getCode(); }
        public String getZoneCode() { return sensor.getZoneCode(); }
        public String getType() { return sensor.getClass().getSimpleName(); }
        public String getStatus() { return sensor.getStatus().toString(); }
        public String getThreshold() { return String.format("[%.1f - %.1f]", sensor.getThresholdMin(), sensor.getThresholdMax()); }
        public String getLastReading() {
            if (sensor.getReadings().isEmpty()) return "No readings";
            Reading last = sensor.getReadings().get(sensor.getReadings().size() - 1);
            return last.getValue() + " " + last.getUnit() + " at " + last.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
    }
}