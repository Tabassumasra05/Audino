# Audino EMR: Object-Oriented Methodology (OOM) Concepts Traceability Map

**Document Version:** 1.0.0  
**Target Evaluation Date:** 19-Nov-2026  
**Evaluation Rubrics:** Object-Oriented Methodology (OOM) & Software Engineering (SE)  
**System Name:** Audino: Intelligent Clinical Decision Support & EMR System  

---

## 1. Executive Summary

Audino was architected from foundational object-oriented design principles to solve a high-stakes, real-world healthcare dilemma: **eliminating preventable Adverse Drug Events (ADEs)**. In clinical software, software defects or poorly structured code lead directly to patient morbidity. 

To satisfy the academic evaluation criteria for **Object-Oriented Methodology (OOM)**, every core design principle, relationship, pattern, and SOLID axiom is explicitly manifested in Java 17 code and tagged with canonical comments (`// OOM: ...`).

This document serves as the primary evaluation guide for course assessors, providing exact file references, class names, method signatures, and theoretical justifications.

---

## 2. Master OOM Traceability Matrix

| OOM Category | Concept Demonstrated | Concrete Implementation File(s) | Primary Methods / Demonstrations |
|---|---|---|---|
| **3.1 Encapsulation** | Field privacy & range invariants | `com.audino.model.PatientVitals` | `validateRanges(...)`, `calculateBmi()`, `getBmiCategory()` |
| **3.1 Encapsulation** | Defensive collection copying | `com.audino.model.Patient`, `Medication`, `Prescription` | `Collections.unmodifiableList(allergies)`, `getIngredients()`, `getItems()` |
| **3.1 Encapsulation** | Telephony & domain invariants | `com.audino.model.Patient` | `validatePhone(...)`, `validateBirthDate(...)` |
| **3.2 Abstraction & Inheritance** | User hierarchy with abstract hooks | `Account` $\leftarrow$ `Admin`, `Doctor`, `Receptionist` | `abstract String getDashboardFxml()`, `abstract Set<String> getPermissions()` |
| **3.2 Abstraction & Inheritance** | Pharmacology delivery hierarchy | `Medication` $\leftarrow$ `TabletMedication`, `CapsuleMedication`, `LiquidMedication`, `InjectionMedication` | `abstract boolean isValidDosage(String)`, `abstract String getUnitLabel()`, `abstract int calculateUnits(...)` |
| **3.3 Polymorphism** | Clinical Decision Support Strategy | `SafetyEngine`, `InteractionStrategy` | Polymorphic loop iterating `List<InteractionStrategy>` without knowing concrete subtypes |
| **3.3 Polymorphism** | Polymorphic GUI Dashboard Routing | `Account.getDashboardFxml()`, `AppShellController` | Dynamic dispatch routes to doctor, receptionist, or admin view without `switch(role)` chains |
| **3.3 Polymorphism** | Dynamic Prescription Costing | `Medication.calculateCost(...)`, `calculateUnits(...)` | Polymorphic calculation scaling with dosage syntax, frequency multipliers, and delivery form |
| **3.4 Relationships** | Association (loose linkage) | `Doctor` $\rightarrow$ `Patient` | Associated through `Token` and `Prescription.prescribedBy` |
| **3.4 Relationships** | Aggregation (independent lifecycle) | `Prescription` $\rightarrow$ `Medication` | Formulary medications exist independently of individual patient prescriptions |
| **3.4 Relationships** | Composition (strict lifecycle ownership) | `Prescription` $\diamond-\rightarrow$ `PrescriptionItem`, `PrescriptionAlert` | Items and alerts created only through parent; cascaded deletion (`ON DELETE CASCADE`) |
| **3.4 Relationships** | Composition (strict lifecycle ownership) | `Patient` $\diamond-\rightarrow$ `PatientAllergy`, `PatientCondition` | Clinical profile attributes bound strictly to parent patient lifecycle |
| **3.5 Design Patterns** | Strategy Pattern | `service.strategy.*` | `InteractionStrategy` with 4 interchangeable safety evaluators |
| **3.5 Design Patterns** | Observer Pattern | `observer.*`, `QueueService` | Live outpatient queue synchronization notifying decoupled UI panels |
| **3.5 Design Patterns** | Data Access Object (DAO) | `dao.*`, `dao.jdbc.*` | Complete decoupling of domain models and services from native JDBC SQL queries |
| **3.5 Design Patterns** | Factory Pattern | `factory.*` | `AccountFactory`, `MedicationFactory`, `AlertFactory` for encapsulated polymorphic instantiation |
| **3.5 Design Patterns** | Singleton Pattern | `util.ConfigurationManager`, `util.SessionManager`, `ui.navigation.*` | Controlled single-instance lifecycle for HikariCP pool, active session, and navigation |
| **3.6 SOLID Principles** | Single Responsibility (SRP) | `PrescriptionService`, `PrescriptionPdfService`, `PillScheduleService`, `AuditService`, `AuthService` | Cleanly separated concerns (orchestration vs PDF rendering vs pill schedule vs forensic audit) |
| **3.6 SOLID Principles** | Open/Closed (OCP) | `SafetyEngine`, `InteractionStrategy` | New rule engines (e.g., Renal, Pediatric) registered at runtime without modifying engine code |
| **3.6 SOLID Principles** | Liskov Substitution (LSP) | `Account`, `Medication` hierarchies | Any subclass substitute-able in collections and services without breaking contracts |
| **3.6 SOLID Principles** | Interface Segregation (ISP) | `dao.*`, `observer.*` | Granular, highly specialized interfaces (`TokenDAO`, `VitalsDAO`, `QueueObserver`) |
| **3.6 SOLID Principles** | Dependency Inversion (DIP) | `service.*`, `dao.*` | High-level business services depend on DAO interfaces, not concrete JDBC classes |
| **3.7 Exception Hierarchy** | Domain Checked/Unchecked Exceptions | `com.audino.exception.*` | Structured hierarchy rooted at `AudinoException` with ACID rollback triggers |

