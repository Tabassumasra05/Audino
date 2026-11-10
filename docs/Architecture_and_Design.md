# System Architecture & Design Specification
## Audino: Intelligent Clinical Decision Support & EMR System

**Document Version:** 1.0.0  
**Phase:** Requirements & Design Baseline (Phase 1)  
**Target Academic Evaluation:** 19 November 2026  
**Courses:** Object-Oriented Methodology (OOM) & Software Engineering (SE)

---

## 1. Architectural Style & Layered Architecture

Audino is engineered using a strict **Layered (N-Tier) Architectural Style** combined with **Domain-Driven Object-Oriented Design Principles**. The system enforces unidirectional dependency flow:

$$\text{View (JavaFX FXML + Looked-Up CSS)} \longrightarrow \text{Controller Layer} \longrightarrow \text{Service / Business Logic Layer} \longrightarrow \text{DAO Data Access Layer} \longrightarrow \text{Relational Database (PostgreSQL)}$$

### Layer Invariants and Separation of Concerns:
1. **Presentation / View Layer (`com.audino.controller`, `com.audino.ui.components`):**
   - Declared declaratively in FXML and styled with looked-up CSS design tokens.
   - Controllers remain **thin**: they handle UI events, orchestrate input formatting, invoke services, and update the view model.
   - **Zero JDBC Rule:** Controllers never reference `java.sql.*`, execute SQL statements, or import DAO classes directly.
2. **Service / Business Logic Layer (`com.audino.service`, `com.audino.service.strategy`, `com.audino.observer`):**
   - Encapsulates domain workflows: authentication, clinical safety validation, queue transitions, pill schedule synthesis, and PDF document generation.
   - Services depend strictly on **DAO Interfaces** injected via constructors (Dependency Inversion Principle - DIP).
   - Manages ACID transaction boundaries for multi-table persistence.
3. **Data Access Object (DAO) Layer (`com.audino.dao`, `com.audino.dao.jdbc`):**
   - Isolates SQL dialect, `PreparedStatement` lifecycle, and `ResultSet` mapping from higher layers.
   - Converts raw relational tuples into rich domain model entities using domain factories.
   - Translates database-specific `SQLException` into domain `DataAccessException` wrappers.
4. **Domain Model (`com.audino.model`):**
   - Rich domain entities exhibiting complete encapsulation, defensive copies, invariant enforcement, and polymorphic behaviors.

```
+-----------------------------------------------------------------------------------+
|                            JavaFX Presentation Layer                              |
|   FXML Layouts | Custom Looked-Up CSS (light.css/dark.css) | ui.components        |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                                Controller Layer                                   |
|   LoginController, AdminController, DoctorConsultationController, KioskController |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                        Service & Business Engine Layer                            |
|   AuthService | SafetyEngine | QueueService | PrescriptionService | AuditService  |
|          |                 |                                                      |
|          |       +---------+----------+                                           |
|          |       | InteractionStrategy|                                           |
|          |       +--------------------+                                           |
|          |       | DrugDrugStrategy   |                                           |
|          |       | DrugAllergyStrategy|                                           |
|          |       +--------------------+                                           |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                             DAO Abstraction Layer                                 |
|   AccountDAO | PatientDAO | MedicationDAO | PrescriptionDAO | TokenDAO (Interfaces)|
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                           JDBC Implementation Layer                               |
|   AccountDAOJdbc, PatientDAOJdbc, MedicationDAOJdbc, PrescriptionDAOJdbc...       |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                     Infrastructure & Connection Pooling                           |
|   HikariCP DataSource Pool | ConfigurationManager | PostgreSQL 15 / Embedded      |
+-----------------------------------------------------------------------------------+
```

---

## 2. Package Decomposition & Responsibilities

