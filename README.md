# 🌾 Smart Farming System - Complete Documentation

## Table of Contents
1. [System Overview](#system-overview)
2. [Package Structure](#package-structure)
3. [Core Domain Models](#core-domain-models)
4. [Zone Management System](#zone-management-system)
5. [Animal Management System](#animal-management-system)
6. [Crop Management System](#crop-management-system)
7. [Sensor Network System](#sensor-network-system)
8. [Alert System](#alert-system)
9. [Production & Reporting](#production--reporting)
10. [Enumerations](#enumerations)
11. [Interfaces](#interfaces)
12. [JavaFX Application Layer](#javafx-application-layer)
13. [Key Design Patterns & OOP Principles](#key-design-patterns--oop-principles)
14. [Common Exam Questions & Answers](#common-exam-questions--answers)

---

## System Overview

The **Smart Farming System** is a comprehensive farm management application that integrates:
- **Zonal farming** with three distinct zone types
- **IoT sensor networks** for environmental monitoring
- **Animal health tracking** with biometric sensors
- **Crop lifecycle management** from sowing to harvest
- **Automated alert generation** for out-of-range readings
- **Production recording** for milk, eggs, harvest, and aquaculture yield

### Core Business Logic
The system follows a **hierarchical zone-based architecture** where each farm is divided into specialized zones, each containing its own entities (crops/animals) and sensors. Data flows from sensors → readings → alerts → manager actions, creating a complete monitoring and response loop.

---

## Package Structure

```
model/
├── animals/           # Animal hierarchy (Ruminant, Poultry)
├── crops/            # Crop management
├── entities/         # Shared entities (Position, Reading, Alert, ProductionRecord, FeedingProgram)
├── enums/            # All enumerations
├── interfaces/       # Producing interface
├── sensors/          # Sensor hierarchy (Environment, Soil, Biometric, Water, GPS)
└── zones/            # Zone hierarchy (CropZone, LivestockZone, AquacultureZone)
```

---

## Core Domain Models

### 1. Position (model.entities.Position)

**Purpose:** Represents geographical coordinates for GPS tracking and spatial operations.

```java
public class Position {
    private double latitude;   // -90 to 90 degrees
    private double longitude;  // -180 to 180 degrees
}
```

**Logic:**
- Used by GPS sensors to track animal locations
- Can be extended to calculate distances between positions
- Essential for boundary violation detection (animal leaving zone)

**Key Methods:**
- `getLatitude() / getLongitude()` - Access coordinates
- `setLatitude() / setLongitude()` - Update position

---

### 2. Reading (model.entities.Reading)

**Purpose:** Captures a single sensor measurement at a specific time.

```java
public class Reading {
    private String sensorCode;      // Which sensor took this reading
    private double value;           // The measured value
    private String unit;            // Measurement unit (°C, %, pH, etc.)
    private LocalDateTime timestamp;// When it was recorded
    private Position position;      // Optional GPS coordinates
}
```

**Logic:**
- Immutable after creation (historical record)
- Timestamp automatically set at creation
- Position is optional (only for GPS sensors)
- Each reading can trigger alert generation

**Why separate from Sensor?**  
Sensors have multiple readings over time. This creates a clean one-to-many relationship: one Sensor → many Readings.

---

### 3. FeedingProgram (model.entities.FeedingProgram)

**Purpose:** Standardizes feeding schedules across livestock and aquaculture zones.

```java
public class FeedingProgram {
    private String feedType;        // e.g., "Organic Hay Mix", "Protein Pellets"
    private double quantityPerMeal; // kg per feeding session
    private int mealsPerDay;        // Number of feeding sessions
}
```

**Logic:**
- `getDailyQuantity()` = quantityPerMeal × mealsPerDay
- Different zones can have different programs
- Helps calculate feed inventory requirements

---

### 4. ProductionRecord (model.entities.ProductionRecord)

**Purpose:** Tracks historical production data for analytics.

```java
public class ProductionRecord {
    private List<Double> productions;  // List of production amounts
    private String unit;               // "L" for milk, "eggs" for poultry, "kg" for crops
}
```

**Logic:**
- Stores multiple production entries over time
- Calculates statistics: total, average
- Enables trend analysis and forecasting

**Key Methods:**
- `addProduction(amount)` - Record new production
- `getTotalProduction()` - Sum of all productions
- `getAverageProduction()` - Mean production value

---

## Zone Management System

### Zone (Abstract Base Class)

**Purpose:** Defines the contract for all farm zones.

```java
public abstract class Zone {
    protected String code;           // Unique identifier (e.g., "CZ001")
    protected String name;           // Human-readable name
    protected ZoneStatus status;     // ACTIVE or SUSPENDED
    protected List<Sensor> sensors;  // All sensors in this zone
}
```

**Key Logic:**
- **Suspension cascade:** When a zone is suspended, ALL its sensors are automatically suspended
- **Activation cascade:** Reactivating a zone restores all sensors to active status
- Abstract method `getEntityCount()` forces subclasses to define what they count

**Important Methods:**
```java
public void suspend() {
    this.status = ZoneStatus.SUSPENDED;
    for (Sensor s : sensors) s.suspend();  // Cascade suspension
}

public void activate() {
    this.status = ZoneStatus.ACTIVE;
    for (Sensor s : sensors) s.activate(); // Cascade activation
}
```

**Why abstract?**  
Different zone types count different entities (crops vs. animals). Abstract method ensures each subclass provides its own count logic.

---

### CropZone (Concrete Zone)

**Purpose:** Manages agricultural crop fields.

```java
public class CropZone extends Zone {
    private List<Crop> crops;  // All crops planted in this zone
}
```

**Logic:**
- Each crop has its own lifecycle (planting → growth stages → harvest)
- Sensors monitor soil conditions (pH, moisture) and environment (temp, humidity)
- Production recorded as harvest weight (kg)

**Entity Count:** Returns number of crops

---

### LivestockZone (Concrete Zone)

**Purpose:** Manages animal farming operations.

```java
public class LivestockZone extends Zone {
    private List<Animal> animals;           // Ruminants + Poultry
    private FeedingProgram feedingProgram;  // Feeding schedule
}
```

**Logic:**
- Supports mixed animal types (both ruminants and poultry)
- Each animal individually tracked with health events
- Feeding program is zone-wide (all animals follow same schedule)
- Biometric sensors attached to individual animals

**Entity Count:** Returns number of animals

---

### AquacultureZone (Concrete Zone)

**Purpose:** Manages fish/shrimp farming in tanks.

```java
public class AquacultureZone extends Zone {
    private List<String> species;      // List of aquatic species
    private int animalCount;           // Total count (not individually tracked)
    private FeedingProgram feedingProgram;
}
```

**Logic:**
- Unlike livestock, animals are not individually tracked (too many fish)
- Population managed as a count, not individual objects
- Water sensors monitor temperature and dissolved oxygen

**Entity Count:** Returns animal count (int, not list size like other zones)

**Why different from LivestockZone?**  
Aquaculture often involves thousands of individuals - tracking each is impractical. System treats the tank as a collective.

---

## Animal Management System

### Animal (Abstract Base Class)

**Purpose:** Defines common animal properties and behavior.

```java
public abstract class Animal {
    protected String id;                  // Unique identifier (e.g., "R1001")
    protected String species;             // "Holstein Friesian", "Rhode Island Red"
    protected int age;                   // In years
    protected double weight;             // In kg
    protected HealthStatus healthStatus; // HEALTHY, SICK, QUARANTINED
    protected List<String> healthEvents; // Log of health changes
}
```

**Key Logic:**
- Health events log provides complete medical history
- Weight changes trigger automatic logging
- Health status can be manually updated by farm manager

**Important Methods:**
```java
public void setWeight(double w) {
    this.weight = w;
    logHealthEvent("Weight: " + w + " kg");  // Automatic logging
}

public void logHealthEvent(String e) {
    healthEvents.add(e);  // Preserve chronological history
}
```

**Why abstract?**  
`getAnimalType()` is abstract because each animal type identifies differently (RUMINANT vs POULTRY).

---

### Ruminant (Concrete Animal)

**Purpose:** Represents milk-producing animals (cows, sheep, goats).

```java
public class Ruminant extends Animal {
    private double milkYield;  // Total milk produced in liters
}
```

**Logic:**
- Implements `Producing` interface indirectly through production recording
- Milk yield accumulates over time
- Each milking session adds to total

**Production Logic:** `addMilkYield(liters)` → increments cumulative production

---

### Poultry (Concrete Animal)

**Purpose:** Represents egg-laying birds (chickens, turkeys).

```java
public class Poultry extends Animal {
    private int eggCount;  // Total eggs laid
}
```

**Logic:**
- Similar to ruminants but tracks eggs instead of milk
- Egg count accumulates over time
- Can be used to calculate laying rates (eggs/day)

---

## Crop Management System

### Crop (model.crops.Crop)

**Purpose:** Represents a planted crop with its lifecycle and requirements.

```java
public class Crop {
    private String name;                      // "Winter Wheat"
    private CropFamily family;                // CEREALS, VEGETABLES, FRUITS
    private LocalDate plantingDate;           // When planted
    private LocalDate expectedHarvestDate;    // Expected harvest time
    private GrowthStage growthStage;          // SOWING, GERMINATION, GROWTH, MATURITY, HARVEST
    private double optimalPHMin;              // Minimum acceptable pH
    private double optimalPHMax;              // Maximum acceptable pH
    private double optimalMoistureMin;        // Minimum soil moisture %
    private double optimalMoistureMax;        // Maximum soil moisture %
}
```

**Logic - Growth Stages Lifecycle:**

| Stage | Description | Typical Duration |
|-------|-------------|------------------|
| SOWING | Seeds planted | Day 0 |
| GERMINATION | Seeds sprout | 5-14 days |
| GROWTH | Vegetative development | 30-60 days |
| MATURITY | Ready for harvest | 15-30 days |
| HARVEST | Collection period | 5-14 days |

**Monitoring Logic:**
- Soil sensors compare readings against optimal ranges
- Alert triggered if pH or moisture outside optimal range
- Growth stage must be manually updated (or automated with AI in advanced systems)

**Key Methods:**
- `getOptimalPHMin()` / `getOptimalPHMax()` - Define acceptable pH range
- `setGrowthStage()` - Update as crop matures
- Getters for all requirements (used by alert system)

---

## Sensor Network System

### Sensor (Abstract Base Class)

**Purpose:** Defines common sensor behavior.

```java
public abstract class Sensor {
    protected String code;              // Unique identifier (e.g., "SENS101")
    protected String zoneCode;          // Which zone contains this sensor
    protected SensorStatus status;      // ACTIVE, FAULTY, SUSPENDED
    protected double thresholdMin;      // Minimum acceptable value
    protected double thresholdMax;      // Maximum acceptable value
    protected List<Reading> readings;   // Historical readings
}
```

**Critical Logic - Status Management:**
```java
public void addReading(Reading reading) {
    if (status == SensorStatus.ACTIVE) {  // Only active sensors record
        readings.add(reading);
    }
}
```

**Why check status?**  
Suspended/faulty sensors should not contribute data - prevents false alerts.

**Abstract Method:**
`getUnit()` - Each sensor type defines its own measurement unit

**Threshold Logic:**
- Normal: `thresholdMin ≤ value ≤ thresholdMax`
- Warning: Value within 30% outside thresholds
- Critical: Value >30% outside thresholds

---

### EnvironmentSensor (Concrete Sensor)

**Purpose:** Monitors atmospheric conditions.

```java
public class EnvironmentSensor extends Sensor {
    private String measurementType;  // "temperature", "humidity", "rainfall"
}
```

**Units:**
- Temperature: "°C"
- Humidity: "%"
- Rainfall: "mm"

**Typical Thresholds:**
- Temperature: 10°C - 35°C (crop growing range)
- Humidity: 30% - 80%
- Rainfall: 0mm - 50mm per day

---

### SoilSensor (Concrete Sensor)

**Purpose:** Monitors soil conditions.

```java
public class SoilSensor extends Sensor {
    private String measurementType;  // "ph", "moisture", "nitrogen"
}
```

**Units:**
- pH: "pH" (0-14 scale)
- Moisture: "%" (percentage of water content)
- Nitrogen: "mg/kg"

**Critical for Crop Health:**
- pH too low (acidic) or too high (alkaline) blocks nutrient absorption
- Moisture too low = drought stress
- Moisture too high = root rot risk

---

### BiometricSensor (Concrete Sensor)

**Purpose:** Monitors individual animal health.

```java
public class BiometricSensor extends Sensor {
    private String animalId;           // Which animal this sensor tracks
    private String measurementType;    // "temperature" or "activity"
}
```

**Units:**
- Temperature: "°C" (normal livestock: 38-39.5°C)
- Activity: "steps/min" (indicates health and mobility)

**Logic:**
- Attached to specific animal (one-to-one relationship)
- Elevated temperature may indicate illness
- Reduced activity may indicate injury or sickness

---

### GPSSensor (Concrete Sensor)

**Purpose:** Tracks animal location for boundary monitoring.

```java
public class GPSSensor extends Sensor {
    private String animalId;
    private Position lastPosition;  // Most recent location
}
```

**Unit:** "coordinates" (latitude/longitude pair)

**Critical Logic - Boundary Violation:**
- Last position stored separately from readings
- Can compare position against zone boundaries
- Should trigger alert if animal leaves designated area

---

### WaterSensor (Concrete Sensor)

**Purpose:** Monitors aquaculture tank conditions.

```java
public class WaterSensor extends Sensor {
    private String measurementType;  // "temperature" or "dissolved_oxygen"
}
```

**Units:**
- Temperature: "°C" (fish: 20-28°C typical)
- Dissolved Oxygen: "mg/L" (5-8 mg/L optimal)

**Critical for Fish Survival:**
- Low oxygen = fish suffocation risk
- Temperature outside range = stress, disease, death

---

## Alert System

### Alert (model.entities.Alert)

**Purpose:** Records and tracks abnormal conditions requiring attention.

```java
public class Alert {
    private String id;                  // Unique identifier (e.g., "ALT001")
    private String sensorCode;          // Which sensor triggered this
    private double readingValue;        // The problematic reading
    private double thresholdMin;        // What the minimum should be
    private double thresholdMax;        // What the maximum should be
    private SeverityLevel severity;     // WARNING or CRITICAL
    private LocalDateTime timestamp;    // When triggered
    private boolean acknowledged;       // Has manager seen it?
    private boolean dismissed;          // Has it been resolved?
}
```

**Alert Lifecycle:**
1. **Triggered** - Reading exceeds thresholds, alert created
2. **Displayed** - Shown in active alerts panel (red for critical)
3. **Acknowledged** - Manager views and accepts responsibility
4. **Dismissed** - Issue resolved, alert moved to history

**Severity Determination Logic:**
```java
// In main application:
if (value < thresholdMin || value > thresholdMax) {
    SeverityLevel severity = SeverityLevel.WARNING;
    if (value < thresholdMin * 0.7 || value > thresholdMax * 1.3) {
        severity = SeverityLevel.CRITICAL;  // 30% beyond threshold
    }
}
```

**Why both acknowledged and dismissed?**
- Acknowledged = seen, but maybe still occurring
- Dismissed = resolved, no longer relevant
- Two-stage workflow prevents losing track of ongoing issues

---

## Production & Reporting

### Producing Interface

**Purpose:** Standardizes production recording across entity types.

```java
public interface Producing {
    double getProduction();         // Get current total
    void recordProduction(double amount);  // Add new production
}
```

**Implementation Logic:**
- Ruminant implements with milkYield
- Poultry implements with eggCount
- CropZone could implement with harvest weight

---

## Enumerations

### AnimalType
```java
public enum AnimalType { RUMINANT, POULTRY }
```
**Usage:** Distinguishes animal categories without instanceof checks.

### CropFamily
```java
public enum CropFamily { CEREALS, VEGETABLES, FRUITS }
```
**Usage:** Crop classification for reporting and requirements.

### GrowthStage
```java
public enum GrowthStage { SOWING, GERMINATION, GROWTH, MATURITY, HARVEST }
```
**Usage:** Tracks crop progress through lifecycle (order matters).

### HealthStatus
```java
public enum HealthStatus { HEALTHY, SICK, QUARANTINED }
```
**Usage:** 
- HEALTHY = normal operations
- SICK = needs treatment
- QUARANTINED = isolated to prevent spread

### SensorStatus
```java
public enum SensorStatus { ACTIVE, FAULTY, SUSPENDED }
```
**Usage:**
- ACTIVE = normal operation, recording data
- FAULTY = malfunctioning, needs repair
- SUSPENDED = intentionally offline (maintenance)

### SeverityLevel
```java
public enum SeverityLevel { WARNING, CRITICAL }
```
**Usage:**
- WARNING = needs attention but not urgent
- CRITICAL = immediate action required

### ZoneStatus
```java
public enum ZoneStatus { ACTIVE, SUSPENDED }
```
**Usage:** Controls entire zone operation including all contained sensors.

---

## JavaFX Application Layer

### Main Class Structure

The Main class serves as the application controller, managing:
- **Data storage** - ObservableLists for zones, alerts, history
- **UI Components** - Sidebar navigation, content area, tables
- **Event Handlers** - Button clicks, dialog submissions
- **Business Logic** - Alert generation, sensor updates, production recording

### Key Data Structures
```java
private static ObservableList<CropZone> cropZones;
private static ObservableList<LivestockZone> livestockZones;
private static ObservableList<AquacultureZone> aquacultureZones;
private static ObservableList<Alert> activeAlerts;
private static ObservableList<Alert> alertHistory;
```

**Why ObservableList?**  
Automatically updates UI when data changes.

### Navigation Pattern
- Sidebar buttons call methods that replace contentArea content
- Each "showXxx()" method creates a new VBox with relevant components
- Centralized dialog creation for CRUD operations

### Alert Generation Flow
```
Sensor Reading → Check Thresholds → Create Alert → Add to activeAlerts → Display in Table → Manager Action → Move to history
```

---

## Key Design Patterns & OOP Principles

### 1. Inheritance Hierarchy
```
Zone (abstract)
├── CropZone
├── LivestockZone
└── AquacultureZone

Sensor (abstract)
├── EnvironmentSensor
├── SoilSensor
├── BiometricSensor
├── GPSSensor
└── WaterSensor

Animal (abstract)
├── Ruminant
└── Poultry
```

**Benefit:** Code reuse via inheritance, polymorphism via abstract methods.

### 2. Encapsulation
- Private fields with public getters/setters
- Health events automatically logged when weight changes
- Sensor status controls reading recording

### 3. Polymorphism
```java
// Zone objects can be treated uniformly
for (Zone zone : allZones) {
    System.out.println(zone.getEntityCount());  // Different for each zone type
}

// Sensor objects with different units
for (Sensor s : sensors) {
    System.out.println(s.getUnit());  // Each sensor type returns appropriate unit
}
```

### 4. Composition
- Zone contains Sensors (has-a relationship)
- LivestockZone contains Animals
- CropZone contains Crops

### 5. Observer Pattern (Implicit)
- Readings trigger alert creation
- Alerts notify manager via UI panel

### 6. Factory Pattern (Implicit in UI)
- Dialog forms create specific zone/sensor types
- Different buttons create different object types

This documentation covers ALL aspects of the system architecture, OOP principles, and component interactions. Study the relationships between classes (inheritance, composition, dependency) and the runtime behavior (alert generation, cascade suspension, production recording) - these are the most common exam topics!