---

## 3. Detailed Concept Demonstrations

### 3.1 Encapsulation, State Guards, and Defensive Copying

Encapsulation ensures that domain entities maintain internal consistency and prevent corrupted clinical states.

#### Code Evidence:
- **`com.audino.model.PatientVitals`**:
  - `validateRanges(double height, double weight, int systolic, int diastolic, int pulse, double temp, int spo2)`: Enforces physiological bounds ($30 \le \text{height} \le 250\text{ cm}$, $2 \le \text{weight} \le 300\text{ kg}$, $50 \le \text{systolic} \le 260\text{ mmHg}$, $30 \le \text{diastolic} \le 180\text{ mmHg}$, $30 \le \text{pulse} \le 220\text{ bpm}$, $33.0 \le \text{temp} \le 43.0^\circ\text{C}$, $50 \le \text{SpO}_2 \le 100\%$).
  - Derived calculations: `calculateBmi()` computes $\text{BMI} = \text{weight} / (\text{height}/100)^2$ rounded to 1 decimal place; `getBmiCategory()` categorizes per WHO thresholds.
- **`com.audino.model.Patient`**:
  - `validatePhone(String)`: Validates Indian 10-digit mobile numbers with regex `^[6-9]\d{9}$`.
  - Defensive Copies:
    ```java
    // OOM: Encapsulation – returns unmodifiable defensive copy of allergy collection
    public List<PatientAllergy> getAllergies() {
        return Collections.unmodifiableList(allergies);
    }
    ```
- **`com.audino.model.Medication`**:
  - `getIngredients()` returns `Collections.unmodifiableList(ingredients)`.