| Package | Purpose & Responsibilities | Key Classes & Interfaces |
|---|---|---|
| `com.audino` | Application bootstrapping, main method execution. | `Launcher`, `AudinoApplication` |
| `com.audino.model` | Domain entities, value objects, and domain enums. | `Account`, `Admin`, `Doctor`, `Receptionist`, `Medication`, `TabletMedication`, `Patient`, `Token`, `Prescription`, `PrescriptionItem`, `PrescriptionAlert` |
| `com.audino.dao` | Persistence contracts and abstractions. | `AccountDAO`, `PatientDAO`, `MedicationDAO`, `PrescriptionDAO`, `TokenDAO`, `InteractionRuleDAO`, `VitalsDAO`, `AuditLogDAO` |
| `com.audino.dao.jdbc` | Concrete JDBC implementations with parameterized SQL. | `AccountDAOJdbc`, `PatientDAOJdbc`, `MedicationDAOJdbc`, `PrescriptionDAOJdbc`, `TokenDAOJdbc`, `InteractionRuleDAOJdbc` |
| `com.audino.service` | Clinical workflows, authorization, document generation. | `AuthService`, `PrescriptionService`, `MedicationSearchService`, `PillScheduleService`, `PrescriptionPdfService`, `AuditService` |
| `com.audino.service.strategy` | Pluggable clinical rule evaluation algorithms. | `InteractionStrategy`, `DrugDrugStrategy`, `DrugAllergyStrategy`, `DrugConditionStrategy`, `DuplicateTherapyStrategy`, `SafetyEngine` |
| `com.audino.observer` | Real-time queue event subscription & notification. | `QueueSubject`, `QueueObserver`, `QueueService` |
| `com.audino.factory` | Creation logic for polymorphic entities and badges. | `AccountFactory`, `MedicationFactory`, `AlertFactory` |
| `com.audino.exception` | Strongly-typed business and persistence exceptions. | `AudinoException`, `AuthenticationException`, `AccountDisabledException`, `CriticalInteractionException`, `InvalidDosageException`, `InvalidTokenStateException`, `ValidationException`, `DataAccessException` |
| `com.audino.controller` | UI mediation, action handling, async task wiring. | `LoginController`, `AdminController`, `DoctorController`, `ReceptionistController`, `KioskController`, `ChangePasswordController` |
| `com.audino.ui.components` | Reusable encapsulated JavaFX custom controls. | `StatCard`, `Chip`, `StatusChip`, `AlertCard`, `PatientBanner`, `EmptyState`, `Toast`, `ToastManager`, `PageHeader`, `SearchField`, `TagInput`, `Stepper`, `ConfirmDialog`, `Avatar`, `Sparkline`, `NumericKeypad` |
| `com.audino.ui.navigation` | Scene navigation, transitions, and theme management. | `NavigationService`, `ThemeManager` |
| `com.audino.util` | System singletons, database bootstrap, seeding, math. | `ConfigurationManager`, `SessionManager`, `DatabaseInitializer`, `DataSeeder`, `ValidationUtils` |

---

## 3. Architecture Decision Records (ADRs)

### ADR-01: Desktop JavaFX Application vs Web Architecture
- **Status:** Approved
- **Context:** The system serves hospital outpatient consultation rooms, reception desks, and waiting room kiosks. Reliability during internet outages, zero latency in physical peripheral access (local receipt/prescription printing), and instantaneous UI responsiveness are paramount.
- **Decision:** Build Audino as a native JavaFX 19 desktop client packaged as an executable Fat JAR.
- **Alternatives Considered:**
  1. *Web Application (Spring Boot + React/Angular):* Introduces complex deployment overhead (reverse proxies, web servers, Node.js runtime), network latency, browser cross-compatibility quirks, and inability to run offline without complex service-worker caching.
  2. *Electron (Node.js + Chromium):* Excessive memory footprint ($> 400$ MB idle), sluggish startup on low-spec clinic hardware, and weak fit for Object-Oriented academic evaluation.
- **Consequences:** Near-instantaneous response times ($< 16$ ms render frame); native hardware access; runs reliably in zero-internet rural and outpatient clinics; perfectly demonstrates pure Java 17 and JavaFX object-oriented GUI architecture.

---

