# Software Requirements Specification (SRS)
## Audino: Intelligent Clinical Decision Support & EMR System

**Document Version:** 1.0.0  
**Status:** Approved for Implementation (Phase 1 Baseline)  
**Standard Compliance:** IEEE 830-1998 Recommended Practice for Software Requirements Specifications  
**Evaluation Target Date:** 19 November 2026  
**Academic Modules:** Object-Oriented Methodology (OOM) & Software Engineering (SE)

---

## 1. Introduction

### 1.1 Purpose
This document specifies the software requirements for **Audino**, an intelligent, desktop-based Clinical Decision Support (CDS) and Electronic Medical Record (EMR) system. Audino is engineered to solve a pervasive and life-threatening problem in outpatient clinics and hospitals: **preventable Adverse Drug Events (ADEs)**, dangerous drug-drug interactions, contraindicated drug-condition prescriptions, and adverse drug-allergy events. The document outlines the functional capabilities, system constraints, user characteristics, and non-functional requirements governing the implementation and academic evaluation of the software.

### 1.2 Document Conventions
- Requirements are prioritized using MoSCoW notation (Must have, Should have, Could have, Won't have for this release).
- Identifiers:
  - Functional Requirements: `FR-[MODULE]-[NUMBER]`, e.g., `FR-AUTH-01`, `FR-SAFE-01`.
  - Non-Functional Requirements: `NFR-[CATEGORY]-[NUMBER]`, e.g., `NFR-PERF-01`, `NFR-SEC-01`.
- Typography: Code identifiers, database tables, and system components are formatted in monospace font (e.g., `PrescriptionService`, `tokens`).

### 1.3 Intended Audience and Reading Suggestions
- **Academic Evaluators & Professors:** Focus on Section 2 (Product Perspective), Section 3 (System Features demonstrating OOM & SE rubrics), and Section 5 (Non-Functional Requirements).
- **Software Engineers & Peer Developers:** Focus on Section 3 (Functional Specifications), Section 4 (Interface Requirements), and Section 5 (Architectural Constraints).
- **Clinical & Administrative Stakeholders:** Focus on Section 2.3 (User Classes) and Section 3 (Clinical Workflow and Safety Enforcement).

### 1.4 Project Scope
Audino provides an integrated, offline-capable, high-reliability clinical workspace for outpatient polyclinics and secondary healthcare facilities. It encompasses:
1. **Clinical Decision Support System (CDSS):** Real-time safety engine that evaluates prescribed drug combinations against known drug-drug interactions, patient allergy records, chronic conditions, and duplicate therapeutic classes using pluggable strategies.
2. **Prescription Authoring & Verification:** Keyboard-first medication composer with typo-tolerant search, polymorphic dosage validation, dynamic pricing in Indian Rupees (₹), and structured iText PDF generation.
3. **Outpatient Queue & Triage Management:** Live token workflow (`WAITING → IN_CONSULTATION → COMPLETED`) synchronized via the Observer pattern and background polling across distributed clinic instances.
4. **Patient Self-Service Kiosk:** A secure, privacy-preserving terminal allowing patients to verify identity using token number and personal credentials, view their structured 4-phase daily pill schedule, and print/download prescriptions.
5. **System Governance & Audit:** Complete staff role-based access control (RBAC), formulary curation, dynamic clinical rule editing, and an immutable audit trail capturing critical overrides and security events.

### 1.5 Definitions, Acronyms, and Abbreviations
| Term | Definition |
|---|---|
| **ADE** | Adverse Drug Event; injury resulting from medical interventions related to a drug. |
| **CDSS** | Clinical Decision Support System; active knowledge system providing clinical advice. |
| **EMR** | Electronic Medical Record; digital version of patient paper charts in an outpatient facility. |
| **RxNorm** | Standardized nomenclature for clinical drugs produced by the National Library of Medicine. |
| **BMI** | Body Mass Index; weight in kilograms divided by height in meters squared ($kg/m^2$). |
| **SpO₂** | Peripheral capillary oxygen saturation; percentage of oxygenated hemoglobin. |
| **DAO** | Data Access Object; design pattern separating persistence logic from business logic. |
| **ADR** | Architecture Decision Record; structured document capturing architectural choices. |
| **RBAC** | Role-Based Access Control; security mechanism restricting resource access by role. |
| **INR / ₹** | Indian Rupee; legal tender and base currency for medication pricing in Audino. |

### 1.6 References
1. IEEE Std 830-1998, *IEEE Recommended Practice for Software Requirements Specifications*.
2. National Medical Commission (NMC) Regulations on Registered Medical Practitioner Conduct (Prescription Format Guidelines), Government of India.
3. Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley.
4. Martin, R. C. (2018). *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall.

---

## 2. Overall Description

### 2.1 Product Perspective
Audino is a standalone, fat-client desktop software system implemented in Java 17 and JavaFX 19. It operates in two deployment modes without any architectural code alteration:
1. **Centralized Clinic Deployment:** Multiple desktop instances (e.g., Reception Desk, Doctor Consultation Room 1, Doctor Consultation Room 2, Waiting Hall Kiosk) connect across a secure Local Area Network (LAN) to a centralized PostgreSQL instance on port `5432`.
2. **Zero-Setup Autonomous Deployment:** When no external PostgreSQL server is detected at `localhost:5432`, the system automatically provisions and boots an embedded PostgreSQL engine (`io.zonky.test:embedded-postgres`) persisting state to `./data/pg-data`. This allows instant zero-setup demonstration and single-doctor clinic operations with zero technical overhead.

```
       +-------------------------------------------------------+
       |                  Audino App Shell                     |
       |  (JavaFX 19 FXML + Custom Looked-Up CSS Design System)|
       +-------------------------------------------------------+
            |                  |                   |
            v                  v                   v
   +----------------+  +----------------+  +----------------+
   |   Admin View   |  |  Doctor View   |  | Reception View |
   +----------------+  +----------------+  +----------------+
            \                  |                  /
             v                 v                 v
       +-------------------------------------------------------+
       |                     Service Layer                     |
       |  (AuthService, SafetyEngine, QueueService, etc.)       |
       +-------------------------------------------------------+
                               |
                               v
       +-------------------------------------------------------+
       |                       DAO Layer                       |
       |  (AccountDAO, PatientDAO, MedicationDAO, TokenDAO...)  |
       +-------------------------------------------------------+
                               |
                               v
       +-------------------------------------------------------+
       |             HikariCP Connection Pool                  |
       +-------------------------------------------------------+
                               |
                               v
            +------------------------------------+
            | PostgreSQL 15 (Local / Embedded)   |
            +------------------------------------+
```

### 2.2 Product Functions
- **Role-Based Access Authentication:** Cryptographic verification using jBCrypt (cost factor 12) with instant polymorphic routing to role-specific shells (`Admin`, `Doctor`, `Receptionist`).
- **Patient Registration & Vitals Intake:** Longitudinal tracking of demographic, anthropometric, and vital statistics with automatic BMI calculation and clinical physiological range guards.
- **Dynamic Daily Token Dispensation:** Generation of sequential daily tokens (`T-001` to `T-999`) assigned to specific practitioners with live queue synchronization.
- **Intelligent Medication Authoring:** High-speed lookup with Levenshtein-distance fuzzy typo-correction, polymorphic dosage validation, and instant price reconciliation.
- **Clinical Safety Radar:** Multi-strategy rule engine detecting drug-drug, drug-allergy, drug-condition, and therapeutic duplication conflicts.
- **Enforced Clinical Override Protocol:** Hard block on saving prescriptions with unresolved CRITICAL interactions unless accompanied by a mandatory justification audit note ($\ge 15$ characters).
- **Chronological Daily Pill Schedule:** Algorithmic translation of medical frequency codes (`Once daily`, `Twice daily`, etc.) into a 4-phase patient day plan (Morning, Afternoon, Evening, Night) with meal timing.
- **Bi-modal PDF Generation:** Generation of formal clinical prescriptions (with doctor registration numbers and clinic headers) and simplified patient guidance sheets.
- **Secure Self-Service Kiosk:** Touch-optimized interface for outpatient prescription and schedule verification with dual-factor identification and automated security lockout.

### 2.3 User Classes and Characteristics
1. **Clinic Administrator (`Admin`):** High privilege level. Performs staff provisioning, formulary maintenance, drug interaction rule authoring, and forensic audit examination. High technical and clinical data literacy.
2. **Consulting Physician (`Doctor`):** Clinical practitioner registered with the Medical Council of India / NMC. High cognitive load; demands rapid, keyboard-first prescribing, unhindered consultation flows, and clear, non-intrusive safety notifications.
3. **Front-Desk Receptionist (`Receptionist`):** Outpatient administrative operator. High throughput requirement during peak clinic hours; performs patient registration, vitals recording, and token generation.
4. **Outpatient (`Patient`):** End-user interacting via the Patient Kiosk. Wide variance in age, education, and digital literacy. Demands large typography, touch-friendly inputs, multi-factor privacy verification, and unambiguous medication guidance.
5. **Hospital Medical Director / Quality Auditor (Secondary):** Reviews audit trails and critical override justifications for clinical governance and medical-legal compliance.
6. **Outpatient Pharmacist (Indirect):** Fulfills prescriptions generated by Audino, relying on standard RxNorm terminology, unambiguous dosages, and total unit counts.

### 2.4 Operating Environment
- **Operating System:** Microsoft Windows 10/11 (64-bit), macOS 12+, Ubuntu Linux 20.04 LTS+.
- **Display Resolution:** Optimized for $1366 \times 768$ laptops at 125% Windows scaling (equivalent effective viewport: $1093 \times 614$). Minimum window height $\le 600$ px with adaptive `ScrollPane` wrappers. Supports full scaling up to $1920 \times 1080$ Full HD.
- **Runtime Environment:** OpenJDK 17.0+ LTS.
- **Database Engine:** PostgreSQL 14+ (external) or Embedded PostgreSQL via `io.zonky.test:embedded-postgres`.

### 2.5 Design and Implementation Constraints
- **Architecture Constraints:** Pure desktop application using Java 17 and JavaFX 19. No web servers, Spring Framework, Hibernate/JPA, or external GUI theme libraries (e.g., AtlantaFX or ControlsFX).
- **Styling & Assets:** Single custom-built CSS design system (`application.css`, `light.css`, `dark.css`) utilizing looked-up color tokens. The sole permitted external UI library is **Ikonli** (`ikonli-javafx` with Material Design 2 or Feather pack).
- **Database Access:** Standard JDBC API utilizing the DAO pattern. SQL statements must be strictly parameterized via `PreparedStatement`. No string concatenation or query builders.
- **Concurrency & UI Safety:** Database operations must never block the JavaFX Application Thread. Multi-threaded updates must synchronize to the UI using `Platform.runLater()`.
- **Zero-Setup Execution:** Project must compile, execute unit tests, and launch via Maven (`mvn clean test`, `mvn clean javafx:run`) on any standard machine possessing only a JDK 17 installation.

### 2.6 Assumptions and Dependencies
- Clinic workstations maintain synchronized system clocks (via NTP) for token timestamps and audit logs.
- Network connectivity between multi-instance workstations is established over an internal LAN if operating against a shared external database.
- Standard physical printers or PDF viewers are configured at the OS level for prescription printing.

---

## 3. System Features & Functional Requirements

### 3.1 Module 1: Authentication & Access Control
- **FR-AUTH-01 (Authentication):** The system shall authenticate users via unique username and password verified against stored jBCrypt cryptographic hashes (cost factor 12).
- **FR-AUTH-02 (Account Status Check):** The system shall verify that the account `active` flag is true; deactivated accounts shall be immediately rejected with an `AccountDisabledException`.
- **FR-AUTH-03 (Polymorphic Role Routing):** Upon successful authentication, the system shall invoke the polymorphic method `account.getDashboardFxml()` without conditional `if/else` role checks to load the appropriate dashboard shell.
- **FR-AUTH-04 (Password Policy & Update):** The system shall allow authenticated users to change their password upon verifying their current password, requiring the new password to be at least 8 characters in length.
- **FR-AUTH-05 (Authentication Auditing):** The system shall log all successful logins and failed authentication attempts into the immutable `audit_logs` table, capturing IP/terminal, timestamp, and username.
- **FR-AUTH-06 (Caps Lock Detection):** The login interface shall detect and display a visual warning when the user's Caps Lock key is active.
- **FR-AUTH-07 (Session Management):** The system shall maintain the current authenticated user context in a thread-safe `SessionManager` singleton.

### 3.2 Module 2: Admin System & Formulary Management
- **FR-ADM-01 (Staff Account CRUD):** The system shall enable administrators to create, read, update, and toggle active status for staff accounts (`Admin`, `Doctor`, `Receptionist`).
- **FR-ADM-02 (Doctor Metadata Capture):** For `Doctor` accounts, the system shall mandate the collection of clinical specialization and Medical Council Registration Number.
- **FR-ADM-03 (Self-Deactivation Guard):** The system shall prohibit an administrator from deactivating or revoking administrative rights from their own currently active session.
- **FR-ADM-04 (Formulary Medication CRUD):** The system shall maintain an exhaustive drug formulary capturing Generic Name, Brand Name, RxNorm Code, Medication Type (`TABLET`, `CAPSULE`, `LIQUID`, `INJECTION`), Strength, Active Ingredients, and Price Per Unit in INR (₹).
- **FR-ADM-05 (Dynamic Medication Pricing Prompt):** When a doctor prescribes a medication lacking a registered unit price, the system shall prompt the user for a price, persist it to the formulary, and proceed seamlessly.
- **FR-ADM-06 (Interaction Rules CRUD):** The system shall allow administrators to author, view, update, and delete clinical interaction rules categorized by `DRUG_DRUG`, `DRUG_ALLERGY`, and `DRUG_CONDITION`, with severity assigned as `CRITICAL`, `WARNING`, or `INFO`.
- **FR-ADM-07 (Hot Rule Reloading):** Any modifications made to interaction rules or formulary entries shall take effect immediately in the running `SafetyEngine` without requiring an application restart.
- **FR-ADM-08 (Forensic Audit Exploration):** The system shall provide an audit exploration interface supporting filtering by operator username, entity type, operation type (`CREATE`, `UPDATE`, `DELETE`, `OVERRIDE`, `LOGIN`), and date range.
- **FR-ADM-09 (Operational KPI Dashboard):** The admin landing page shall display real-time metric cards for Today's Total Tokens, Prescriptions Issued, Critical Overrides Recorded, and Active Medical Personnel, accompanied by graphical trend charts.

### 3.3 Module 3: Receptionist & Outpatient Queue Management
- **FR-REC-01 (Patient Registration):** The system shall capture patient demographic records: First Name, Last Name, Date of Birth (with age dynamically calculated), Gender, 10-digit Indian Mobile Number, and Address.
- **FR-REC-02 (Medical Profile Initialization):** Registration shall support multi-entry tagging for confirmed drug allergies and chronic medical conditions.
- **FR-REC-03 (Duplicate Patient Detection):** The system shall alert the receptionist if a patient with identical mobile number and name already exists.
- **FR-REC-04 (Longitudinal Vitals Capture):** The system shall record patient physiological vitals per visit: Height (cm), Weight (kg), Systolic Blood Pressure (mmHg), Diastolic Blood Pressure (mmHg), Pulse Rate (bpm), Body Temperature (°C), and Oxygen Saturation SpO₂ (%).
- **FR-REC-05 (Automatic BMI Computation):** The system shall compute Body Mass Index ($BMI = weight / (height/100)^2$) instantly upon entering height and weight, highlighting underweight, normal, overweight, and obese classifications.
- **FR-REC-06 (Vitals Physiological Range Validation):** The system shall reject unrealistic vital inputs (e.g., Systolic BP $< 50$ or $> 260$, Pulse $< 30$ or $> 220$, SpO₂ $< 50\%$ or $> 100\%$) with descriptive validation messages.
- **FR-REC-07 (Sequential Daily Token Dispensation):** The system shall generate daily sequential tokens formatted as `T-001` through `T-999`, restarting each calendar day, optionally linked to a specific doctor.
- **FR-REC-08 (Token State Machine Enforcement):** The system shall enforce legal transitions: `WAITING → IN_CONSULTATION → COMPLETED` and `WAITING → CANCELLED`. Invalid transitions shall throw `InvalidTokenStateException`.
- **FR-REC-09 (Live Queue Synchronization):** The queue interface shall register as a `QueueObserver` with `QueueService`, synchronizing state every 3 seconds via background polling and updating the JavaFX UI on `Platform.runLater()`.

### 3.4 Module 4: Clinical Consultation & Safety Decision Support
- **FR-DOC-01 (Doctor Queue & Patient Calling):** The doctor portal shall provide "My Queue" (tokens assigned to doctor or unassigned) and "All Patients" views. A single click on "Call Next" shall advance the oldest `WAITING` token to `IN_CONSULTATION` and load the patient's record.
- **FR-DOC-02 (Persistent Patient Safety Banner):** The consultation workspace shall display a sticky top banner presenting patient demographics, ID, token number, latest vitals summary, and prominent color-coded chips for allergies (Red) and chronic conditions (Amber).
- **FR-DOC-03 (Longitudinal History & Sparkline Trends):** The doctor shall have instant tabbed access to past consultation history and physiological parameter sparklines across previous visits.
- **FR-DOC-04 (Fuzzy Typo-Tolerant Medication Search):** The medication search bar shall perform rapid prefix and substring matching across Generic Names, Brand Names, and RxNorm codes, utilizing Levenshtein distance and trigram cosine similarity to provide "Did you mean...?" suggestions for misspelled queries.
- **FR-DOC-05 (Polymorphic Dosage Verification):** The system shall validate prescribed dosages according to the specific subclass of `Medication`:
  - `TabletMedication`: Validates integer tablet counts (e.g., "1", "2") or metric strengths (e.g., "650mg").
  - `CapsuleMedication`: Validates unit capsule counts.
  - `LiquidMedication`: Validates volumetric doses in milliliters (e.g., "5ml", "10ml").
  - `InjectionMedication`: Validates vial or volume notations (e.g., "1 vial", "2ml").
- **FR-DOC-06 (Clinical Safety Radar Engine):** The system shall execute `SafetyEngine.evaluate()` asynchronously on every item addition, modification, or deletion, evaluating:
  - Drug-Drug Interactions (`DrugDrugStrategy`)
  - Drug-Allergy Contraindications (`DrugAllergyStrategy`)
  - Drug-Condition Contraindications (`DrugConditionStrategy`)
  - Duplicate Therapeutic Classes (`DuplicateTherapyStrategy`)
- **FR-DOC-07 (Mandatory Critical Override Enforcement):** If one or more `CRITICAL` interaction alerts are triggered:
  - The prescription approval mechanism shall be strictly blocked.
  - The doctor must either remove the offending medication or initiate an "Override" action.
  - The override dialog shall mandate a clinical justification rationale consisting of at least 15 non-whitespace characters.
  - Attempting to persist an un-overridden critical prescription shall throw `CriticalInteractionException`.
- **FR-DOC-08 (Prescription Lifecycle & Cost Summary):** The doctor may save prescriptions as `DRAFT` or finalize them as `APPROVED`. The system shall compute real-time line-item and aggregate prescription costs in INR (₹) polymorphically using `medication.calculateUnits(...)`.
- **FR-DOC-09 (Undo Support):** When a medication item is removed from the active prescription table, the system shall provide a transient snackbar notification allowing one-click Undo restoration.
- **FR-DOC-10 (Consultation Finalization):** The doctor may only complete a consultation (`TokenStatus.COMPLETED`) if a prescription has been approved or if the physician explicitly confirms "No Prescription Required".

### 3.5 Module 5: Prescription & Documentation Output
- **FR-DOC-11 (Formal PDF Generation):** The system shall utilize iText 5 to compile an official clinical prescription containing:
  - Clinic branding, address, and date/time.
  - Doctor's full name, clinical specialization, and National Medical Council Registration Number.
  - Patient demographics, ID, and visit vitals summary.
  - Itemized table of prescribed medications, dosages, frequencies, durations, and food instructions.
  - Visual 4-phase daily pill schedule grid.
  - Documented justification notes for any overridden critical alerts (doctor copy).
  - Formal signature line and legal disclaimer.

### 3.6 Module 6: Patient Kiosk Self-Service
- **FR-KSK-01 (Kiosk Entry & Dual Verification):** The patient kiosk shall operate without staff login. The patient must enter their Token Number (e.g., `T-001`) and a second verification factor: either the last 4 digits of their registered mobile phone number or their Date of Birth (DD/MM/YYYY).
- **FR-KSK-02 (Brute-Force Lockout Defense):** After 3 consecutive failed verification attempts, the kiosk interface shall lock input for 60 seconds, displaying a countdown timer.
- **FR-KSK-03 (Touch-Optimized Numeric Keypad):** The kiosk shall feature an on-screen high-contrast touch keypad for easy finger input on touch monitors.
- **FR-KSK-04 (Visual Chrono-Grid Pill Schedule):** The kiosk shall present the patient's daily medication schedule formatted into four intuitive day phases:
  - Morning (☀ 08:00 AM)
  - Afternoon (⛅ 01:00 PM)
  - Evening (☕ 06:00 PM)
  - Night (🌙 09:00 PM)
  Each card shall state the medicine brand name, dosage, and dietary instruction (e.g., "After food").
- **FR-KSK-05 (Patient PDF Download / Print):** The patient may trigger a download/print of the simplified patient prescription sheet with a single button tap.
- **FR-KSK-06 (Privacy Inactivity Reset):** The kiosk shall maintain a 120-second idle timer. If no user interaction occurs, the screen shall clear all displayed medical data and return to the verification landing view.

---

## 4. External Interface Requirements

### 4.1 User Interfaces
- **Visual Design System:** Modern clinical SaaS aesthetic implemented exclusively via custom JavaFX CSS (`application.css`, `light.css`, `dark.css`).
- **Color Palette & Contrast Tokens:**
  - Clinical Teal Primary: `#0F766E` (Hover: `#115E59`, Soft: `#CCFBF1`)
  - Neutral Backgrounds: Light `#F4F6FA`, Dark `#0F172A`; Surfaces: Light `#FFFFFF`, Dark `#1E293B`
  - Alert States: Critical `#DC2626`, Warning `#D97706`, Info `#2563EB`, Success `#16A34A`
- **Typography:** Inter font family loaded dynamically from bundled TrueType font files (`Inter-Regular.ttf`, `Inter-Medium.ttf`, `Inter-SemiBold.ttf`, `Inter-Bold.ttf`).
- **Iconography:** Vector icons rendered exclusively via Ikonli (`ikonli-javafx` and `ikonli-materialdesign2-pack` / `ikonli-feather-pack`).
- **Responsive Geometry:** Main window opens maximized with a minimum constrained size of $1093 \times 600$ px, fully compatible with $1366 \times 768$ displays at 125% Windows DPI scaling.
- **Keyboard Navigation & Accelerators:**
  - `Ctrl + K`: Universal Command Palette / Fast Search
  - `Ctrl + S`: Approve & Save Active Prescription
  - `Ctrl + Enter`: Call Next Waiting Patient
  - `Ctrl + N`: New Patient Registration
  - `Escape`: Dismiss active dialogs or close command palette

### 4.2 Software Interfaces
- **Database Management System:** PostgreSQL 14+ via standard JDBC driver (`org.postgresql:postgresql:42.6.0`).
- **Embedded Database Engine:** `io.zonky.test:embedded-postgres` version 2.0.4+ for zero-setup execution.
- **Connection Pool:** HikariCP (`com.zaxxer:HikariCP:5.0.1`) managing pooled database connections.
- **Document Rendering:** iText 5 (`com.itextpdf:itextpdf:5.5.13.3`) generating PDF binary documents.
- **Password Hashing:** jBCrypt (`org.mindrot:jbcrypt:0.4`) for adaptive salted hashing.

---

## 5. Non-Functional Requirements (NFR)

### 5.1 Performance Requirements (`NFR-PERF`)
- **NFR-PERF-01 (Safety Engine Latency):** Clinical safety evaluation across all active strategies for a prescription containing up to 20 medications shall complete in under **200 milliseconds**.
- **NFR-PERF-02 (Authentication Response):** User authentication processing, including BCrypt cost-12 hash verification and UI shell initialization, shall complete within **1000 milliseconds**.
- **NFR-PERF-03 (Queue Synchronization Cadence):** The background queue polling service shall refresh live token state across networked instances within **3.0 seconds**.
- **NFR-PERF-04 (Autocorrect & Fuzzy Search):** Medication search suggestions and Levenshtein typo-correction calculations shall deliver results within **50 milliseconds** of keyboard input.
- **NFR-PERF-05 (PDF Generation Speed):** Generating and writing a complete multi-page clinical prescription PDF to disk shall complete in under **1.5 seconds**.

### 5.2 Safety & Reliability (`NFR-SAFE`)
- **NFR-SAFE-01 (Zero Unjustified Critical Bypass):** Under no circumstance shall the system allow a prescription with an active `CRITICAL` interaction alert to transition to `APPROVED` without an associated justification record of at least 15 characters.
- **NFR-SAFE-02 (ACID Transaction Integrity):** All multi-table clinical writes (prescription header, line items, interaction alerts, audit logs, and token status update) shall be bound within a single JDBC transaction (`connection.setAutoCommit(false)`), executing complete rollback on any exception.
- **NFR-SAFE-03 (Idempotent Database Initialization):** The database schema initializer and seed routines must be strictly idempotent, allowing continuous restarts without data duplication or constraint violation.
- **NFR-SAFE-04 (Fail-Safe Data Persistence):** If external database connectivity drops, the system shall report a descriptive connection error dialog without abnormal program termination.

### 5.3 Security & Privacy (`NFR-SEC`)
- **NFR-SEC-01 (Cryptographic Storage):** Plaintext user passwords shall never be persisted or logged; passwords must be hashed with jBCrypt at work factor 12.
- **NFR-SEC-02 (SQL Injection Prevention):** 100% of database queries must utilize parameterized `PreparedStatement` or callable routines. Zero string concatenation is permitted in SQL commands.
- **NFR-SEC-03 (Strict Role-Based Authorization):** Controllers must verify session role permissions prior to executing administrative or prescribing operations.
- **NFR-SEC-04 (Kiosk Identity Protection):** The Patient Kiosk shall enforce dual-factor verification (Token + Phone/DOB) and mask medical data after 120 seconds of user inactivity.
- **NFR-SEC-05 (Immutable Forensic Audit Trail):** Every security-sensitive action (logins, overrides, rule updates, staff changes) shall write an immutable record to `audit_logs` containing timestamp, operator, entity, and detailed context.

### 5.4 Usability & Accessibility (`NFR-USE`)
- **NFR-USE-01 (Viewport Accommodating Design):** The UI layout shall fit comfortably without clipping or horizontal scrollbars on a $1366 \times 768$ display running at 125% Windows scaling.
- **NFR-USE-02 (Dual Theme Support):** The system shall provide seamless toggle between Light and Dark themes, with user preference persisted across restarts via `java.util.prefs`.
- **NFR-USE-03 (Non-Color-Only Indicators):** All critical clinical notifications and statuses must employ both a visual icon and descriptive text in addition to color-coding (WCAG 2.1 Success Criterion 1.4.1).
- **NFR-USE-04 (Keyboard Prescribing Efficiency):** A physician shall be capable of authoring a standard 3-medication prescription entirely via keyboard shortcuts without touching a mouse.

### 5.5 Maintainability & Extensibility (`NFR-MAINT`)
- **NFR-MAINT-01 (Open-Closed Clinical Engine):** New interaction checking rules (e.g., Genetic Contraindication Strategy) must be introducible by implementing the `InteractionStrategy` interface without altering `SafetyEngine`.
- **NFR-MAINT-02 (Polymorphic Medication Subtypes):** New medication delivery forms must be implementable by extending `Medication` without modifying prescription calculation or pricing routines.
- **NFR-MAINT-03 (DAO Data Layer Abstraction):** The service layer must interact exclusively with DAO interfaces, permitting underlying persistence engine substitution without business logic refactoring.
- **NFR-MAINT-04 (Automated Code Quality & Test Coverage):** The codebase shall achieve $\ge 80\%$ line coverage across core domain, service, and safety engine packages evaluated via JaCoCo.

---

## 6. Verification & Traceability Matrix

| Requirement ID | Module / Category | Target Class / Component | Verification Method |
|---|---|---|---|
| `FR-AUTH-01` | Auth | `AuthService`, `AccountDAOJdbc` | Unit & Integration Test |
| `FR-AUTH-03` | Auth / OOM | `Account.getDashboardFxml()`, `Router` | System / Unit Test |
| `FR-ADM-04` | Formulary | `MedicationDAOJdbc`, `MedicationFactory` | Integration Test |
| `FR-ADM-06` | Rules | `InteractionRuleDAOJdbc`, `SafetyEngine` | Unit & Integration Test |
| `FR-REC-04` | Vitals | `VitalsDAOJdbc`, `VitalsValidationTest` | Unit Test |
| `FR-REC-07` | Queue | `TokenDAOJdbc`, `QueueService` | Integration Test |
| `FR-REC-08` | State Machine | `TokenStatus.canTransitionTo()` | Unit Test (`TokenStatusTest`) |
| `FR-DOC-04` | Prescribe | `MedicationSearchService` | Unit Test (`MedicationSearchTest`) |
| `FR-DOC-05` | Prescribe / OOM | `Medication.isValidDosage()` | Unit Test (`MedicationDosageTest`) |
| `FR-DOC-06` | Safety / OOM | `SafetyEngine`, `DrugDrugStrategy` | Unit Test (`SafetyEngineTest`) |
| `FR-DOC-07` | Safety / Law | `PrescriptionService.save()` | Unit Test (`PrescriptionServiceTest`) |
| `FR-DOC-11` | PDF | `PrescriptionPdfService` | System / Integration Test |
| `FR-KSK-01` | Kiosk | `KioskController`, `TokenService` | System Test |
| `NFR-PERF-01` | Performance | `SafetyEngineBenchmark` | Performance Unit Test |
| `NFR-SEC-01` | Security | `BCryptPasswordHasher` | Unit Test (`AuthServiceTest`) |
