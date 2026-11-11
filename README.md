# Audino: Intelligent Clinical Decision Support & EMR System

[![Java Version](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![JavaFX Version](https://img.shields.io/badge/JavaFX-19-orange.svg)](https://openjfx.io/)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![Tests](https://img.shields.io/badge/Tests-74%20Passed-brightgreen.svg)]()
[![Database](https://img.shields.io/badge/Database-PostgreSQL%20%7C%20Embedded-blue.svg)]()
[![Academic Evaluation](https://img.shields.io/badge/Evaluation-19--Nov--2026-critical.svg)]()

> **Audino** is a clinical-grade, zero-setup JavaFX 19 desktop application engineered to eliminate preventable **Adverse Drug Events (ADEs)** in outpatient clinics through real-time multi-dimensional safety screening, mandatory physician override justification with forensic audit trails, and an outpatient 4-phase chronological pill schedule.

Built strictly from foundational computer science principles for double university evaluation in **Object-Oriented Methodology (OOM)** and **Software Engineering (SE)**.

---

## 1. Quick Start: Zero-Setup Run

Audino requires **only JDK 17 and Apache Maven**. No manual PostgreSQL installation or configuration is required: the system automatically connects to `localhost:5432` if available, or automatically spins up **Embedded PostgreSQL** with local storage in `./data/pg-data`.

### Clone & Launch

```bash
# Clone the repository
git clone https://github.com/audino-clinical/clinicalprescriber.git
cd clinicalprescriber

# Execute all automated tests (74 tests)
mvn clean test

# Launch the desktop application
mvn clean javafx:run
```

---

## 2. Pre-Seeded Clinical Accounts & Credentials

The database automatically initializes its relational schema and seeds realistic clinical demonstration records on first startup:

| Role | Username | Password | Full Name / Description | Default Workspace Screen |
|---|---|---|---|---|
| **Doctor** | `doctor` | `1234567890` | **Dr. Arjun Mehta, MD** (Cardiologist, MCI-48291) | Consultation Prescribing & Safety Radar |
| **Admin** | `admin` | `1234567890` | **Rajesh Gupta** (System Administrator) | Admin Portal, Formulary & Audit Logs |
| **Receptionist** | `reception` | `1234567890` | **Neha Sharma** (Front Desk Coordinator) | Patient Intake, Vitals & Queue Tokens |
| **Patient Kiosk** | *(None)* | *(None)* | **Self-Service Terminal** (Token + Phone / DOB) | 4-Phase Chrono-Grid Pill Schedule |

> **Tip:** The login screen includes **1-Click Demo Quick-Fill Buttons** for instant evaluator sign-in.

---

## 3. 5-Minute Clinical Evaluation Demo Script

To demonstrate the system's clinical decision support and architectural rigor during course evaluation:

### Step 1: Reception Desk (Patient Intake & Queue Triage)
1. Log in as `reception` (or click "Reception Neha" demo button).
2. Register a new patient or select existing patient **Ramesh Sharma (`P-1001`)**.
3. Record physiological vitals: Height `175 cm`, Weight `78 kg`, BP `130/85 mmHg`, Pulse `76 bpm`, SpO2 `98%`. Notice instant **BMI computation (25.5 - Overweight)**.
4. Issue a queue token: Assign to **Dr. Arjun Mehta** $\rightarrow$ generates Token **`T-001`**.

### Step 2: Doctor Workspace (Prescribing & Safety Radar)
1. Log out and log in as `doctor` (or click "Dr. Arjun Mehta").
2. The left queue drawer displays **Token `T-001`** in `WAITING` status. Click **"Call Next Patient"** $\rightarrow$ Token updates to `IN_CONSULTATION`.
3. The top sticky clinical banner reflects Ramesh Sharma's details, red allergy chips (`Penicillin`, `NSAIDs`), and latest vitals.
4. **Demonstrate Typo-Autocorrect Search:**
   - Type `"amoxcilin"` in the search field $\rightarrow$ Autocorrect suggests *"Did you mean: Amoxicillin?"*.
   - Type `"brufn"` $\rightarrow$ Suggests *"Did you mean: Brufen 400?"*.
5. **Trigger Clinical Safety Alerts:**
   - Search and add **Warfarin 5mg** (Blood thinner).
   - Search and add **Aspirin 75mg** (Antiplatelet).
   - **Result:** The Safety Radar instantly triggers a **CRITICAL Drug-Drug Interaction** (`Aspirin + Warfarin: Severe gastrointestinal hemorrhage risk`).
   - The high-visibility **Blocking Banner** appears, and the **"Approve & Sign Prescription"** button is **hard-disabled**.
6. **Mandatory Clinical Override:**
   - Click **"Acknowledge & Override"** on the critical alert card.
   - Try entering `"ok"` $\rightarrow$ Rejected (must be $\ge 15$ characters).
   - Enter: `"Daily INR monitored; indicated for acute co-morbidity under hospital observation"` $\rightarrow$ Alert is acknowledged.
   - The "Approve" button is unlocked.
7. Click **"Approve & Sign Prescription"** $\rightarrow$ Atomically writes to PostgreSQL via an ACID transaction.
8. Click **"Download Official Prescription PDF"** $\rightarrow$ Exports an official signed PDF document with clinic branding, patient vitals, item table, and alert warnings.

### Step 3: Patient Self-Service Kiosk (Visual 4-Phase Chrono-Grid)
1. Log out to return to the Login screen.
2. Click **"Open Patient Kiosk (No Login Required)"**.
3. Use the on-screen numeric keypad to enter Token **`T-001`** and verify with Ramesh's phone (`9820112345`).
4. **Visual Schedule Display:** Prescriptions are mapped into a clear chronological daily grid:
   - **Morning (08:00 AM)**
   - **Afternoon (01:00 PM)**
   - **Evening (06:00 PM)**
   - **Night (09:00 PM)**
   - Includes meal precautions (*"Take on an empty stomach"* or *"Take strictly after meals"*).
5. Click **"Download My Medication Schedule PDF"** for the patient printout.

### Step 4: Admin Portal (Forensic Audit Trail & Hot Rule Ingestion)
1. Log in as `admin`.
2. Inspect the **Executive KPI Cards** and **JavaFX Activity Charts** (7-day prescription trend, alert severity breakdown).
3. Switch to the **Forensic Audit Logs** tab: Notice the immutable audit record detailing Dr. Arjun Mehta's critical override, including the exact physician justification note and timestamp.
4. Switch to the **Interaction Rules** tab: Add a new clinical rule (e.g. `Ciprofloxacin` + `Theophylline`). The safety engine ingests it dynamically at runtime with **zero server restart** (Open/Closed Principle).

---

## 4. Technology Stack

| Layer / Concern | Technology Selection | Justification / Engineering Rationale |
|---|---|---|
| **Language** | Java 17 (LTS) | Modern language features: Records, Pattern Matching, Sealed types |
| **UI Framework** | JavaFX 19 | Hardware-accelerated desktop UI with declarative FXML separation |
| **Design System** | Custom CSS Design Tokens | Built from scratch (`light.css`, `dark.css`); zero external theme bloat |
| **Database** | PostgreSQL 15 / Embedded PG | Dual-mode: attaches to local PG or boots embedded in `./data/pg-data` |
| **Connection Pool** | HikariCP 5.0.1 | Zero-overhead, high-performance production JDBC connection pool |
| **Persistence** | Native JDBC (DAO Pattern) | Parameterized `PreparedStatement` with ACID multi-table transactions |
| **Security** | jBCrypt (Cost 12) | Adaptive key expansion resisting GPU dictionary attacks |
| **PDF Engine** | iText 5 (`com.itextpdf`) | High-fidelity medical prescription document generation |
| **Iconography** | Ikonli (Material Design 2) | Vector icon font packs (`ikonli-materialdesign2-pack`) |
| **Testing** | JUnit 5 + AssertJ + JaCoCo | 74 automated tests with branch and instruction coverage reporting |

---

## 5. Architectural Structure

Audino strictly follows the **View (FXML) $\rightarrow$ Controller $\rightarrow$ Service $\rightarrow$ DAO $\rightarrow$ PostgreSQL** architectural pattern. Controllers remain thin; business logic resides solely in services, and services depend on abstract DAO interfaces:

```
com.audino
├── Launcher, AudinoApplication
├── controller            (JavaFX thin controllers)
│   ├── AdminDashboardController
│   ├── AppShellController
│   ├── DoctorConsultationController
│   ├── KioskController
│   ├── LoginController
│   └── ReceptionistController
├── dao                   (DAO contracts)
│   └── jdbc              (Native JDBC parameterized implementations)
├── exception             (AudinoException hierarchy)
├── factory               (AccountFactory, MedicationFactory, AlertFactory)
├── model                 (Domain entities, enums)
│   ├── Account           (Admin, Doctor, Receptionist)
│   └── Medication        (Tablet, Capsule, Liquid, Injection)
├── observer              (Live QueueSubject, QueueObserver)
├── service               (Auth, Prescriptions, Search, PDF, Audit)
│   └── strategy          (SafetyEngine, DrugDrug, Allergy, Condition, Duplicate)
├── ui
│   ├── components        (14 custom controls: StatCard, PatientBanner, Sparkline...)
│   └── navigation        (NavigationService, ThemeManager, CommandPalette)
└── util                  (ConfigurationManager, DataSeeder, DatabaseInitializer)
```

---

## 6. Object-Oriented Methodology (OOM) Highlights

Every concept in the OOM rubric is cleanly coded and marked with an inline comment (`// OOM: ...`):

- **Encapsulation:** Private state, physiological boundary validation in `PatientVitals`, and unmodifiable defensive collection copies in `Patient`, `Medication`, and `Prescription`.
- **Inheritance Hierarchies:**
  1. `Account` $\leftarrow$ `Admin`, `Doctor`, `Receptionist`
  2. `Medication` $\leftarrow$ `TabletMedication`, `CapsuleMedication`, `LiquidMedication`, `InjectionMedication`
- **Dynamic Polymorphism:**
  - `SafetyEngine` evaluates `InteractionStrategy` instances polymorphically.
  - Login view routing executes `account.getDashboardFxml()` with zero `if (role == ...)` chains.
  - Financial costing delegates directly to `medication.calculateCost(...)`.
- **Object Relationships:**
  - **Association:** `Doctor` consults `Patient`.
  - **Aggregation:** `Prescription` references independent `Medication` entities in the formulary.
  - **Composition:** `Prescription` owns its `PrescriptionItem`s and `PrescriptionAlert`s (`ON DELETE CASCADE`).
- **Design Patterns:** Strategy, Observer, DAO, Factory, and Singleton.
- **SOLID Principles:** SRP (decoupled services), OCP (runtime strategy registration), LSP (subclass substitutability), ISP (granular DAOs), and DIP (services depend on DAO interfaces).

---

## 7. Documentation Index

The complete Software Development Life Cycle (SDLC) documentation suite is located in [`docs/`](docs/):

1. **[`SRS.md`](docs/SRS.md):** Full IEEE 830-1998 Software Requirements Specification.
2. **[`Requirement_Analysis.md`](docs/Requirement_Analysis.md):** 10 Detailed Use Case Specifications with sequence steps and alternative flows.
3. **[`UML_Diagrams.md`](docs/UML_Diagrams.md):** 9 PlantUML architectural and behavioral diagrams (with embedded high-res PNG previews).
4. **[`Architecture_and_Design.md`](docs/Architecture_and_Design.md):** Architecture document with Architectural Decision Records (ADRs 001–008).
5. **[`OOM_Concepts_Map.md`](docs/OOM_Concepts_Map.md):** Exhaustive traceability matrix mapping every OOM rubric concept to source code lines.
6. **[`Test_Plan.md`](docs/Test_Plan.md):** IEEE 829 Software Verification & Test Plan with 74-test inventory and JaCoCo coverage metrics.
7. **[`Quality_Assurance.md`](docs/Quality_Assurance.md):** Software Quality Assurance report covering cryptography, SQL injection immunity, and UI viewport ergonomics.
8. **[`Project_Plan.md`](docs/Project_Plan.md):** SDLC management plan, WBS, Gantt schedule, and risk management log.
9. **[`Maintenance.md`](docs/Maintenance.md):** Developer extensibility manual and operations guide.

---

## 8. License & University Submission

Submitted for academic evaluation in **Object-Oriented Methodology (OOM)** and **Software Engineering (SE)**.  
**Evaluation Date:** 19 November 2026.  
**License:** Educational & Clinical Open-Source Evaluation License.