### ADR-02: PostgreSQL RDBMS with Native JDBC/DAO vs JPA/Hibernate ORM
- **Status:** Approved
- **Context:** The clinical system mandates absolute control over SQL query generation, multi-table atomic transactions, and zero hidden N+1 query performance degradation.
- **Decision:** Use PostgreSQL as the relational storage engine coupled with native JDBC using the Data Access Object (DAO) pattern.
- **Alternatives Considered:**
  1. *JPA / Hibernate ORM:* Heavyweight dependency footprint; complex session management; prone to lazy loading exceptions in detached desktop threads; obscures raw SQL execution required for academic Software Engineering review.
  2. *SQLite / Flat Files (JSON/XML):* Lacks concurrent network writes, row-level locking, rich SQL constraint engines, and multi-user network scalability across reception and consultation rooms.
- **Consequences:** Total transparency in SQL statements; 100% parameterization via `PreparedStatement` eliminating SQL injection; lightweight binary footprint; predictable transactional boundaries (`connection.setAutoCommit(false)`).

---

### ADR-03: Zero-Setup Embedded PostgreSQL (Zonky) Fallback Architecture
- **Status:** Approved
- **Context:** Ground Rule 4 demands zero-setup execution: an academic evaluator or teammate must clone the repository and run `mvn clean test` or `mvn clean javafx:run` with only JDK 17 and Maven, without manually installing or configuring a PostgreSQL server.
- **Decision:** Implement an intelligent connection broker in `ConfigurationManager`. The system probes `localhost:5432` for an active PostgreSQL daemon. If present, it connects to it; if absent, it transparently spins up an embedded PostgreSQL process (`io.zonky.test:embedded-postgres`) with data persisted in `./data/pg-data`.
- **Alternatives Considered:**
  1. *H2 Database:* While lightweight, H2 features different SQL dialects, lack of robust PostgreSQL array/JSON syntax, differing constraint behaviors, and breaks production parity.
  2. *Mandatory External PostgreSQL:* Violates the zero-setup requirement and causes test failures on machines lacking pre-installed database engines.
- **Consequences:** Guarantees 100% out-of-the-box success across Windows, Linux, and macOS environments while maintaining pure PostgreSQL syntax and ACID compliance in both local and test runs.

---

### ADR-04: Strategy Pattern for Clinical Decision Support Engine
- **Status:** Approved
- **Context:** Drug interaction safety rules span distinct clinical categories (drug-drug, drug-allergy, drug-condition, duplicate therapy). New clinical guidelines must be pluggable without modifying the core orchestrator.
- **Decision:** Implement the GoF **Strategy Pattern**. Define `InteractionStrategy` with a uniform `check()` signature. `SafetyEngine` maintains a collection of strategies and evaluates prescriptions polymorphically.
- **Alternatives Considered:**
  1. *Monolithic Method with Nested Conditionals:* Violates the Open/Closed Principle (OCP); leads to unmaintainable, error-prone branching code.
  2. *Rule Engine Framework (e.g., Drools):* Adds enormous runtime overhead, external DSL learning curves, and violates the constraint against external heavyweight frameworks.
- **Consequences:** Fully satisfies OOM rubric (Open-Closed Principle and Polymorphism); enables independent unit testing of each clinical rule type; permits hot runtime injection of novel strategies.

---

### ADR-05: Observer Pattern with Background Polling for Multi-Instance Queue Synchronization
- **Status:** Approved
- **Context:** In a physical clinic, the Receptionist and Doctor run Audino on separate desktop computers connected over a local network. When the receptionist issues token `T-014`, the doctor's queue must update in real time.
- **Decision:** Implement the **Observer Pattern** (`QueueSubject` / `QueueObserver`) within the application, augmented by a scheduled background daemon (`ScheduledService`) polling the database every 3 seconds. When a change in the active queue snapshot is detected, `QueueService` broadcasts to all observers, which dispatch UI updates to `Platform.runLater()`.
- **Alternatives Considered:**
  1. *Pure In-Memory Observer:* Only works if Doctor and Receptionist share the same JVM instance; fails in multi-machine clinic deployments.
  2. *WebSocket / Message Broker (RabbitMQ/ActiveMQ):* Requires external infrastructure, ports, and complex server processes, violating the zero-setup constraint.
  3. *PostgreSQL LISTEN/NOTIFY:* Requires continuous dedicated socket connections, which can be brittle under intermittent network drops or with embedded database engines.
- **Consequences:** Operates seamlessly both in single-instance demo mode and across distributed multi-workstation clinic environments with zero additional network configuration.

