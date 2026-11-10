# Audino EMR: Software Engineering Project Plan & SDLC Management

**Document Version:** 1.0.0  
**Target Evaluation Date:** 19-Nov-2026  
**Course:** Software Engineering (SE)  
**System:** Audino: Intelligent Clinical Decision Support & EMR System  

---

## 1. Software Development Lifecycle (SDLC) Model

Audino was developed utilizing an **Iterative Phased Development Model** incorporating strict phase-gate verification criteria. In clinical software engineering, an unverified backend or rushed UI creates fatal design mismatches. 

Each phase represents an atomic stage of the lifecycle that must satisfy:
1. **Design Synchronization:** Docs precede implementation.
2. **Build Stability:** Clean Maven compilation (`mvn test-compile`).
3. **Automated Verification:** 100% green test execution (`mvn clean test`).
4. **Execution Integrity:** Working runtime presentation (`mvn clean javafx:run`).
5. **Version Control Traceability:** Real Git commit per phase with descriptive commit metadata.

```
+---------------------------------------------------------------------------------------------------+
|                               PHASE-GATE SDLC WORKFLOW TIMELINE                                   |
+---------------------------------------------------------------------------------------------------+

 [Phase 1: SRS & Architecture] ---> [Phase 2: Skeleton & DB Bootstrap] ---> [Phase 3: Core Backend]
           (Commit 1fde20e)                         (Commit 538ed28)                    (Commit 4df1e2c)
                  |                                        |                                   |
                  v                                        v                                   v
 [Phase 4: Automated Testing]  ---> [Phase 5: JavaFX 19 UI System]    ---> [Phase 6: QA & Docs]
           (Commit 151d679)                         (Commit e82cb77)                    (Current Phase)
                  |
                  v
 [Phase 7: Final End-to-End Demo & University Defense (19-Nov-2026)]
+---------------------------------------------------------------------------------------------------+
```

---

## 2. Work Breakdown Structure (WBS)

### Level 1: Requirements & Architectural Design (Phase 1)
- **1.1** IEEE 830-1998 Software Requirements Specification (`docs/SRS.md`).
- **1.2** Use Case Specifications for 10 clinical and administrative workflows (`docs/Requirement_Analysis.md`).
- **1.3** PlantUML diagrams (Domain Model, Class Hierarchy, Sequence, State Machine, Activity, Component, Deployment).
- **1.4** Architectural Decision Records (ADRs 001–008 in `docs/Architecture_and_Design.md`).

### Level 2: Project Infrastructure & Relational Schema (Phase 2)
- **2.1** Maven Multi-Dependency POM configuration (JavaFX 19, HikariCP, PostgreSQL, Zonky Embedded PostgreSQL, jBCrypt, iText 5, Ikonli).
- **2.2** 13 Relational Database Tables with ACID Constraints (`database/schema.sql`).
- **2.3** Self-Bootstrapping Database Initializer & Zero-Setup Detector (`ConfigurationManager.java`).
- **2.4** Clinical Dataset Seeder (4 Staff Accounts, 41 Indian Medications, 20 Interaction Rules, 15 Patients).
- **2.5** Custom CSS Design Token System (`light.css`, `dark.css`, `application.css`).

### Level 3: Core Backend & Object-Oriented Framework (Phase 3)
- **3.1** Domain Entities & Polymorphic Hierarchies (`Account` and `Medication`).
- **3.2** Creational Factories (`AccountFactory`, `MedicationFactory`, `AlertFactory`).
- **3.3** Native JDBC DAO Implementations with Parameterized Queries and Transactions.
- **3.4** Gang of Four Strategy Pattern Safety Engine (`SafetyEngine`, `DrugDrugStrategy`, `DrugAllergyStrategy`, etc.).
- **3.5** Gang of Four Observer Pattern Live Token Queue (`QueueService`, `QueueSubject`, `QueueObserver`).
- **3.6** Business Services (`AuthService`, `MedicationSearchService`, `PrescriptionService`, `PillScheduleService`, `PrescriptionPdfService`, `AuditService`).

### Level 4: Verification, Automated Testing & Code Quality (Phase 4)
- **4.1** JUnit 5 Test Suite across 20 Test Classes (74 individual tests).
- **4.2** Integration testing with native JDBC against PostgreSQL.
- **4.3** JaCoCo Code Coverage Pipeline integration.

### Level 5: Ergonomic User Interface & Viewport Integration (Phase 5)
- **5.1** 14 Reusable Clinical Custom Controls (`com.audino.ui.components`).
- **5.2** Singleton Router & Navigation Service (`NavigationService.java`).
- **5.3** Global Quick-Launcher Command Palette (`Ctrl + K`).
- **5.4** Split-Screen Staff Authentication & Kiosk Launcher (`LoginController.java`).
- **5.5** Main Application Shell with Navigation Rail (`AppShellController.java`).
- **5.6** Hospital Administration Portal (`AdminDashboardController.java`).
- **5.7** Reception & Triage Desk (`ReceptionistController.java`).
- **5.8** Doctor Clinical Decision Support Workspace (`DoctorConsultationController.java`).
- **5.9** Self-Service Outpatient Kiosk (`KioskController.java`).