- **`com.audino.model.Prescription`**:
  - `getItems()` and `getAlerts()` return unmodifiable lists. Items can only be manipulated via `addItem()` and `removeItem()`, which automatically invoke `recalculateTotal()`.

---

### 3.2 Abstraction & Inheritance Hierarchies

Two distinct, logically rigorous inheritance hierarchies are implemented:

#### Hierarchy A: User & Identity (`Account`)
```
                  ┌────────────────────────┐
                  │    abstract Account    │
                  ├────────────────────────┤
                  │ # id: Long             │
                  │ # username: String     │
                  │ # passwordHash: String │
                  │ # fullName: String     │
                  │ # role: Role           │
                  │ # active: boolean      │
                  ├────────────────────────┤
                  │ + getDashboardFxml()*  │
                  │ + getPermissions()*    │
                  └───────────▲────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
┌────────┴────────┐  ┌────────┴────────┐  ┌────────┴────────┐
│      Admin      │  │     Doctor      │  │  Receptionist   │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ + getDashboard()│  │ - specialization│  │ + getDashboard()│
│ + getPerms()    │  │ - regNumber     │  │ + getPerms()    │
└─────────────────┘  │ + getDashboard()│  └─────────────────┘
                     │ + getPerms()    │
                     └─────────────────┘
```
- **File:** `src/main/java/com/audino/model/Account.java`
- **Abstract Methods:**
  - `public abstract String getDashboardFxml();`
  - `public abstract Set<String> getPermissions();`
- **Subclasses:**
  - `Admin`: Returns `/fxml/admin_dashboard.fxml`, permissions: `ALL_ADMIN_ACCESS`.
  - `Doctor`: Adds `specialization`, `registrationNumber`. Returns `/fxml/doctor_consultation.fxml`, permissions: `PRESCRIBE_MEDICATION`, `VIEW_PATIENT_HISTORY`, etc.
  - `Receptionist`: Returns `/fxml/receptionist_dashboard.fxml`, permissions: `REGISTER_PATIENT`, `ISSUE_QUEUE_TOKEN`.

#### Hierarchy B: Pharmaceutical Delivery Systems (`Medication`)
```
                       ┌─────────────────────────┐
                       │   abstract Medication   │
                       ├─────────────────────────┤
                       │ # id: Long              │
                       │ # genericName: String   │
                       │ # brandName: String     │
                       │ # rxNormCode: String    │
                       │ # type: MedicationType  │
                       │ # strength: String      │
                       │ # pricePerUnit: BigDec  │
                       ├─────────────────────────┤
                       │ + isValidDosage()*      │
                       │ + getUnitLabel()*       │
                       │ + calculateUnits()*     │
                       │ + calculateCost()       │
                       └────────────▲────────────┘
                                    │
       ┌───────────────┬────────────┴───┬────────────────┐
┌──────┴──────┐ ┌──────┴──────┐ ┌───────┴──────┐ ┌───────┴──────┐
│TabletMedicat│ │CapsuleMedica│ │LiquidMedicat │ │InjectionMedic│
└─────────────┘ └─────────────┘ └──────────────┘ └──────────────┘
```
- **File:** `src/main/java/com/audino/model/Medication.java`
- **Abstract Methods:**
  - `public abstract boolean isValidDosage(String dosage);`
  - `public abstract String getUnitLabel();`
  - `public abstract int calculateUnits(String dosage, int timesPerDay, int days);`
- **Subclasses:**
  - `TabletMedication`: Accepts dosage like `"1 tablet"`, `"2 tabs"`, `"650mg"`. Calculates integer tablets.
  - `CapsuleMedication`: Accepts `"1 capsule"`, `"2 caps"`. Validates capsule delivery.
  - `LiquidMedication`: Accepts `"5ml"`, `"10 ml"`, `"1 tsp"`, `"1 tbsp"`. Converts milliliters into whole bottles (standard 100ml bottle ceiling) for inventory/costing.
  - `InjectionMedication`: Accepts `"1 vial"`, `"2 vials"`, `"1 ampoule"`, `"2ml"`. Calculates vials/ampoules required.