---

### ADR-06: Cryptographic Security using jBCrypt (Cost Factor 12)
- **Status:** Approved
- **Context:** EMR systems store sensitive medical personnel and patient data. Plaintext or weak hashing (MD5, SHA-1, SHA-256) is vulnerable to dictionary and rainbow-table attacks.
- **Decision:** Adopt the OpenBSD Blowfish password hashing algorithm via `jBCrypt` with an adaptive computational work factor (cost) of 12.
- **Alternatives Considered:**
  1. *PBKDF2 / Argon2:* Argon2 requires native platform binaries; jBCrypt is 100% pure Java, highly portable, and industry-standard.
  2. *Plain SHA-256:* Extremely fast on GPUs; easily cracked without high per-hash computation times.
- **Consequences:** Immune to rainbow table attacks due to embedded random 128-bit salts; cost factor 12 provides a deliberate ~250 ms calculation overhead per authentication, thwarting brute-force attempts while staying well within the 1-second login requirement.

---

### ADR-07: Mandatory Strict Clinical Override Justification Protocol
- **Status:** Approved
- **Context:** Preventable ADEs often occur because clinicians blindly dismiss alert dialogs ("alert fatigue"). However, in clinical practice, doctors sometimes intentionally prescribe interacting drugs (e.g., Warfarin + Aspirin for prosthetic heart valves) under strict clinical monitoring.
- **Decision:** Enforce a hard block on approving any prescription that triggers a `CRITICAL` alert until an explicit clinical override is performed. The override action opens a dedicated modal dialog requiring a clinical justification note of at least 15 non-whitespace characters. The override note, timestamp, and physician ID are permanently saved and written to the audit log.
- **Alternatives Considered:**
  1. *Passive Warning Dialog:* Easily clicked through without reading; high clinical risk.
  2. *Absolute Hard Block (No Override Permitted):* Unrealistic in healthcare; prevents valid clinical exceptions and forces doctors to abandon the system.
- **Consequences:** Eliminates accidental critical prescriptions; establishes an unalterable medical-legal audit trail; satisfies healthcare accreditation and clinical governance requirements.

---

### ADR-08: Doctor Portal Layout Architecture (Justification)
- **Status:** Approved
- **Context:** Section 6 grants freedom on layouts while mandating speed, patient safety, and optimal visibility on a 1366×768 screen at 125% scaling.
- **Decision:** Employ a **Dynamic Dual-Zone Consultation Workspace** comprising:
  1. **Top Sticky Patient Banner:** Name, Patient ID, Age/Gender, Token No., and prominent, high-contrast chips for confirmed allergies (Red `#DC2626`) and chronic conditions (Amber `#D97706`). This banner is pinned permanently above all tabs so critical patient contraindications never scroll out of view.
  2. **Collapsible Left Queue Drawer:** Displays waiting tokens with live status chips and wait timers. Can be collapsed to a compact icon bar to give full screen width to prescribing on narrow displays.
  3. **Tabbed Primary Workspace:**
     - **Tab 1: Overview:** Displays latest visit vitals with visual color indicators, BMI calculation, and sparklines showing physiological trends over past visits.
     - **Tab 2: Prescribe (Composer):** Keyboard-first medication authoring pad with live auto-complete, polymorphic dosage validator, duration, frequency, and running cost total in INR (₹).
     - **Tab 3: History:** Longitudinal record of previous prescriptions issued to this patient.
  4. **Docked Right-Hand Safety Radar:** Stays visible during prescribing. Renders real-time count badges and interactive alert cards. If a CRITICAL alert occurs, a red blocking banner spans the top of the composer, disabling the save button until overridden or resolved.
  5. **Sticky Bottom Action Bar:** Displays running prescription total (₹), item count, "Save Draft", "Approve & Save (Ctrl+S)", "Download PDF", and "Complete Consultation".
- **Rationale & Ergonomic Justification:**
  - *Prevents Cognitive Overload:* Instead of cramming three rigid columns into a $1366 \times 768$ display (which results in horizontal truncation), the collapsible drawer and docked safety radar provide spacious, legible input zones.
  - *Safety by Design:* Allergies and active alerts are impossible to miss because they occupy permanent anchor zones.
  - *Speed for Clinicians:* Doctors can type, select, tab through frequencies, and add items using keyboard shortcuts without touching the mouse.