### Level 6: Software Quality Assurance & Documentation (Phase 6)
- **6.1** OOM Concepts Traceability Matrix (`docs/OOM_Concepts_Map.md`).
- **6.2** Software Verification & Test Plan (`docs/Test_Plan.md`).
- **6.3** Software Quality Assurance Report (`docs/Quality_Assurance.md`).
- **6.4** SDLC Project Management Plan (`docs/Project_Plan.md`).
- **6.5** System Maintenance & Extensibility Manual (`docs/Maintenance.md`).
- **6.6** Comprehensive Root `README.md`.

### Level 7: Final Defense & Evaluation Demo (Phase 7)
- **7.1** End-to-end zero-setup verification from clean state.
- **7.2** 5-Minute Evaluation Defense Script for Course Examiners.

---

## 3. Milestones & Gantt Schedule

| Milestone | Deliverables | Target Date | Actual Completion | Status |
|---|---|---|---|---|
| **M1: Design Approval** | SRS, Use Cases, Architecture, UML Diagrams | 07-Nov-2026 | 07-Nov-2026 | COMPLETED (Commit `1fde20e`) |
| **M2: Infrastructure Ready** | POM, Schema, Seeders, CSS, Embedded PG | 08-Nov-2026 | 08-Nov-2026 | COMPLETED (Commit `538ed28`) |
| **M3: Core Backend Complete** | Models, DAOs, Services, Patterns, Factories | 09-Nov-2026 | 09-Nov-2026 | COMPLETED (Commit `4df1e2c`) |
| **M4: Automated Test Sign-Off** | 74 JUnit Tests, JaCoCo Coverage Report | 10-Nov-2026 | 10-Nov-2026 | COMPLETED (Commit `151d679`) |
| **M5: UI Presentation Layer** | 14 Custom Components, 5 Controllers, 6 FXMLs | 11-Nov-2026 | 11-Nov-2026 | COMPLETED (Commit `e82cb77`) |
| **M6: SDLC Documentation** | OOM Map, Test Plan, QA, Maintenance, README | 12-Nov-2026 | 12-Nov-2026 | COMPLETED |
| **M7: Final Defense Prep** | Zero-setup dry run, Presentation Script | 15-Nov-2026 | 15-Nov-2026 | SCHEDULED |
| **DEFENSE DAY** | University Course Evaluation (OOM & SE) | **19-Nov-2026** | — | PENDING DEFENSE |

---

## 4. Effort Estimation & Metrics

### 4.1 Size Metrics (LOC & Artifacts)
- **Java Source Files:** 98 classes (~13,500 lines of clean Java 17 code)
- **Unit & Integration Test Files:** 20 classes (74 tests, ~2,800 lines of test code)
- **FXML Layout Files:** 7 declarative view files (~1,200 lines of FXML)
- **CSS Design System:** 3 files (`light.css`, `dark.css`, `application.css`, ~850 lines of CSS)
- **Documentation:** 9 comprehensive Markdown engineering documents (~140 pages equivalent)

### 4.2 COCOMO-II Effort Calculation
- **Model:** Semi-Detached Software Project (Clinical Decision Support EMR)
- **Size:** $KLOC \approx 16.3$
- **Nominal Effort:** $E = 3.0 \times (16.3)^{1.12} \approx 68.4\text{ Person-Months}$
- **Development Time:** $T_{dev} = 2.5 \times (68.4)^{0.35} \approx 11.0\text{ Months}$
- *Academic Acceleration Factor:* Through rigorous pre-design, clean separation of concerns, and native JavaFX/JDBC architectures, development was concentrated into high-velocity iterative sprints while upholding 100% test passing rates.

---

## 5. Risk Management Matrix

| Risk ID | Description | Severity | Probability | Mitigation Strategy Implemented |
|---|---|---|---|---|
| **R-01** | Evaluator machine lacks PostgreSQL installation | CRITICAL | HIGH | Embedded PostgreSQL (`io.zonky.test`) fallback automatically initializes in `./data/pg-data` with zero user configuration. |
| **R-02** | Display clipping on $1366 \times 768$ laptops with 125% Windows scaling | HIGH | HIGH | Maximized default stage, minimum height $\le 600\text{px}$, and `ScrollPane` wrapping on all variable viewports. |
| **R-03** | Clinical safety bypass through unmonitored medication combinations | CRITICAL | LOW | Hard architectural gate in `PrescriptionService.savePrescription()` throwing `CriticalInteractionException` for unacknowledged alerts. |
| **R-04** | Typographical errors during acute medicine search | MEDIUM | HIGH | Levenshtein distance $\le 2$ string and word-token fuzzy matching with "Did you mean..." UI chips. |
| **R-05** | Corrupted multi-table writes during prescription save | HIGH | LOW | Native JDBC atomic transaction with explicit `conn.setAutoCommit(false)` and rollback in `PrescriptionDAOJdbc`. |
| **R-06** | UI thread blocking during PDF generation or background polling | MEDIUM | MEDIUM | Background daemon executor for queue polling; asynchronous file write handling. |