---

### 3.3 Dynamic Polymorphism

Polymorphic dispatch eliminates brittle conditional trees (`if/else` or `switch` chains), enabling seamless system extensibility.

#### Code Evidence:

1. **Safety Engine Evaluation Loop:**
   - **File:** `src/main/java/com/audino/service/strategy/SafetyEngine.java`
   ```java
   // OOM: Polymorphism – engine calls check() on every strategy without knowing its concrete type.
   for (InteractionStrategy strategy : strategies) {
       alerts.addAll(strategy.check(prescription, patient));
   }
   ```
   *Explanation:* `SafetyEngine` holds a `List<InteractionStrategy>` and invokes `.check(...)`. It neither knows nor cares whether the strategy is evaluating drug-drug interactions, allergy cross-reactivity, renal failure contraindications, or duplicate therapy.

2. **Polymorphic Dashboard Routing:**
   - **File:** `src/main/java/com/audino/controller/AppShellController.java`
   ```java
   // OOM: Polymorphism – routes to the dashboard defined by Account subclass without an 'if (role == ...)' chain.
   String initialFxml = account.getDashboardFxml();
   NavigationService.getInstance().navigateTo(initialFxml, initialTitle);
   ```
   *Explanation:* When a user authenticates, the application shell queries `account.getDashboardFxml()`. The JVM dispatches dynamically to `Doctor.getDashboardFxml()`, `Admin.getDashboardFxml()`, or `Receptionist.getDashboardFxml()`.

3. **Polymorphic Cost and Unit Calculations:**
   - **File:** `src/main/java/com/audino/model/Medication.java`
   - **File:** `src/main/java/com/audino/controller/DoctorConsultationController.java`
   ```java
   // OOM: Polymorphism – delegates calculateUnits and calculateCost directly to Medication subclass.
   int totalUnits = med.calculateUnits(dosage, freq.getTimesPerDay(), duration);
   BigDecimal lineCost = med.calculateCost(totalUnits);
   ```

---

### 3.4 Object Relationships

UML object relationships are precisely modelled in both Java code and the relational database schema:

```
+-------------------------------------------------------------------------------------------------+
|                                    UML OBJECT RELATIONSHIPS                                     |
+-------------------------------------------------------------------------------------------------+

 1. ASSOCIATION (Peer Linkage)
    [ Doctor ] --------------------------- [ Patient ]
                 consults (via Token & Prescription)

 2. AGGREGATION (Independent Lifecycle - Open Diamond)
    [ Prescription ] <>------------------- [ Medication ]
                       references formulary

 3. COMPOSITION (Strict Lifecycle Ownership - Filled Diamond - ON DELETE CASCADE)
    [ Prescription ] *==================== [ PrescriptionItem ]
                     *==================== [ PrescriptionAlert ]
    
    [ Patient ]      *==================== [ PatientAllergy ]
                     *==================== [ PatientCondition ]
+-------------------------------------------------------------------------------------------------+
```

#### Code Evidence:
- **Association:**
  - `Doctor` interacts with `Patient` through appointments and prescriptions. Neither owns the lifecycle of the other. Demonstrated in `Prescription.java` where `doctorUsername` associates the physician with the encounter.
- **Aggregation:**
  - `Prescription` aggregates `Medication`. A `Medication` resides in the hospital formulary database. When a prescription is cancelled or deleted, the underlying `Medication` remains intact.