---

## 4. Clinical Decision Support System (CDSS) Architecture

### 4.1 Search & Autocorrect Algorithmic Architecture
Medication selection speed is a primary determinant of physician adoption. Audino implements a two-stage hybrid search engine in `MedicationSearchService`:
1. **Multi-Token Prefix & Substring Matcher:**
   - Indexes all generic names, brand names, and RxNorm codes in memory upon startup.
   - Tokenizes input queries and performs case-insensitive prefix and substring intersections in $O(N)$ time, delivering matching results in $< 5$ milliseconds.
2. **Levenshtein Distance & Trigram Cosine Similarity Autocorrect:**
   - If an exact or prefix match yields zero results (e.g., doctor types "amoxcilin" or "paracetmol"), the engine triggers the typo autocorrect pipeline.
   - Computes standard Levenshtein edit distance ($D \le 2$) normalized by word length.
   - Calculates character-trigram cosine similarity:
     $$\text{Similarity}(S_1, S_2) = \frac{|\text{tri}(S_1) \cap \text{tri}(S_2)|}{\sqrt{|\text{tri}(S_1)| \times |\text{tri}(S_2)|}}$$
   - If similarity exceeds threshold $\theta = 0.65$, the system suggests: *"Did you mean [Correct Drug Name]?"*.

### 4.2 Pluggable Safety Evaluation Pipeline
The `SafetyEngine` operates as an open, composable pipeline:
```
Prescription + Patient Context
          |
          v
+--------------------+
|    SafetyEngine    |
+--------------------+
          |
          +---> [1] DrugDrugStrategy --------> Checks drug pair interactions against interaction_rules
          |
          +---> [2] DrugAllergyStrategy -----> Checks drug ingredients against patient allergy list
          |
          +---> [3] DrugConditionStrategy ---> Checks drug contraindications against chronic conditions
          |
          +---> [4] DuplicateTherapyStrategy -> Checks for redundant overlapping therapeutic classes
          |
          v
Aggregated List<PrescriptionAlert>
          |
          v
AlertFactory (Creates UI AlertCards) ---> Displayed on Safety Radar & Blocking Banner
```

---

## 5. Concurrency, Threading, and JavaFX UI Safety

JavaFX enforces a single-threaded GUI model: any modification to the scene graph outside the **JavaFX Application Thread** throws an `IllegalStateException`. Conversely, executing blocking database queries or heavy computations on the JavaFX thread freezes the UI.

Audino establishes strict concurrency boundaries:
1. **Background Execution via `Task<T>` and `ExecutorService`:**
   - Database operations (loading patient history, fetching audit logs, compiling PDFs) are offloaded to background threads using JavaFX `Task<T>`.
   - UI controls display indeterminate progress indicators or skeleton states during execution.
2. **Safe UI Dispatch via `Platform.runLater()`:**
   - Background completion handlers dispatch state mutations back to the scene graph using `Platform.runLater(() -> { ... })`.
   - The queue observer and polling daemon run on a background daemon thread, guaranteeing that UI tables update smoothly without hitching the animation clock.
3. **Database Connection Thread-Safety:**
   - HikariCP manages an isolated connection pool (`maximumPoolSize = 10`).
   - Every background task acquires and closes its own `Connection` instance within a standard `try-with-resources` block, preventing connection leakage and thread contention.

---

## 6. Design for Testability and Maintainability

1. **Dependency Injection via Constructors:**
   - All services receive their DAO interfaces through public constructors.
   - Unit tests can mock DAOs using standard mocking techniques or lightweight stubs without requiring a live database.
2. **Integration Testing with Isolated Embedded Postgres:**
   - Integration tests (`*IT.java`) spin up an isolated embedded PostgreSQL instance in a **temporary directory** (`@TempDir`), apply `database/schema.sql`, execute transactions, and verify rollback semantics without polluting production `./data/pg-data`.
3. **Automated JaCoCo Coverage:**
   - The Maven build is wired with `jacoco-maven-plugin` to verify line and branch coverage across core business logic packages.
