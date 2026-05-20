# Smart Farming System - Complete Technical Documentation

## How the Java Application Works

### Application Lifecycle

```
Start → Load Data from Files → Start Periodic Checker → Show UI → User Interaction → Save Data → Exit
```

---

## 1. Main.java - The Heart of the Application

### Application Class Structure
```java
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) { ... }
    public static void main(String[] args) { launch(args); }
}
```

- `Application` is JavaFX's main class
- `launch()` starts the JavaFX runtime
- `start()` is called automatically - this is where UI is built

### Data Storage (ObservableList)
```java
private static ObservableList<CropZone> cropZones = FXCollections.observableArrayList();
```

**Why ObservableList?** Automatically updates UI when data changes. No need to manually refresh tables.

### The Main Flow

**Step 1: Load Data**
```java
loadDataFromFile() → DataManager.loadAllData() → Reads 4 text files → Populates ObservableLists
```

**Step 2: Start Background Timer**
```java
startPeriodicReadingCheck() → Timer runs every 30 seconds → checkReadingsFile()
```

**Step 3: Build UI**
```java
createSidebar() → Creates navigation buttons
showDashboard() → Shows statistics cards and recent alerts
```

**Step 4: User Interaction**
- Click buttons → Show dialogs → Modify data → Save to files

---

## 2. JavaFX Explained

### What is JavaFX?
JavaFX is a framework for building desktop applications with graphical user interfaces.

### Key JavaFX Components Used:

| Component | Purpose | In Our App |
|-----------|---------|-------------|
| `Stage` | Main window | `primaryStage` - the application window |
| `Scene` | Container for UI | Holds all UI elements |
| `BorderPane` | Layout manager | Left=sidebar, Center=content |
| `VBox` | Vertical box layout | Sidebar buttons arranged vertically |
| `StackPane` | Stacked layout | Main content area |
| `TableView` | Table display | Shows zones, sensors, alerts |
| `TableColumn` | Column in table | Maps to object properties |
| `Dialog` | Popup window | Create/edit forms |
| `Button` | Clickable element | Navigation and actions |
| `Label` | Text display | Titles and labels |
| `TextField` | Text input | Forms |
| `ComboBox` | Dropdown menu | Selecting options |
| `DatePicker` | Date selection | Planting/harvest dates |
| `TabPane` | Tabbed interface | Active alerts vs history |
| `Timer` | Background task | Periodic file checking |

### JavaFX CSS Styling
```java
btn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-background-radius: 5;");
```

Properties:
- `-fx-background-color`: Background color (hex or named)
- `-fx-text-fill`: Text color
- `-fx-background-radius`: Round corners
- `-fx-cursor: hand`: Hand cursor on hover

### Event Handling
```java
btn.setOnAction(e -> {
    // This code runs when button is clicked
    showDashboard();
});
```

---

## 3. Data Flow in Detail

### Loading Data (Startup)
```
1. Main.loadDataFromFile()
2. DataManager.loadAllData()
3. CropDataManager.load()     → Reads crop_zones.txt
4. LivestockDataManager.load() → Reads livestock_zones.txt
5. AquacultureDataManager.load() → Reads aquaculture_zones.txt
6. AlertDataManager.load()     → Reads alert_history.txt
7. Populate ObservableLists
8. Update UI with loaded data
```

### Saving Data (On Change)
```
1. User creates/edits/deletes something
2. saveAllData() called
3. DataManager.saveAllData()
4. CropDataManager.save()     → Writes to crop_zones.txt
5. LivestockDataManager.save() → Writes to livestock_zones.txt
6. AquacultureDataManager.save() → Writes to aquaculture_zones.txt
7. AlertDataManager.save()     → Writes to alert_history.txt
```

### Processing Sensor Readings (Every 30 seconds)
```
1. Timer triggers
2. checkReadingsFile() called
3. DataManager.processReadingsFile()
4. SensorReadingsProcessor.process()
5. Read sensor_readings.txt line by line
6. For each line: find sensor by code
7. Add reading to sensor's history
8. Compare value with threshold (min/max)
9. If out of range → create Alert object
10. Add to activeAlerts and alertHistory
11. Archive processed line to data/archive/
12. Remove from sensor_readings.txt
13. Show warning dialog in UI
```

---

## 4. Class Responsibilities

| Class | Responsibility |
|-------|----------------|
| `Main` | UI, user interaction, event handling |
| `DataManager` | Coordinates all data operations |
| `CropDataManager` | Reads/writes crop_zones.txt |
| `LivestockDataManager` | Reads/writes livestock_zones.txt |
| `AquacultureDataManager` | Reads/writes aquaculture_zones.txt |
| `AlertDataManager` | Reads/writes alert_history.txt |
| `SensorReadingsProcessor` | Processes incoming readings |
| `SensorSerializer` | Converts sensors to/from text format |
| `Zone` | Base class for all zones |
| `Sensor` | Base class for all sensors |
| `Alert` | Alert object with status |

---


## 5. Quick Debug Commands

| What to Check | Where |
|---------------|-------|
| Are zones loaded? | Sidebar shows zone count |
| Are sensors loaded? | Sensors page shows list |
| Are alerts appearing? | Dashboard shows alert count |
| Is timer running? | Console prints "Checking for new readings" |
| Are readings processed? | Check `data/archive/` folder |
| Is data saving? | Check file modification times |

---

## 6. Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| No alerts | Sensor not ACTIVE | Edit sensor → Set status to ACTIVE |
| Readings not processed | Wrong timestamp format | Use `YYYY-MM-DDThh:mm:ss` |
| File not found | Wrong path | Put `data` folder in project root |
| Sensors missing | Zone not created first | Create zone before adding sensor |
| Alert won't dismiss | Already in history | Refresh Alerts page |