- **Composition:**
  - `Prescription` **composes** `PrescriptionItem` and `PrescriptionAlert`:
    ```java
    // OOM: Composition – items are created and added only through prescription
    public void addItem(PrescriptionItem item) {
        Objects.requireNonNull(item, "Prescription item must not be null");
        item.setPrescriptionId(this.id);
        this.items.add(item);
        recalculateTotal();
    }
    ```
    In `schema.sql`:
    ```sql
    CONSTRAINT fk_pi_prescription FOREIGN KEY (prescription_id) 
        REFERENCES prescriptions (id) ON DELETE CASCADE
    ```
  - `Patient` **composes** `PatientAllergy` and `PatientCondition`:
    ```java
    // OOM: Composition – allergies are bound directly to patient lifecycle
    public void addAllergy(String allergen, String reaction, String severity) {
        this.allergies.add(new PatientAllergy(this.id, allergen, reaction, severity));
    }
    ```

---

### 3.5 Design Patterns in Practice

Audino implements 5 major Gang of Four (GoF) design patterns:

#### 1. Strategy Pattern (`service.strategy.*`)
- **Intent:** Define a family of clinical safety algorithms, encapsulate each one, and make them interchangeable at runtime.
- **Participants:**
  - `InteractionStrategy`: Strategy interface defining `List<PrescriptionAlert> check(Prescription, Patient)`.
  - `DrugDrugStrategy`: Screens multi-drug co-prescriptions against keyword interaction rules.
  - `DrugAllergyStrategy`: Screens prescribed ingredients against patient known allergens.
  - `DrugConditionStrategy`: Screens medications against chronic disease contraindications.
  - `DuplicateTherapyStrategy`: Detects redundant prescriptions sharing identical active ingredients.
  - `SafetyEngine`: Context maintaining active strategies and evaluating prescriptions.

#### 2. Observer Pattern (`observer.*`, `QueueService`)
- **Intent:** Define a one-to-many dependency between the live outpatient queue and clinic UI panels so that when queue state updates, all views are notified automatically.
- **Participants:**
  - `QueueSubject`: Subject contract (`registerObserver`, `removeObserver`, `notifyObservers`).
  - `QueueObserver`: Observer contract (`onQueueUpdated(List<Token>)`).
  - `QueueService`: Concrete subject maintaining thread-safe observers and running background cross-instance polling.
  - `DoctorConsultationController` & `ReceptionistController`: Concrete observers synchronizing UI tables via `Platform.runLater()`.

#### 3. Data Access Object (DAO) Pattern (`dao.*`, `dao.jdbc.*`)
- **Intent:** Abstract and encapsulate all access to PostgreSQL data sources, providing a clean separation between domain logic and persistence.
- **Interfaces:** `AccountDAO`, `PatientDAO`, `MedicationDAO`, `PrescriptionDAO`, `InteractionRuleDAO`, `TokenDAO`, `VitalsDAO`, `AuditLogDAO`.
- **Implementations:** Pure native JDBC with `PreparedStatement` and HikariCP connection pooling. Multi-table writes (e.g., `PrescriptionDAOJdbc`) use manual transactions with `conn.setAutoCommit(false)` and `conn.rollback()`.

#### 4. Factory Pattern (`factory.*`)
- **Intent:** Encapsulate complex object construction logic and instantiate the appropriate concrete subclass based on parameters or database records.
- **Participants:**
  - `AccountFactory`: Instantiates `Admin`, `Doctor`, or `Receptionist` based on `Role` or `ResultSet`.
  - `MedicationFactory`: Instantiates `TabletMedication`, `CapsuleMedication`, `LiquidMedication`, or `InjectionMedication` based on `MedicationType`.
  - `AlertFactory`: Constructs styled `PrescriptionAlert` instances with appropriate severity levels.

#### 5. Singleton Pattern
- **Intent:** Ensure a class has only one instance and provide a global point of access.
- **Implementations:**
  - `ConfigurationManager`: Thread-safe double-checked locking managing the single HikariCP connection pool.
  - `SessionManager`: Manages the currently logged-in authenticated user session.
  - `NavigationService`: Central router managing primary stage scenes and shell view swaps.
  - `ThemeManager`: Manages application-wide light/dark CSS stylesheets.

---

