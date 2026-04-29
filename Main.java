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
import java.util.stream.Collectors;

public class Main extends Application {
    // Data storage using fully qualified names for Alert to avoid ambiguity with JavaFX
    private static final List<CropZone> cropZones = new ArrayList<>();
    private static final List<LivestockZone> livestockZones = new ArrayList<>();
    private static final List<AquacultureZone> aquacultureZones = new ArrayList<>();
    private static final List<model.entities.Alert> activeAlerts = new ArrayList<>();
    private static final List<model.entities.Alert> alertHistory = new ArrayList<>();

    private BorderPane mainLayout;
    private VBox sidebar;
    private StackPane contentArea;

    // Styling constants
    private final String PRIMARY_COLOR = "#2e7d32"; // Dark Green
    private final String SECONDARY_COLOR = "#f1f8e9"; // Light Green Background
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

        Scene scene = new Scene(mainLayout, 1100, 750);
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
                createSidebarButton("Dashboard", this::showDashboard),
                createSidebarButton("Crop Zones", this::showCropZones),
                createSidebarButton("Livestock Zones", this::showLivestockZones),
                createSidebarButton("Aquaculture", this::showAquacultureZones),
                createSidebarButton("Alerts Center", this::showAlerts),
                createSidebarButton("Reports", this::showReports)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        box.getChildren().add(spacer);

