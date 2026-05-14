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
    private static int zoneCounter = 1;
    private static int sensorCounter = 1;

    private BorderPane mainLayout;
    private VBox sidebar;
    private StackPane contentArea;
    private Label currentPageTitle;

    private final String PRIMARY_COLOR = "#2e7d32";
    private final String SECONDARY_COLOR = "#f5f5f5";
    private final String SIDEBAR_COLOR = "#2e7d32";
    private final String ACCENT_COLOR = "#ff6f00";
    private final String DANGER_COLOR = "#c62828";

    private Timer readingCheckerTimer;
    private boolean isAutoCheckEnabled = true;
    private int checkIntervalSeconds = 30;

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
        File dataFile = new File("data/farm_data.txt");
        if (dataFile.exists()) {
            try {
                // Clear existing data
                cropZones.clear();
                livestockZones.clear();
                aquacultureZones.clear();
                activeAlerts.clear();
                alertHistory.clear();

                // Load from file
                DataManager.loadAllData(cropZones, livestockZones, aquacultureZones, alertHistory);
                System.out.println("Data loaded from file successfully");

                // Print loaded data count for verification
                System.out.println("Loaded " + cropZones.size() + " crop zones");
                System.out.println("Loaded " + livestockZones.size() + " livestock zones");
                System.out.println("Loaded " + aquacultureZones.size() + " aquaculture zones");
                System.out.println("Loaded " + alertHistory.size() + " alerts");
            } catch (Exception e) {
                System.out.println("Error loading from file: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No data file found at: " + dataFile.getAbsolutePath());
            System.out.println("Will use sample data");
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

        Scene scene = new Scene(mainLayout, 1000, 600);
        primaryStage.setMaximized(false);
        primaryStage.setTitle("ðŸŒ¾ Smart Farming System - Complete Management Console");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createSidebar() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(25, 15, 25, 15));
        box.setPrefWidth(280);
        box.setStyle("-fx-background-color: " + SIDEBAR_COLOR + ";");

        VBox header = new VBox(5);
        Label title = new Label("ðŸŒ¾ SMART FARM");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        Label subtitle = new Label("Management System");
        subtitle.setFont(Font.font("System", 12));
        subtitle.setTextFill(Color.web("#bbdefb"));
        subtitle.setAlignment(Pos.CENTER);
        subtitle.setMaxWidth(Double.MAX_VALUE);

        header.getChildren().addAll(title, subtitle);
        header.setPadding(new Insets(0, 0, 20, 0));
        box.getChildren().add(header);

        box.getChildren().addAll(
                createSidebarButton("ðŸ“Š Dashboard", "View farm overview", this::showDashboard),
                createSeparator(),
                createSidebarButton("ðŸŒ¾ Crop Zones", "Manage crop fields", () -> showZones("crop")),
                createSidebarButton("ðŸ„ Livestock Zones", "Manage animal zones", () -> showZones("livestock")),
                createSidebarButton("ðŸŸ Aquaculture Zones", "Manage fish ponds", () -> showZones("aquaculture")),
                createSeparator(),
                createSidebarButton("ðŸ“¡ Sensors", "Configure and monitor", this::showSensors),
                createSidebarButton("âš ï¸ Alerts Center", "View and manage alerts", this::showAlerts),
                createSidebarButton("ðŸ“ˆ Reports", "Generate system reports", this::showReports),
                createSeparator(),
                createSidebarButton("âž• Create New Zone", "Add zone to farm", this::showCreateZoneMenu),
                createSidebarButton("ðŸ”§ Create New Sensor", "Add sensor to zone", this::showCreateSensorMenu)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        box.getChildren().add(spacer);

        VBox statusBar = new VBox(5);
        statusBar.setPadding(new Insets(10, 0, 0, 0));
        statusBar.setStyle("-fx-border-color: #37474f; -fx-border-width: 1 0 0 0;");

        Label stats = new Label(String.format("Zones: %d | Sensors: %d | Alerts: %d",
                cropZones.size() + livestockZones.size() + aquacultureZones.size(),
                getAllSensors().size(),
                activeAlerts.size()));
        stats.setTextFill(Color.web("#bbdefb"));
        stats.setFont(Font.font("System", 11));
        statusBar.getChildren().add(stats);

        box.getChildren().add(statusBar);

        Button quitBtn = createSidebarButton("ðŸšª Quit", "Exit application", () -> {
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Exit");
            confirm.setHeaderText("Exit Smart Farming System");
            confirm.setContentText("Are you sure you want to exit?");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                System.exit(0);
            }
        });
        quitBtn.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; -fx-alignment: CENTER-LEFT; -fx-cursor: hand; -fx-font-weight: bold;");
        box.getChildren().add(quitBtn);

        return box;
    }

    private Separator createSeparator() {
        Separator sep = new Separator();
        sep.setPadding(new Insets(5, 0, 5, 0));
        return sep;
    }

    private Button createSidebarButton(String text, String tooltip, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 15, 12, 15));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-cursor: hand;");
        btn.setTooltip(new Tooltip(tooltip));

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #1b5e20; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-cursor: hand;"));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void setPageTitle(String title) {
        if (currentPageTitle == null) {
            currentPageTitle = new Label(title);
            currentPageTitle.setFont(Font.font("System", FontWeight.BOLD, 28));
            currentPageTitle.setTextFill(Color.web(PRIMARY_COLOR));
            currentPageTitle.setPadding(new Insets(0, 0, 20, 0));
        } else {
            currentPageTitle.setText(title);
        }
    }

    private void showDashboard() {
        VBox container = new VBox(20);
        setPageTitle("ðŸ  Farm Dashboard");
        container.getChildren().add(currentPageTitle);

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(20);
        statsGrid.setAlignment(Pos.CENTER);

        int totalZones = cropZones.size() + livestockZones.size() + aquacultureZones.size();
        int totalCrops = cropZones.stream().mapToInt(CropZone::getEntityCount).sum();
        int totalAnimals = livestockZones.stream().mapToInt(LivestockZone::getEntityCount).sum() +
                aquacultureZones.stream().mapToInt(AquacultureZone::getAnimalCount).sum();
        int totalSensors = getAllSensors().size();

        statsGrid.add(createStatCard("Total Zones", String.valueOf(totalZones), "#1976d2", "ðŸ—ºï¸"), 0, 0);
        statsGrid.add(createStatCard("Active Alerts", String.valueOf(activeAlerts.size()), "#d32f2f", "âš ï¸"), 1, 0);
        statsGrid.add(createStatCard("Total Crops", String.valueOf(totalCrops), "#388e3c", "ðŸŒ¾"), 2, 0);
        statsGrid.add(createStatCard("Total Animals", String.valueOf(totalAnimals), "#fbc02d", "ðŸ„"), 3, 0);
        statsGrid.add(createStatCard("Total Sensors", String.valueOf(totalSensors), "#ff9800", "ðŸ“¡"), 0, 1);
        statsGrid.add(createStatCard("Active Zones", String.valueOf(totalZones), "#4caf50", "âœ…"), 1, 1);
        statsGrid.add(createStatCard("Critical Alerts", String.valueOf(activeAlerts.stream().filter(a -> a.getSeverity() == SeverityLevel.CRITICAL).count()), "#f44336", "ðŸ”´"), 2, 1);
        statsGrid.add(createStatCard("Warning Alerts", String.valueOf(activeAlerts.stream().filter(a -> a.getSeverity() == SeverityLevel.WARNING).count()), "#ff9800", "ðŸŸ¡"), 3, 1);

        container.getChildren().add(statsGrid);

        Label alertsLabel = new Label("ðŸ”” Recent System Alerts");
        alertsLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        alertsLabel.setTextFill(Color.web("#333"));
        container.getChildren().add(alertsLabel);

        TableView<model.entities.Alert> alertTable = createAlertTableView(activeAlerts);
        alertTable.setPrefHeight(300);
        container.getChildren().add(alertTable);

        Label quickLabel = new Label("âš¡ Quick Actions");
        quickLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        quickLabel.setTextFill(Color.web("#333"));
        quickLabel.setPadding(new Insets(10, 0, 0, 0));
        container.getChildren().add(quickLabel);

        HBox quickActions = new HBox(15);
        quickActions.setPadding(new Insets(10, 0, 0, 0));

        Button generateAlertBtn = createQuickButton("âš ï¸ Generate Alert", "#ff9800", this::showManualAlertDialog);
        Button refreshBtn = createQuickButton("ðŸ”„ Refresh All", "#607d8b", () -> showDashboard());
        Button exportBtn = createQuickButton("ðŸ“Š Export Report", "#4caf50", () -> showInfoDialog("Export", "Report exported successfully!"));

        quickActions.getChildren().addAll(generateAlertBtn, refreshBtn, exportBtn);
        container.getChildren().add(quickActions);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        contentArea.getChildren().setAll(scrollPane);
    }

    private VBox createStatCard(String title, String value, String color, String icon) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setPrefSize(200, 120);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(24));

        Label titleLbl = new Label(title);
        titleLbl.setTextFill(Color.GRAY);
        titleLbl.setFont(Font.font("System", 12));

        Label valueLbl = new Label(value);
        valueLbl.setFont(Font.font("System", FontWeight.BOLD, 32));
        valueLbl.setTextFill(Color.web(color));

        card.getChildren().addAll(iconLbl, titleLbl, valueLbl);
        return card;
    }

    private Button createQuickButton(String text, String color, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 8;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void showZones(String type) {
        VBox container = new VBox(15);

        if (type.equals("crop")) {
            setPageTitle("ðŸŒ¾ Crop Zone Management");
            container.getChildren().add(currentPageTitle);
            container.getChildren().add(createZoneTable(cropZones, "crop"));
        } else if (type.equals("livestock")) {
            setPageTitle("ðŸ„ Livestock Zone Management");
            container.getChildren().add(currentPageTitle);
            container.getChildren().add(createLivestockZoneTable());
        } else {
            setPageTitle("ðŸŸ Aquaculture Zone Management");
            container.getChildren().add(currentPageTitle);
            container.getChildren().add(createAquacultureZoneTable());
        }

        contentArea.getChildren().setAll(container);
    }

    private VBox createZoneTable(ObservableList<CropZone> zones, String type) {
        VBox wrapper = new VBox(10);

        TableView<CropZone> table = new TableView<>();
        table.setItems(zones);
        table.setPrefHeight(500);

        TableColumn<CropZone, String> codeCol = new TableColumn<>("Zone Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(120);

        TableColumn<CropZone, String> nameCol = new TableColumn<>("Zone Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<CropZone, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> new TableCell<CropZone, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("ACTIVE")) {
                        setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<CropZone, Integer> countCol = new TableColumn<>("Crop Count");
        countCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getEntityCount()).asObject());
        countCol.setPrefWidth(100);

        TableColumn<CropZone, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(250);
        actionsCol.setCellFactory(col -> new TableCell<CropZone, Void>() {
            private final Button editBtn = new Button("âœï¸ Edit");
            private final Button deleteBtn = new Button("ðŸ—‘ï¸ Delete");
            private final Button cropsBtn = new Button("ðŸŒ¾ Crops");
            private final HBox pane = new HBox(5, editBtn, deleteBtn, cropsBtn);

            {
                editBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                cropsBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-cursor: hand;");

                editBtn.setOnAction(e -> {
                    CropZone zone = getTableView().getItems().get(getIndex());
                    showEditCropZoneDialog(zone);
                });

                deleteBtn.setOnAction(e -> {
                    CropZone zone = getTableView().getItems().get(getIndex());
                    showDeleteConfirmation("Zone", zone.getName(), () -> {
                        cropZones.remove(zone);
                        showZones("crop");
                        showInfoDialog("Deleted", "Zone deleted successfully");
                    });
                });

                cropsBtn.setOnAction(e -> {
                    CropZone zone = getTableView().getItems().get(getIndex());
                    showManageCropsDialog(zone);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(codeCol, nameCol, statusCol, countCol, actionsCol);
        wrapper.getChildren().add(table);

        Button addBtn = createQuickButton("âž• Add New Crop Zone", PRIMARY_COLOR, this::showCreateCropZoneDialog);
        wrapper.getChildren().add(addBtn);

        return wrapper;
    }

    private VBox createLivestockZoneTable() {
        VBox wrapper = new VBox(10);

        TableView<LivestockZone> table = new TableView<>();
        table.setItems(livestockZones);
        table.setPrefHeight(500);

        TableColumn<LivestockZone, String> codeCol = new TableColumn<>("Zone Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(120);

        TableColumn<LivestockZone, String> nameCol = new TableColumn<>("Zone Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<LivestockZone, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        statusCol.setPrefWidth(100);

        TableColumn<LivestockZone, Integer> countCol = new TableColumn<>("Animal Count");
        countCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getEntityCount()).asObject());
        countCol.setPrefWidth(100);

        TableColumn<LivestockZone, String> feedCol = new TableColumn<>("Feeding Program");
        feedCol.setCellValueFactory(data -> {
            FeedingProgram fp = data.getValue().getFeedingProgram();
            return new SimpleStringProperty(fp != null ? fp.getFeedType() + " - " + fp.getDailyQuantity() + "kg/day" : "Not set");
        });
        feedCol.setPrefWidth(200);

        TableColumn<LivestockZone, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(300);
        actionsCol.setCellFactory(col -> new TableCell<LivestockZone, Void>() {
            private final Button editBtn = new Button("âœï¸ Edit");
            private final Button deleteBtn = new Button("ðŸ—‘ï¸ Delete");
            private final Button animalsBtn = new Button("ðŸ„ Animals");
            private final Button feedBtn = new Button("ðŸ½ï¸ Feed");
            private final HBox pane = new HBox(5, editBtn, deleteBtn, animalsBtn, feedBtn);

            {
                editBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                animalsBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-cursor: hand;");
                feedBtn.setStyle("-fx-background-color: #9c27b0; -fx-text-fill: white; -fx-cursor: hand;");

                editBtn.setOnAction(e -> {
                    LivestockZone zone = getTableView().getItems().get(getIndex());
                    showEditLivestockZoneDialog(zone);
                });

                deleteBtn.setOnAction(e -> {
                    LivestockZone zone = getTableView().getItems().get(getIndex());
                    showDeleteConfirmation("Zone", zone.getName(), () -> {
                        livestockZones.remove(zone);
                        showZones("livestock");
                        showInfoDialog("Deleted", "Zone deleted successfully");
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

        table.getColumns().addAll(codeCol, nameCol, statusCol, countCol, feedCol, actionsCol);
        wrapper.getChildren().add(table);

        Button addBtn = createQuickButton("âž• Add New Livestock Zone", PRIMARY_COLOR, this::showCreateLivestockZoneDialog);
        wrapper.getChildren().add(addBtn);

        return wrapper;
    }

    private VBox createAquacultureZoneTable() {
        VBox wrapper = new VBox(10);

        TableView<AquacultureZone> table = new TableView<>();
        table.setItems(aquacultureZones);
        table.setPrefHeight(500);

        TableColumn<AquacultureZone, String> codeCol = new TableColumn<>("Zone Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(120);

        TableColumn<AquacultureZone, String> nameCol = new TableColumn<>("Zone Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<AquacultureZone, Integer> countCol = new TableColumn<>("Fish Count");
        countCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getAnimalCount()).asObject());
        countCol.setPrefWidth(100);

        TableColumn<AquacultureZone, String> speciesCol = new TableColumn<>("Species");
        speciesCol.setCellValueFactory(data -> new SimpleStringProperty(String.join(", ", data.getValue().getSpecies())));
        speciesCol.setPrefWidth(200);

        TableColumn<AquacultureZone, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(250);
        actionsCol.setCellFactory(col -> new TableCell<AquacultureZone, Void>() {
            private final Button editBtn = new Button("âœï¸ Edit");
            private final Button deleteBtn = new Button("ðŸ—‘ï¸ Delete");
            private final Button fishBtn = new Button("ðŸŸ Details");
            private final HBox pane = new HBox(5, editBtn, deleteBtn, fishBtn);

            {
                editBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                fishBtn.setStyle("-fx-background-color: #009688; -fx-text-fill: white; -fx-cursor: hand;");

                editBtn.setOnAction(e -> {
                    AquacultureZone zone = getTableView().getItems().get(getIndex());
                    showEditAquacultureZoneDialog(zone);
                });

                deleteBtn.setOnAction(e -> {
                    AquacultureZone zone = getTableView().getItems().get(getIndex());
                    showDeleteConfirmation("Zone", zone.getName(), () -> {
                        aquacultureZones.remove(zone);
                        showZones("aquaculture");
                        showInfoDialog("Deleted", "Zone deleted successfully");
                    });
                });

                fishBtn.setOnAction(e -> {
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

        Button addBtn = createQuickButton("âž• Add New Aquaculture Zone", PRIMARY_COLOR, this::showCreateAquacultureZoneDialog);
        wrapper.getChildren().add(addBtn);

        return wrapper;
    }

    private void showSensors() {
        VBox container = new VBox(15);
        setPageTitle("ðŸ“¡ Sensor Management");
        container.getChildren().add(currentPageTitle);

        TableView<SensorWrapper> table = new TableView<>();
        ObservableList<SensorWrapper> sensorList = FXCollections.observableArrayList();

        for (Sensor s : getAllSensors()) {
            sensorList.add(new SensorWrapper(s));
        }
        table.setItems(sensorList);

        TableColumn<SensorWrapper, String> codeCol = new TableColumn<>("Sensor Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(120);

        TableColumn<SensorWrapper, String> nameCol = new TableColumn<>("Sensor Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(150);

        TableColumn<SensorWrapper, String> zoneCol = new TableColumn<>("Zone");
        zoneCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getZoneCode()));
        zoneCol.setPrefWidth(120);

        TableColumn<SensorWrapper, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        typeCol.setPrefWidth(120);

        TableColumn<SensorWrapper, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> new TableCell<SensorWrapper, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("ACTIVE")) {
                        setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
                    } else if (item.equals("FAULTY")) {
                        setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<SensorWrapper, String> thresholdCol = new TableColumn<>("Threshold");
        thresholdCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getThreshold()));
        thresholdCol.setPrefWidth(150);

        TableColumn<SensorWrapper, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(250);
        actionsCol.setCellFactory(col -> new TableCell<SensorWrapper, Void>() {
            private final Button historyBtn = new Button("ðŸ“Š History");
            private final Button editBtn = new Button("âœï¸");
            private final Button deleteBtn = new Button("ðŸ—‘ï¸");
            private final Button statusBtn = new Button("ðŸ”„");
            private final HBox pane = new HBox(5, historyBtn, editBtn, statusBtn, deleteBtn);

            {
                historyBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand;");
                editBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                statusBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-cursor: hand;");

                historyBtn.setOnAction(e -> {
                    SensorWrapper sw = getTableView().getItems().get(getIndex());
                    showReadingHistory(sw.getSensor());
                });

                editBtn.setOnAction(e -> {
                    SensorWrapper sw = getTableView().getItems().get(getIndex());
                    showEditSensorDialog(sw.getSensor());
                });

                deleteBtn.setOnAction(e -> {
                    SensorWrapper sw = getTableView().getItems().get(getIndex());
                    showDeleteConfirmation("Sensor", sw.getCode(), () -> {
                        removeSensor(sw.getSensor());
                        showSensors();
                        showInfoDialog("Deleted", "Sensor deleted successfully");
                    });
                });

                statusBtn.setOnAction(e -> {
                    SensorWrapper sw = getTableView().getItems().get(getIndex());
                    toggleSensorStatus(sw.getSensor());
                    showSensors();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(codeCol, nameCol, zoneCol, typeCol, statusCol, thresholdCol, actionsCol);
        table.setPrefHeight(500);
        container.getChildren().add(table);

        HBox actions = new HBox(10);
        Button addReadingBtn = createQuickButton("ðŸ“Š Add Reading", "#2196f3", this::showAddReadingDialog);
        Button createSensorBtn = createQuickButton("âž• Create New Sensor", PRIMARY_COLOR, this::showCreateSensorMenu);
        Button refreshBtn = createQuickButton("ðŸ”„ Refresh", "#607d8b", this::showSensors);
        actions.getChildren().addAll(createSensorBtn, addReadingBtn, refreshBtn);
        container.getChildren().add(actions);

        contentArea.getChildren().setAll(container);
    }

    private void showAlerts() {
        VBox container = new VBox(15);
        setPageTitle("âš ï¸ Alerts Center");
        container.getChildren().add(currentPageTitle);

        TabPane tabs = new TabPane();

        Tab activeTab = new Tab("ðŸ”´ Active Alerts (" + activeAlerts.size() + ")");
        VBox activeContent = new VBox(10);
        TableView<model.entities.Alert> activeTable = createAlertTableView(activeAlerts);
        activeTable.setPrefHeight(500);

        HBox activeActions = new HBox(10);
        Button acknowledgeBtn = createQuickButton("âœ… Acknowledge Selected", "#4caf50", () -> {
            model.entities.Alert selected = activeTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.acknowledge();
                showInfoDialog("Acknowledged", "Alert " + selected.getId() + " acknowledged");
                showAlerts();
            } else {
                showWarningDialog("No Selection", "Please select an alert to acknowledge");
            }
        });

        Button dismissBtn = createQuickButton("âŒ Dismiss Selected", "#f44336", () -> {
            model.entities.Alert selected = activeTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.dismiss();
                activeAlerts.remove(selected);
                showInfoDialog("Dismissed", "Alert " + selected.getId() + " dismissed");
                showAlerts();
            } else {
                showWarningDialog("No Selection", "Please select an alert to dismiss");
            }
        });

        activeActions.getChildren().addAll(acknowledgeBtn, dismissBtn);
        activeContent.getChildren().addAll(activeTable, activeActions);
        activeTab.setContent(activeContent);

        Tab historyTab = new Tab("ðŸ“œ Alert History (" + alertHistory.size() + ")");
        TableView<model.entities.Alert> historyTable = createAlertTableView(alertHistory);
        historyTable.setPrefHeight(500);

        HBox historyActions = new HBox(10);
        Button clearHistoryBtn = createQuickButton("ðŸ—‘ï¸ Clear History", "#f44336", () -> {
            showDeleteConfirmation("Alert History", "all alerts", () -> {
                alertHistory.clear();
                showAlerts();
                showInfoDialog("Cleared", "Alert history cleared");
            });
        });
        historyActions.getChildren().add(clearHistoryBtn);

        VBox historyContent = new VBox(10);
        historyContent.getChildren().addAll(historyTable, historyActions);
        historyTab.setContent(historyContent);

        activeTab.setClosable(false);
        historyTab.setClosable(false);
        tabs.getTabs().addAll(activeTab, historyTab);
        tabs.setPrefHeight(600);

        container.getChildren().add(tabs);
        contentArea.getChildren().setAll(container);
    }

    private void showReports() {
        VBox container = new VBox(20);
        setPageTitle("ðŸ“ˆ System Reports");
        container.getChildren().add(currentPageTitle);

        ScrollPane scrollPane = new ScrollPane();
        VBox reportsBox = new VBox(15);
        reportsBox.setPadding(new Insets(10));

        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(15);
        summaryGrid.setVgap(15);

        int totalZones = cropZones.size() + livestockZones.size() + aquacultureZones.size();
        summaryGrid.add(createStatCard("Total Zones", String.valueOf(totalZones), "#1976d2", "ðŸ­"), 0, 0);
        summaryGrid.add(createStatCard("Total Revenue (est.)", "$45,230", "#4caf50", "ðŸ’°"), 1, 0);
        summaryGrid.add(createStatCard("Active Sensors", String.valueOf(getAllSensors().stream().filter(s -> s.getStatus() == SensorStatus.ACTIVE).count()), "#2196f3", "ðŸ“¡"), 2, 0);

        reportsBox.getChildren().add(summaryGrid);

        TitledPane cropReport = new TitledPane();
        cropReport.setText("ðŸŒ¾ Crop Production Report");
        VBox cropContent = new VBox(10);
        cropContent.setPadding(new Insets(15));

        for (CropZone zone : cropZones) {
            Label zoneLabel = new Label("ðŸ“ " + zone.getName() + " (" + zone.getCode() + ")");
            zoneLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            zoneLabel.setTextFill(Color.web(PRIMARY_COLOR));
            cropContent.getChildren().add(zoneLabel);

            for (Crop c : zone.getCrops()) {
                String info = "   â€¢ " + c.getName() + " | Stage: " + c.getGrowthStage() +
                        " | Planted: " + c.getPlantingDate() + " | Harvest: " + c.getExpectedHarvestDate();
                cropContent.getChildren().add(new Label(info));
            }

            if (zone.getCrops().isEmpty()) {
                cropContent.getChildren().add(new Label("   No crops in this zone"));
            }
        }

        if (cropZones.isEmpty()) {
            cropContent.getChildren().add(new Label("No crop zones available."));
        }
        cropReport.setContent(cropContent);
        cropReport.setExpanded(true);
        reportsBox.getChildren().add(cropReport);

        TitledPane livestockReport = new TitledPane();
        livestockReport.setText("ðŸ„ Livestock Production Report");
        VBox livestockContent = new VBox(10);
        livestockContent.setPadding(new Insets(15));

        double totalMilk = 0;
        int totalEggs = 0;

        for (LivestockZone zone : livestockZones) {
            Label zoneLabel = new Label("ðŸ“ " + zone.getName() + " (" + zone.getCode() + ")");
            zoneLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            zoneLabel.setTextFill(Color.web(PRIMARY_COLOR));
            livestockContent.getChildren().add(zoneLabel);

            for (Animal a : zone.getAnimals()) {
                String info = "   â€¢ " + a.getId() + " - " + a.getSpecies() + " | Age: " + a.getAge() + " yrs | Health: " + a.getHealthStatus();
                livestockContent.getChildren().add(new Label(info));

                if (a instanceof Ruminant) {
                    double milk = ((Ruminant) a).getMilkYield();
                    totalMilk += milk;
                    livestockContent.getChildren().add(new Label("      ðŸ¥› Milk Yield: " + String.format("%.1f", milk) + " L"));
                }
                if (a instanceof Poultry) {
                    int eggs = ((Poultry) a).getEggCount();
                    totalEggs += eggs;
                    livestockContent.getChildren().add(new Label("      ðŸ¥š Egg Count: " + eggs));
                }
            }

            if (zone.getAnimals().isEmpty()) {
                livestockContent.getChildren().add(new Label("   No animals in this zone"));
            }
        }

        livestockContent.getChildren().add(new Separator());
        livestockContent.getChildren().add(new Label("ðŸ“Š TOTAL MILK PRODUCTION: " + String.format("%.1f", totalMilk) + " L"));
        livestockContent.getChildren().add(new Label("ðŸ“Š TOTAL EGG PRODUCTION: " + totalEggs));
        livestockReport.setContent(livestockContent);
        livestockReport.setExpanded(true);
        reportsBox.getChildren().add(livestockReport);

        TitledPane sensorReport = new TitledPane();
        sensorReport.setText("ðŸ“¡ Sensor Health Report");
        VBox sensorContent = new VBox(10);
        sensorContent.setPadding(new Insets(15));

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
            sensorContent.getChildren().add(new Label("   â€¢ " + s.getCode() + " (" + s.getClass().getSimpleName() + ") | Status: " + s.getStatus() + " | Last: " + lastReading));
        }

        sensorContent.getChildren().add(new Separator());
        sensorContent.getChildren().add(new Label("ðŸ“Š Total Sensors: " + totalSensors));
        sensorContent.getChildren().add(new Label("   âœ… Active: " + activeCount));
        sensorContent.getChildren().add(new Label("   âš ï¸ Faulty: " + faultyCount));
        sensorContent.getChildren().add(new Label("   â¸ï¸ Suspended: " + suspendedCount));
        sensorReport.setContent(sensorContent);
        sensorReport.setExpanded(true);
        reportsBox.getChildren().add(sensorReport);

        TitledPane alertReport = new TitledPane();
        alertReport.setText("âš ï¸ Alert System Report");
        VBox alertContent = new VBox(10);
        alertContent.setPadding(new Insets(15));

        int criticalCount = 0;
        int warningCount = 0;
        int acknowledgedCount = 0;
        int dismissedCount = 0;

        for (model.entities.Alert a : alertHistory) {
            if (a.getSeverity() == SeverityLevel.CRITICAL) criticalCount++;
            else warningCount++;
            if (a.isAcknowledged()) acknowledgedCount++;
            if (a.isDismissed()) dismissedCount++;
        }

        alertContent.getChildren().add(new Label("ðŸ“Š Total Alerts Generated: " + alertHistory.size()));
        alertContent.getChildren().add(new Label("   ðŸ”´ Critical Alerts: " + criticalCount));
        alertContent.getChildren().add(new Label("   ðŸŸ¡ Warning Alerts: " + warningCount));
        alertContent.getChildren().add(new Label("   âœ… Acknowledged: " + acknowledgedCount));
        alertContent.getChildren().add(new Label("   âŒ Dismissed: " + dismissedCount));
        alertContent.getChildren().add(new Label("   âš ï¸ Currently Active: " + activeAlerts.size()));
        alertReport.setContent(alertContent);
        alertReport.setExpanded(true);
        reportsBox.getChildren().add(alertReport);

        scrollPane.setContent(reportsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        container.getChildren().add(scrollPane);
        contentArea.getChildren().setAll(container);
    }

    private void showCreateZoneMenu() {
        VBox container = new VBox(20);
        setPageTitle("âž• Create New Zone");
        container.getChildren().add(currentPageTitle);

        Label instruction = new Label("Select the type of zone you want to create:");
        instruction.setFont(Font.font("System", 16));
        instruction.setTextFill(Color.web("#555"));
        container.getChildren().add(instruction);

        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(30);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(30, 0, 0, 0));

        VBox cropCard = createZoneOptionCard("ðŸŒ¾", "Crop Zone", "For managing crop fields, vegetables, and fruits", "#388e3c", this::showCreateCropZoneDialog);
        VBox livestockCard = createZoneOptionCard("ðŸ„", "Livestock Zone", "For managing ruminants and poultry", "#fbc02d", this::showCreateLivestockZoneDialog);
        VBox aquacultureCard = createZoneOptionCard("ðŸŸ", "Aquaculture Zone", "For managing fish and aquatic species", "#009688", this::showCreateAquacultureZoneDialog);

        grid.add(cropCard, 0, 0);
        grid.add(livestockCard, 1, 0);
        grid.add(aquacultureCard, 2, 0);

        container.getChildren().add(grid);
        contentArea.getChildren().setAll(container);
    }

    private VBox createZoneOptionCard(String icon, String title, String description, String color, Runnable action) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(25));
        card.setPrefSize(280, 200);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 0); -fx-cursor: hand;");
        card.setAlignment(Pos.CENTER);

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(48));

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLbl.setTextFill(Color.web(color));

        Label descLbl = new Label(description);
        descLbl.setFont(Font.font("System", 12));
        descLbl.setTextFill(Color.GRAY);
        descLbl.setWrapText(true);
        descLbl.setAlignment(Pos.CENTER);

        card.getChildren().addAll(iconLbl, titleLbl, descLbl);

        card.setOnMouseClicked(e -> action.run());
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 20, 0, 0, 0); -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 0); -fx-cursor: hand;"));

        return card;
    }

    private void showCreateSensorMenu() {
        VBox container = new VBox(20);
        setPageTitle("ðŸ”§ Create New Sensor");
        container.getChildren().add(currentPageTitle);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(30, 0, 0, 0));

        VBox envCard = createSensorOptionCard("ðŸŒ¡ï¸", "Environment Sensor", "Temperature, Humidity, Rainfall", "#2196f3", () -> showCreateSensorDialog("EnvironmentSensor"));
        VBox soilCard = createSensorOptionCard("ðŸŒ±", "Soil Sensor", "pH, Moisture, Nitrogen", "#4caf50", () -> showCreateSensorDialog("SoilSensor"));
        VBox bioCard = createSensorOptionCard("â¤ï¸", "Biometric Sensor", "Animal temperature, Activity", "#ff9800", () -> showCreateSensorDialog("BiometricSensor"));
        VBox waterCard = createSensorOptionCard("ðŸ’§", "Water Sensor", "Water temperature, Oxygen", "#009688", () -> showCreateSensorDialog("WaterSensor"));
        VBox gpsCard = createSensorOptionCard("ðŸ“", "GPS Sensor", "Animal location tracking", "#9c27b0", () -> showCreateSensorDialog("GPSSensor"));

        grid.add(envCard, 0, 0);
        grid.add(soilCard, 1, 0);
        grid.add(bioCard, 2, 0);
        grid.add(waterCard, 0, 1);
        grid.add(gpsCard, 1, 1);

        container.getChildren().add(grid);
        contentArea.getChildren().setAll(container);
    }

    private VBox createSensorOptionCard(String icon, String title, String description, String color, Runnable action) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setPrefSize(220, 160);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0); -fx-cursor: hand;");
        card.setAlignment(Pos.CENTER);

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(36));

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLbl.setTextFill(Color.web(color));

        Label descLbl = new Label(description);
        descLbl.setFont(Font.font("System", 10));
        descLbl.setTextFill(Color.GRAY);
        descLbl.setWrapText(true);
        descLbl.setAlignment(Pos.CENTER);

        card.getChildren().addAll(iconLbl, titleLbl, descLbl);
        card.setOnMouseClicked(e -> action.run());

        return card;
    }

    private void showCreateSensorDialog(String sensorType) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create " + sensorType);
        dialog.setHeaderText("Configure " + sensorType + " Details");
        dialog.setResizable(true);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setPrefWidth(500);

        TextField codeField = new TextField();
        codeField.setPromptText("e.g., SENS" + String.format("%03d", sensorCounter));

        TextField nameField = new TextField();
        nameField.setPromptText("e.g., North Field Temperature Sensor");

        ComboBox<String> zoneBox = new ComboBox<>();
        for (CropZone z : cropZones) zoneBox.getItems().add(z.getCode() + " - " + z.getName());
        for (LivestockZone z : livestockZones) zoneBox.getItems().add(z.getCode() + " - " + z.getName());
        for (AquacultureZone z : aquacultureZones) zoneBox.getItems().add(z.getCode() + " - " + z.getName());

        TextField minField = new TextField();
        minField.setPromptText("Minimum threshold");

        TextField maxField = new TextField();
        maxField.setPromptText("Maximum threshold");

        ComboBox<String> measurementBox = new ComboBox<>();

        switch (sensorType) {
            case "EnvironmentSensor":
                measurementBox.getItems().addAll("temperature", "humidity", "rainfall");
                break;
            case "SoilSensor":
                measurementBox.getItems().addAll("ph", "moisture", "nitrogen");
                break;
            case "BiometricSensor":
                measurementBox.getItems().addAll("temperature", "activity");
                break;
            case "WaterSensor":
                measurementBox.getItems().addAll("temperature", "dissolved_oxygen");
                break;
            case "GPSSensor":
                measurementBox.setVisible(false);
                break;
        }

        int row = 0;
        grid.add(new Label("Sensor Code:"), 0, row);
        grid.add(codeField, 1, row++);
        grid.add(new Label("Sensor Name:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Assign to Zone:"), 0, row);
        grid.add(zoneBox, 1, row++);
        grid.add(new Label("Min Threshold:"), 0, row);
        grid.add(minField, 1, row++);
        grid.add(new Label("Max Threshold:"), 0, row);
        grid.add(maxField, 1, row++);

        if (!sensorType.equals("GPSSensor")) {
            grid.add(new Label("Measurement Type:"), 0, row);
            grid.add(measurementBox, 1, row++);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && !codeField.getText().isEmpty() && !nameField.getText().isEmpty() && zoneBox.getValue() != null) {
                try {
                    String zoneCode = zoneBox.getValue().split(" - ")[0];
                    double min = Double.parseDouble(minField.getText());
                    double max = Double.parseDouble(maxField.getText());

                    Sensor sensor = null;
                    String measurement = measurementBox.getValue();

                    switch (sensorType) {
                        case "EnvironmentSensor":
                            sensor = new EnvironmentSensor(codeField.getText(), zoneCode, min, max, measurement);
                            break;
                        case "SoilSensor":
                            sensor = new SoilSensor(codeField.getText(), zoneCode, min, max, measurement);
                            break;
                        case "BiometricSensor":
                            sensor = new BiometricSensor(codeField.getText(), zoneCode, min, max, "ANM001", measurement);
                            break;
                        case "WaterSensor":
                            sensor = new WaterSensor(codeField.getText(), zoneCode, min, max, measurement);
                            break;
                        case "GPSSensor":
                            sensor = new GPSSensor(codeField.getText(), zoneCode, min, max, "ANM001");
                            break;
                    }

                    if (sensor != null) {
                        addSensorToZone(sensor);
                        sensorCounter++;
                        showInfoDialog("Success", sensorType + " created successfully!");
                        showSensors();
                    }
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number format for thresholds");
                }
            }
        });
    }

    private void showEditSensorDialog(Sensor sensor) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Sensor");
        dialog.setHeaderText("Edit " + sensor.getClass().getSimpleName() + " Configuration");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField minField = new TextField(String.valueOf(sensor.getThresholdMin()));
        TextField maxField = new TextField(String.valueOf(sensor.getThresholdMax()));

        ComboBox<SensorStatus> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(SensorStatus.values());
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
                    showInfoDialog("Success", "Sensor updated successfully!");
                    showSensors();
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number format");
                }
            }
        });
    }

    private void showReadingHistory(Sensor sensor) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reading History - " + sensor.getCode());
        dialog.setHeaderText("Sensor: " + sensor.getClass().getSimpleName() + " | Unit: " + sensor.getUnit());
        dialog.setResizable(true);
        dialog.setWidth(800);
        dialog.setHeight(600);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        TableView<Reading> table = new TableView<>();
        ObservableList<Reading> readings = FXCollections.observableArrayList(sensor.getReadings());
        table.setItems(readings);

        TableColumn<Reading, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        timeCol.setPrefWidth(200);

        TableColumn<Reading, Double> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getValue()).asObject());
        valueCol.setPrefWidth(150);

        TableColumn<Reading, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnit()));
        unitCol.setPrefWidth(100);

        table.getColumns().addAll(timeCol, valueCol, unitCol);
        table.setPrefHeight(300);

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Reading Number");
        yAxis.setLabel("Value (" + sensor.getUnit() + ")");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Sensor Readings Trend");
        lineChart.setPrefHeight(250);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(sensor.getCode());

        for (int i = 0; i < sensor.getReadings().size(); i++) {
            series.getData().add(new XYChart.Data<>(i + 1, sensor.getReadings().get(i).getValue()));
        }

        lineChart.getData().add(series);

        content.getChildren().addAll(new Label("Reading History:"), table, new Label("Trend Chart:"), lineChart);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showCreateCropZoneDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Crop Zone");
        dialog.setHeaderText("Enter crop zone details");
        dialog.setResizable(true);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setPrefWidth(500);

        TextField codeField = new TextField("CZ" + String.format("%03d", zoneCounter));
        TextField nameField = new TextField();
        nameField.setPromptText("e.g., North Valley Farm");

        grid.add(new Label("Zone Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Zone Name:"), 0, 1);
        grid.add(nameField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && !codeField.getText().isEmpty() && !nameField.getText().isEmpty()) {
                CropZone newZone = new CropZone(codeField.getText(), nameField.getText());
                cropZones.add(newZone);
                zoneCounter++;
                showInfoDialog("Success", "Crop Zone '" + nameField.getText() + "' created successfully!");

                javafx.scene.control.Alert cropAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                cropAlert.setTitle("Add Crops");
                cropAlert.setHeaderText("Would you like to add crops to this zone?");
                cropAlert.setContentText("You can add crops now or later by editing the zone.");

                if (cropAlert.showAndWait().get() == ButtonType.OK) {
                    showManageCropsDialog(newZone);
                } else {
                    showZones("crop");
                }
            }
        });
    }

    private void showEditCropZoneDialog(CropZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Crop Zone");
        dialog.setHeaderText("Edit zone details");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(zone.getName());
        ComboBox<ZoneStatus> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(ZoneStatus.values());
        statusBox.setValue(zone.getStatus());

        grid.add(new Label("Zone Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);

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
                showInfoDialog("Success", "Zone updated successfully!");
                showZones("crop");
            }
        });
    }

    private void showManageCropsDialog(CropZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Manage Crops - " + zone.getName());
        dialog.setHeaderText("Add or remove crops");
        dialog.setResizable(true);
        dialog.setWidth(800);
        dialog.setHeight(600);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        TableView<Crop> table = new TableView<>();
        ObservableList<Crop> crops = FXCollections.observableArrayList(zone.getCrops());
        table.setItems(crops);
        table.setPrefHeight(300);

        TableColumn<Crop, String> nameCol = new TableColumn<>("Crop Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(120);

        TableColumn<Crop, String> familyCol = new TableColumn<>("Family");
        familyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFamily().toString()));
        familyCol.setPrefWidth(100);

        TableColumn<Crop, String> stageCol = new TableColumn<>("Growth Stage");
        stageCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGrowthStage().toString()));
        stageCol.setPrefWidth(100);

        TableColumn<Crop, String> plantingCol = new TableColumn<>("Planting Date");
        plantingCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPlantingDate().toString()));
        plantingCol.setPrefWidth(120);

        TableColumn<Crop, String> harvestCol = new TableColumn<>("Harvest Date");
        harvestCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getExpectedHarvestDate().toString()));
        harvestCol.setPrefWidth(120);

        TableColumn<Crop, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<Crop, Void>() {
            private final Button removeBtn = new Button("Remove");
            {
                removeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                removeBtn.setOnAction(e -> {
                    Crop crop = getTableView().getItems().get(getIndex());
                    zone.getCrops().remove(crop);
                    table.setItems(FXCollections.observableArrayList(zone.getCrops()));
                    showInfoDialog("Removed", "Crop removed successfully");
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });
        actionsCol.setPrefWidth(100);

        table.getColumns().addAll(nameCol, familyCol, stageCol, plantingCol, harvestCol, actionsCol);

        TitledPane addCropPane = new TitledPane();
        addCropPane.setText("âž• Add New Crop");
        addCropPane.setExpanded(true);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(15);
        formGrid.setPadding(new Insets(15));

        TextField cropNameField = new TextField();
        cropNameField.setPromptText("e.g., Winter Wheat");

        ComboBox<CropFamily> familyBox = new ComboBox<>();
        familyBox.getItems().addAll(CropFamily.values());

        DatePicker plantingDate = new DatePicker(LocalDate.now());
        DatePicker harvestDate = new DatePicker(LocalDate.now().plusMonths(3));

        TextField phMinField = new TextField("6.0");
        TextField phMaxField = new TextField("7.5");
        TextField moistureMinField = new TextField("20.0");
        TextField moistureMaxField = new TextField("30.0");

        ComboBox<GrowthStage> stageBox = new ComboBox<>();
        stageBox.getItems().addAll(GrowthStage.values());
        stageBox.setValue(GrowthStage.SOWING);

        formGrid.add(new Label("Crop Name:"), 0, 0);
        formGrid.add(cropNameField, 1, 0);
        formGrid.add(new Label("Family:"), 0, 1);
        formGrid.add(familyBox, 1, 1);
        formGrid.add(new Label("Planting Date:"), 0, 2);
        formGrid.add(plantingDate, 1, 2);
        formGrid.add(new Label("Expected Harvest:"), 0, 3);
        formGrid.add(harvestDate, 1, 3);
        formGrid.add(new Label("pH Range (min-max):"), 0, 4);
        formGrid.add(new HBox(10, phMinField, new Label("-"), phMaxField), 1, 4);
        formGrid.add(new Label("Moisture % (min-max):"), 0, 5);
        formGrid.add(new HBox(10, moistureMinField, new Label("-"), moistureMaxField), 1, 5);
        formGrid.add(new Label("Growth Stage:"), 0, 6);
        formGrid.add(stageBox, 1, 6);

        Button addBtn = new Button("ðŸŒ¾ Add Crop");
        addBtn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        addBtn.setOnAction(e -> {
            if (!cropNameField.getText().isEmpty() && familyBox.getValue() != null) {
                Crop newCrop = new Crop(
                        cropNameField.getText(),
                        familyBox.getValue(),
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
                showInfoDialog("Success", "Crop added successfully!");
            }
        });

        formGrid.add(addBtn, 0, 7, 2, 1);
        addCropPane.setContent(formGrid);

        content.getChildren().addAll(new Label("Current Crops:"), table, addCropPane);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();

        showZones("crop");
    }

    private void showCreateLivestockZoneDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Livestock Zone");
        dialog.setHeaderText("Enter livestock zone details");
        dialog.setResizable(true);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setPrefWidth(500);

        TextField codeField = new TextField("LZ" + String.format("%03d", zoneCounter));
        TextField nameField = new TextField();
        nameField.setPromptText("e.g., West Pasture");

        grid.add(new Label("Zone Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Zone Name:"), 0, 1);
        grid.add(nameField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && !codeField.getText().isEmpty() && !nameField.getText().isEmpty()) {
                LivestockZone newZone = new LivestockZone(codeField.getText(), nameField.getText());
                livestockZones.add(newZone);
                zoneCounter++;
                showInfoDialog("Success", "Livestock Zone '" + nameField.getText() + "' created successfully!");

                javafx.scene.control.Alert animalAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                animalAlert.setTitle("Add Animals");
                animalAlert.setHeaderText("Would you like to add animals to this zone?");
                animalAlert.setContentText("You can add animals now or later.");

                if (animalAlert.showAndWait().get() == ButtonType.OK) {
                    showManageAnimalsDialog(newZone);
                } else {
                    showZones("livestock");
                }
            }
        });
    }

    private void showEditLivestockZoneDialog(LivestockZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Livestock Zone");
        dialog.setHeaderText("Edit zone details");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(zone.getName());
        ComboBox<ZoneStatus> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(ZoneStatus.values());
        statusBox.setValue(zone.getStatus());

        grid.add(new Label("Zone Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);

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
                showInfoDialog("Success", "Zone updated successfully!");
                showZones("livestock");
            }
        });
    }

    private void showManageAnimalsDialog(LivestockZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Manage Animals - " + zone.getName());
        dialog.setHeaderText("Add or remove animals");
        dialog.setResizable(true);
        dialog.setWidth(900);
        dialog.setHeight(700);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        TableView<Animal> table = new TableView<>();
        ObservableList<Animal> animals = FXCollections.observableArrayList(zone.getAnimals());
        table.setItems(animals);
        table.setPrefHeight(350);

        TableColumn<Animal, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        idCol.setPrefWidth(80);

        TableColumn<Animal, String> speciesCol = new TableColumn<>("Species");
        speciesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSpecies()));
        speciesCol.setPrefWidth(120);

        TableColumn<Animal, Integer> ageCol = new TableColumn<>("Age");
        ageCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getAge()).asObject());
        ageCol.setPrefWidth(60);

        TableColumn<Animal, Double> weightCol = new TableColumn<>("Weight (kg)");
        weightCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getWeight()).asObject());
        weightCol.setPrefWidth(80);

        TableColumn<Animal, String> healthCol = new TableColumn<>("Health");
        healthCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getHealthStatus().toString()));
        healthCol.setPrefWidth(100);

        TableColumn<Animal, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAnimalType().toString()));
        typeCol.setPrefWidth(80);

        TableColumn<Animal, String> productionCol = new TableColumn<>("Production");
        productionCol.setCellValueFactory(data -> {
            if (data.getValue() instanceof Ruminant) {
                return new SimpleStringProperty("Milk: " + ((Ruminant) data.getValue()).getMilkYield() + " L");
            } else if (data.getValue() instanceof Poultry) {
                return new SimpleStringProperty("Eggs: " + ((Poultry) data.getValue()).getEggCount());
            }
            return new SimpleStringProperty("-");
        });
        productionCol.setPrefWidth(120);

        TableColumn<Animal, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<Animal, Void>() {
            private final Button removeBtn = new Button("Remove");
            private final Button eventBtn = new Button("Events");
            private final HBox pane = new HBox(5, eventBtn, removeBtn);
            {
                removeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                eventBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand;");

                removeBtn.setOnAction(e -> {
                    Animal animal = getTableView().getItems().get(getIndex());
                    zone.getAnimals().remove(animal);
                    table.setItems(FXCollections.observableArrayList(zone.getAnimals()));
                    showInfoDialog("Removed", "Animal removed successfully");
                });

                eventBtn.setOnAction(e -> {
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
        actionsCol.setPrefWidth(150);

        table.getColumns().addAll(idCol, speciesCol, ageCol, weightCol, healthCol, typeCol, productionCol, actionsCol);

        TitledPane addAnimalPane = new TitledPane();
        addAnimalPane.setText("âž• Add New Animal");
        addAnimalPane.setExpanded(true);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(15);
        formGrid.setPadding(new Insets(15));

        TextField idField = new TextField();
        idField.setPromptText("e.g., R1001");

        TextField speciesField = new TextField();
        speciesField.setPromptText("e.g., Holstein Friesian");

        TextField ageField = new TextField();
        ageField.setPromptText("Age in years");

        TextField weightField = new TextField();
        weightField.setPromptText("Weight in kg");

        ComboBox<AnimalType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(AnimalType.values());

        ComboBox<HealthStatus> healthBox = new ComboBox<>();
        healthBox.getItems().addAll(HealthStatus.values());
        healthBox.setValue(HealthStatus.HEALTHY);

        formGrid.add(new Label("Animal ID:"), 0, 0);
        formGrid.add(idField, 1, 0);
        formGrid.add(new Label("Species:"), 0, 1);
        formGrid.add(speciesField, 1, 1);
        formGrid.add(new Label("Age (years):"), 0, 2);
        formGrid.add(ageField, 1, 2);
        formGrid.add(new Label("Weight (kg):"), 0, 3);
        formGrid.add(weightField, 1, 3);
        formGrid.add(new Label("Animal Type:"), 0, 4);
        formGrid.add(typeBox, 1, 4);
        formGrid.add(new Label("Health Status:"), 0, 5);
        formGrid.add(healthBox, 1, 5);

        Button addBtn = new Button("ðŸ„ Add Animal");
        addBtn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        addBtn.setOnAction(e -> {
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
                    animal.setHealthStatus(healthBox.getValue());

                    zone.addAnimal(animal);
                    table.setItems(FXCollections.observableArrayList(zone.getAnimals()));
                    idField.clear();
                    speciesField.clear();
                    ageField.clear();
                    weightField.clear();
                    showInfoDialog("Success", "Animal added successfully!");
                } catch (NumberFormatException ex) {
                    showErrorDialog("Error", "Invalid number format for age or weight");
                }
            }
        });

        formGrid.add(addBtn, 0, 6, 2, 1);
        addAnimalPane.setContent(formGrid);

        content.getChildren().addAll(new Label("Current Animals:"), table, addAnimalPane);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();

        showZones("livestock");
    }

    private void showAnimalHealthEvents(Animal animal) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Health Events - " + animal.getId());
        dialog.setHeaderText(animal.getSpecies() + " | " + animal.getAnimalType());
        dialog.setResizable(true);
        dialog.setWidth(600);
        dialog.setHeight(400);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(10);
        infoGrid.setPadding(new Insets(0, 0, 10, 0));

        infoGrid.add(new Label("ID:"), 0, 0);
        infoGrid.add(new Label(animal.getId()), 1, 0);
        infoGrid.add(new Label("Species:"), 2, 0);
        infoGrid.add(new Label(animal.getSpecies()), 3, 0);
        infoGrid.add(new Label("Age:"), 0, 1);
        infoGrid.add(new Label(animal.getAge() + " years"), 1, 1);
        infoGrid.add(new Label("Weight:"), 2, 1);
        infoGrid.add(new Label(animal.getWeight() + " kg"), 3, 1);
        infoGrid.add(new Label("Health:"), 0, 2);
        infoGrid.add(new Label(animal.getHealthStatus().toString()), 1, 2);

        content.getChildren().add(infoGrid);

        ListView<String> eventsList = new ListView<>();
        eventsList.getItems().addAll(animal.getHealthEvents());
        eventsList.setPrefHeight(200);
        content.getChildren().addAll(new Label("Health Event History:"), eventsList);

        HBox addEventBox = new HBox(10);
        TextField eventField = new TextField();
        eventField.setPromptText("New health event");
        eventField.setPrefWidth(400);

        Button addEventBtn = new Button("Add Event");
        addEventBtn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-cursor: hand;");
        addEventBtn.setOnAction(e -> {
            if (!eventField.getText().isEmpty()) {
                animal.logHealthEvent(eventField.getText());
                eventsList.getItems().add(eventField.getText());
                eventField.clear();
                showInfoDialog("Added", "Health event recorded");
            }
        });

        addEventBox.getChildren().addAll(eventField, addEventBtn);
        content.getChildren().add(addEventBox);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showSetFeedingDialog(LivestockZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Set Feeding Program - " + zone.getName());
        dialog.setHeaderText("Configure feeding schedule");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
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
        grid.add(new Label("Quantity per Meal (kg):"), 0, 1);
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
                    showInfoDialog("Success", "Feeding program updated!");
                    showZones("livestock");
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number format");
                }
            }
        });
    }

    private void showCreateAquacultureZoneDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Aquaculture Zone");
        dialog.setHeaderText("Enter aquaculture zone details");
        dialog.setResizable(true);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setPrefWidth(500);

        TextField codeField = new TextField("AZ" + String.format("%03d", zoneCounter));
        TextField nameField = new TextField();
        nameField.setPromptText("e.g., East Pond");

        grid.add(new Label("Zone Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Zone Name:"), 0, 1);
        grid.add(nameField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && !codeField.getText().isEmpty() && !nameField.getText().isEmpty()) {
                AquacultureZone newZone = new AquacultureZone(codeField.getText(), nameField.getText());
                aquacultureZones.add(newZone);
                zoneCounter++;
                showInfoDialog("Success", "Aquaculture Zone '" + nameField.getText() + "' created successfully!");

                showAquacultureSetupDialog(newZone);
            }
        });
    }

    private void showAquacultureSetupDialog(AquacultureZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Setup Aquaculture Zone - " + zone.getName());
        dialog.setHeaderText("Configure species and feeding");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField speciesField = new TextField();
        speciesField.setPromptText("e.g., Tilapia, Catfish (comma separated)");

        TextField countField = new TextField();
        countField.setPromptText("Number of fish");

        TextField feedTypeField = new TextField();
        feedTypeField.setPromptText("Feed type");

        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity per meal (kg)");

        TextField mealsField = new TextField();
        mealsField.setPromptText("Meals per day");

        grid.add(new Label("Species (comma separated):"), 0, 0);
        grid.add(speciesField, 1, 0);
        grid.add(new Label("Animal Count:"), 0, 1);
        grid.add(countField, 1, 1);
        grid.add(new Label("Feed Type:"), 0, 2);
        grid.add(feedTypeField, 1, 2);
        grid.add(new Label("Quantity/Meal (kg):"), 0, 3);
        grid.add(quantityField, 1, 3);
        grid.add(new Label("Meals per Day:"), 0, 4);
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

                    double quantity = Double.parseDouble(quantityField.getText());
                    int meals = Integer.parseInt(mealsField.getText());
                    zone.setFeedingProgram(new FeedingProgram(feedTypeField.getText(), quantity, meals));

                    showInfoDialog("Success", "Aquaculture zone configured successfully!");
                    showZones("aquaculture");
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number format");
                }
            }
        });
    }

    private void showEditAquacultureZoneDialog(AquacultureZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Aquaculture Zone");
        dialog.setHeaderText("Edit zone details");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(zone.getName());
        TextField countField = new TextField(String.valueOf(zone.getAnimalCount()));

        grid.add(new Label("Zone Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Animal Count:"), 0, 1);
        grid.add(countField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                zone.setName(nameField.getText());
                zone.setAnimalCount(Integer.parseInt(countField.getText()));
                showInfoDialog("Success", "Zone updated successfully!");
                showZones("aquaculture");
            }
        });
    }

    private void showAquacultureDetails(AquacultureZone zone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Aquaculture Zone Details - " + zone.getName());
        dialog.setHeaderText("Detailed information");
        dialog.setResizable(true);
        dialog.setWidth(600);
        dialog.setHeight(500);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(10);

        infoGrid.add(new Label("Zone Code:"), 0, 0);
        infoGrid.add(new Label(zone.getCode()), 1, 0);
        infoGrid.add(new Label("Zone Name:"), 0, 1);
        infoGrid.add(new Label(zone.getName()), 1, 1);
        infoGrid.add(new Label("Animal Count:"), 0, 2);
        infoGrid.add(new Label(String.valueOf(zone.getAnimalCount())), 1, 2);
        infoGrid.add(new Label("Species:"), 0, 3);
        infoGrid.add(new Label(String.join(", ", zone.getSpecies())), 1, 3);

        if (zone.getFeedingProgram() != null) {
            infoGrid.add(new Label("Feed Type:"), 0, 4);
            infoGrid.add(new Label(zone.getFeedingProgram().getFeedType()), 1, 4);
            infoGrid.add(new Label("Daily Feed:"), 0, 5);
            infoGrid.add(new Label(zone.getFeedingProgram().getDailyQuantity() + " kg/day"), 1, 5);
        }

        content.getChildren().add(infoGrid);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showAddReadingDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Sensor Reading");
        dialog.setHeaderText("Record a new sensor reading");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        ComboBox<String> sensorBox = new ComboBox<>();
        for (Sensor s : getAllSensors()) {
            sensorBox.getItems().add(s.getCode() + " - " + s.getClass().getSimpleName() + " (" + s.getUnit() + ")");
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
                    if (sensor != null && sensor.getStatus() == SensorStatus.ACTIVE) {
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
                            showWarningDialog("Alert Triggered", "Reading out of range! Severity: " + severity + "\nValue: " + value + " " + sensor.getUnit() + "\nRange: [" + sensor.getThresholdMin() + " - " + sensor.getThresholdMax() + "]");
                        }

                        showInfoDialog("Success", "Reading recorded: " + value + " " + sensor.getUnit());
                        showSensors();
                    } else if (sensor != null && sensor.getStatus() != SensorStatus.ACTIVE) {
                        showErrorDialog("Error", "Sensor is not active. Current status: " + sensor.getStatus());
                    }
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number format");
                }
            }
        });
    }

    private void showManualAlertDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Generate Manual Alert");
        dialog.setHeaderText("Create a new system alert");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
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

                    showInfoDialog("Alert Generated", "Manual alert created with ID: " + alert.getId() + "\nSeverity: " + severity);
                    showDashboard();
                } catch (NumberFormatException e) {
                    showErrorDialog("Error", "Invalid number format");
                }
            }
        });
    }

    private void toggleSensorStatus(Sensor sensor) {
        if (sensor.getStatus() == SensorStatus.ACTIVE) {
            sensor.suspend();
            showInfoDialog("Status Changed", "Sensor " + sensor.getCode() + " has been suspended");
        } else if (sensor.getStatus() == SensorStatus.SUSPENDED) {
            sensor.activate();
            showInfoDialog("Status Changed", "Sensor " + sensor.getCode() + " has been activated");
        }
    }

    private void removeSensor(Sensor sensor) {
        for (CropZone z : cropZones) {
            if (z.getSensors().remove(sensor)) return;
        }
        for (LivestockZone z : livestockZones) {
            if (z.getSensors().remove(sensor)) return;
        }
        for (AquacultureZone z : aquacultureZones) {
            if (z.getSensors().remove(sensor)) return;
        }
    }

    private void addSensorToZone(Sensor sensor) {
        for (CropZone z : cropZones) {
            if (z.getCode().equals(sensor.getZoneCode())) {
                z.addSensor(sensor);
                return;
            }
        }
        for (LivestockZone z : livestockZones) {
            if (z.getCode().equals(sensor.getZoneCode())) {
                z.addSensor(sensor);
                return;
            }
        }
        for (AquacultureZone z : aquacultureZones) {
            if (z.getCode().equals(sensor.getZoneCode())) {
                z.addSensor(sensor);
                return;
            }
        }
    }

    private TableView<model.entities.Alert> createAlertTableView(ObservableList<model.entities.Alert> dataList) {
        TableView<model.entities.Alert> table = new TableView<>();
        table.setItems(dataList);

        TableColumn<model.entities.Alert, String> idCol = new TableColumn<>("Alert ID");
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
        severityCol.setPrefWidth(100);
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
                        setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<model.entities.Alert, String> statusCol = new TableColumn<>("Acknowledged");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isAcknowledged() ? "âœ“ Yes" : "âœ— No"));
        statusCol.setPrefWidth(100);

        TableColumn<model.entities.Alert, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        timeCol.setPrefWidth(180);

        table.getColumns().addAll(idCol, sensorCol, valueCol, thresholdCol, severityCol, statusCol, timeCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

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

        EnvironmentSensor sensor1 = new EnvironmentSensor("SENS101", "CZ001", 10.0, 35.0, "temperature");
        EnvironmentSensor sensor2 = new EnvironmentSensor("SENS102", "CZ001", 30.0, 80.0, "humidity");
        cropZone.addSensor(sensor1);
        cropZone.addSensor(sensor2);
        sensor1.addReading(new Reading("SENS101", 23.5, "Â°C", LocalDateTime.now()));
        sensor2.addReading(new Reading("SENS102", 65.0, "%", LocalDateTime.now()));

        model.entities.Alert alert = new model.entities.Alert(
                "ALT001", "SENS101", 36.5, 10.0, 35.0, SeverityLevel.CRITICAL, LocalDateTime.now()
        );
        activeAlerts.add(alert);
        alertHistory.add(alert);

        zoneCounter = 2;
        sensorCounter = 103;
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static class SensorWrapper {
        private Sensor sensor;

        public SensorWrapper(Sensor s) { this.sensor = s; }

        public Sensor getSensor() { return sensor; }
        public String getCode() { return sensor.getCode(); }
        public String getName() {
            return sensor.getClass().getSimpleName() + "-" + sensor.getCode();
        }
        public String getZoneCode() { return sensor.getZoneCode(); }
        public String getType() { return sensor.getClass().getSimpleName().replace("Sensor", ""); }
        public String getStatus() { return sensor.getStatus().toString(); }
        public String getThreshold() { return String.format("[%.1f - %.1f] %s", sensor.getThresholdMin(), sensor.getThresholdMax(), sensor.getUnit()); }
        public String getLastReading() {
            if (sensor.getReadings().isEmpty()) return "No readings";
            Reading last = sensor.getReadings().get(sensor.getReadings().size() - 1);
            return last.getValue() + " " + last.getUnit() + " at " + last.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
    }
}