### 3.6 SOLID Principles Verification

| Principle | Demonstration in Audino | Concrete File Reference |
|---|---|---|
| **Single Responsibility Principle (SRP)** | Every service has one clearly defined business responsibility. Orchestration is decoupled from PDF generation, pill timeline formatting, and security auditing. | `PrescriptionService.java` (approval logic) <br> `PrescriptionPdfService.java` (iText PDF) <br> `PillScheduleService.java` (4-phase grid) <br> `AuditService.java` (forensic logging) |
| **Open/Closed Principle (OCP)** | The clinical safety engine is open for extension (new interaction strategies can be registered at runtime without restarting or editing `SafetyEngine`), but closed for modification. | `SafetyEngine.java` (`registerStrategy()`) <br> Tested in `SafetyEngineTest.testOpenClosedPrincipleCustomStrategy()` |
| **Liskov Substitution Principle (LSP)** | Subclasses (`Doctor`, `Admin`, `TabletMedication`, `LiquidMedication`) can be used anywhere their abstract base class is expected without breaking functionality or throwing unexpected exceptions. | `Account.java` & `Medication.java` <br> Tested in `MedicationDosageTest.java` |
| **Interface Segregation Principle (ISP)** | Clients should not be forced to depend on methods they do not use. DAOs and observers are split into focused, single-purpose interfaces rather than one monolithic repository. | `VitalsDAO.java`, `TokenDAO.java`, `QueueObserver.java` |
| **Dependency Inversion Principle (DIP)** | High-level business services (`PrescriptionService`, `AuthService`, `QueueService`) depend strictly on abstract DAO interfaces, injected via constructors, not concrete JDBC classes. | `PrescriptionService(PrescriptionDAO, SafetyEngine, ...)` |

---

### 3.7 Exception Handling & Transaction Integrity

Audino features a custom exception hierarchy rooted at `AudinoException`:

```
                       ┌─────────────────────────┐
                       │     AudinoException     │
                       └────────────▲────────────┘
                                    │
      ┌────────────────┬────────────┴────┬────────────────┬──────────────┐
┌─────┴─────┐   ┌──────┴──────┐   ┌──────┴──────┐   ┌─────┴─────┐  ┌────┴────┐
│Authenticat│   │AccountDisabl│   │CriticalInter│   │InvalidDosa│  │DataAcces│
│Exception  │   │Exception    │   │Exception    │   │geException│  │Exception│
└───────────┘   └─────────────┘   └─────────────┘   └───────────┘  └─────────┘
```

#### ACID Multi-Table Transactions:
In `PrescriptionDAOJdbc.java`, saving an approved prescription writes to `prescriptions`, `prescription_items`, and `prescription_alerts` within a single atomic database transaction:
```java
conn.setAutoCommit(false);
try {
    // 1. Insert prescription header
    // 2. Insert prescription items in batch
    // 3. Insert prescription alerts with override notes
    conn.commit();
} catch (SQLException e) {
    conn.rollback();
    throw new DataAccessException("Prescription transaction failed, rolled back", e);
} finally {
    conn.setAutoCommit(true);
}
```

---

## 4. Academic Evaluation Checklist

- [x] **Classes, Objects, Constructors, Encapsulation:** Demonstrated with range checks, private state, and defensive copies.
- [x] **Inheritance:** Two distinct hierarchies (`Account` and `Medication`).
- [x] **Polymorphism:** Demonstrated in Safety Engine, Dynamic Routing, and Cost Calculation.
- [x] **UML Relationships:** Association, Aggregation, and Composition clearly modelled in code and diagrams.
- [x] **Design Patterns:** Strategy, Observer, DAO, Factory, and Singleton implemented with zero external framework dependencies.
- [x] **SOLID:** All 5 principles satisfied with file citations and unit test verification.
- [x] **Exceptions & ACID Transactions:** Custom exception hierarchy and atomic multi-table writes.