        Button exitBtn = createSidebarButton("Exit System", () -> System.exit(0));
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
                createStatCard("Livestock Count", String.valueOf(livestockZones.stream().mapToInt(Zone::getEntityCount).sum()), "#fbc02d")
        );

        container.getChildren().add(statsCards);

        Label alertsLabel = new Label("Recent System Alerts");
        alertsLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        container.getChildren().add(alertsLabel);

        TableView<model.entities.Alert> alertTable = createAlertTableView(activeAlerts);
        alertTable.setPrefHeight(300);
        container.getChildren().add(alertTable);

        contentArea.getChildren().setAll(container);
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(20));
        card.setPrefSize(200, 100);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        Label titleLbl = new Label(title);
        titleLbl.setTextFill(Color.GRAY);

        Label valueLbl = new Label(value);
        valueLbl.setFont(Font.font("System", FontWeight.BOLD, 28));
        valueLbl.setTextFill(Color.web(color));

        card.getChildren().addAll(titleLbl, valueLbl);
        return card;
    }

    private void showCropZones() {
        VBox container = new VBox(15);
        setPageTitle("Crop Zone Management", container);

        HBox actions = new HBox(10);
        Button addZoneBtn = new Button("Create New Zone");
        addZoneBtn.setOnAction(e -> showCreateCropZoneDialog());
        actions.getChildren().add(addZoneBtn);
        container.getChildren().add(actions);

        TableView<CropZone> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(cropZones));

        TableColumn<CropZone, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));

        TableColumn<CropZone, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<CropZone, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));

        TableColumn<CropZone, String> countCol = new TableColumn<>("Crops");
        countCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getEntityCount())));

        table.getColumns().addAll(codeCol, nameCol, statusCol, countCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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

        TableColumn<LivestockZone, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<LivestockZone, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));

        TableColumn<LivestockZone, String> programCol = new TableColumn<>("Feeding Program");
        programCol.setCellValueFactory(data -> {
            FeedingProgram fp = data.getValue().getFeedingProgram();
            return new SimpleStringProperty(fp != null ? fp.getFeedType() : "None");
        });

        table.getColumns().addAll(codeCol, nameCol, statusCol, programCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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

        TableColumn<AquacultureZone, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<AquacultureZone, String> speciesCol = new TableColumn<>("Species Count");
        speciesCol.setCellValueFactory(data -> new SimpleStringProperty("Managed Species"));

        TableColumn<AquacultureZone, String> countCol = new TableColumn<>("Animal Count");
        countCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getAnimalCount())));

        table.getColumns().addAll(codeCol, nameCol, speciesCol, countCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        container.getChildren().add(table);
        contentArea.getChildren().setAll(container);
    }

    private void showAlerts() {
        VBox container = new VBox(15);
        setPageTitle("Alerts Center", container);

        TabPane tabs = new TabPane();
        Tab activeTab = new Tab("Active Alerts", createAlertTableView(activeAlerts));
        Tab historyTab = new Tab("Alert History", createAlertTableView(alertHistory));
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

        TableColumn<model.entities.Alert, String> sensorCol = new TableColumn<>("Sensor");
        sensorCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSensorCode()));

        TableColumn<model.entities.Alert, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.1f", data.getValue().getReadingValue())));

        TableColumn<model.entities.Alert, String> severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeverity().toString()));

        TableColumn<model.entities.Alert, String> statusCol = new TableColumn<>("Acknowledged");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isAcknowledged() ? "Yes" : "No"));

        table.getColumns().addAll(idCol, sensorCol, valueCol, severityCol, statusCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    private void showReports() {
        VBox container = new VBox(20);
        setPageTitle("System Reports", container);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);

        grid.add(createReportActionCard("Farm Overview", "General statistics and system status summary."), 0, 0);
        grid.add(createReportActionCard("Crop Production", "Growth stages and expected harvest dates."), 1, 0);
        grid.add(createReportActionCard("Livestock Yield", "Milk and egg production tracking."), 0, 1);
        grid.add(createReportActionCard("Sensor Health", "Uptime and connectivity report."), 1, 1);

        container.getChildren().add(grid);
        contentArea.getChildren().setAll(container);
    }

    private VBox createReportActionCard(String title, String desc) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setPrefWidth(350);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8;");

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label descLbl = new Label(desc);
        descLbl.setWrapText(true);
        descLbl.setTextFill(Color.GRAY);

        Button btn = new Button("Generate Report");
        btn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white;");

        card.getChildren().addAll(titleLbl, descLbl, btn);
        return card;
    }

    private void showCreateCropZoneDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Crop Zone");
        dialog.setHeaderText("Enter details for the new crop zone");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));

        TextField codeField = new TextField();
        codeField.setPromptText("CZ00X");
        TextField nameField = new TextField();
        nameField.setPromptText("Zone Name");

        grid.add(new Label("Zone Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Zone Name:"), 0, 1);
        grid.add(nameField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                cropZones.add(new CropZone(codeField.getText(), nameField.getText()));
                showCropZones();
            }
        });
    }

    private void loadSampleData() {
        CropZone cropZone = new CropZone("CZ001", "North Valley Farm");
        cropZone.addCrop(new Crop("Winter Wheat", CropFamily.CEREALS,
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 7, 15), 6.0, 7.5, 20.0, 30.0));
        cropZone.addCrop(new Crop("Cherry Tomato", CropFamily.VEGETABLES,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30), 6.2, 6.8, 25.0, 35.0));
        cropZones.add(cropZone);

        LivestockZone livestockZone = new LivestockZone("LZ001", "East Pasture");
        livestockZone.setFeedingProgram(new FeedingProgram("Organic Hay Mix", 5.5, 3));
        Ruminant cow = new Ruminant("R1001", "Holstein Friesian", 4, 650.0);
        cow.addMilkYield(125.5);
        livestockZone.addAnimal(cow);
        livestockZone.addAnimal(new Poultry("P1001", "Rhode Island Red", 1, 2.5));
        livestockZones.add(livestockZone);

        AquacultureZone aquaZone = new AquacultureZone("AZ001", "West Pond");
        aquaZone.addSpecies("Nile Tilapia");
        aquaZone.addSpecies("African Catfish");
        aquaZone.setAnimalCount(1250);
        aquaZone.setFeedingProgram(new FeedingProgram("Protein Pellets", 3.5, 4));
        aquacultureZones.add(aquaZone);

        EnvironmentSensor sensor = new EnvironmentSensor("SENS101", "CZ001", 10.0, 35.0, "temperature");
        cropZone.addSensor(sensor);
        sensor.addReading(new Reading("SENS101", 23.5, "°C", LocalDateTime.now()));

        // FIXED: Added the missing LocalDateTime parameter (7 parameters total)
        model.entities.Alert alert = new model.entities.Alert(
                "AL-001",           // id
                "SENS101",          // sensorCode
                36.5,               // readingValue
                10.0,               // thresholdMin
                35.0,               // thresholdMax
                SeverityLevel.CRITICAL,  // severity
                LocalDateTime.now()      // timestamp - THIS WAS MISSING!
        );
        activeAlerts.add(alert);
    }

    public static void main(String[] args) {
        launch(args);
    }
}