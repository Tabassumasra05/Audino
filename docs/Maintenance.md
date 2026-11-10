# Audino EMR: System Maintenance & Developer Extensibility Manual

**Document Version:** 1.0.0  
**Target Evaluation Date:** 19-Nov-2026  
**Audience:** Software Engineers, System Administrators, Clinical Informaticians  
**System:** Audino: Intelligent Clinical Decision Support & EMR System  

---

## 1. Introduction & Maintenance Architecture

Audino is engineered for long-term clinical evolvability. As new pharmacotherapies emerge and clinical guidelines update, the software must adapt without architectural rot or regressions. 

By strictly adhering to the **SOLID principles** (specifically the **Open/Closed Principle** and the **Dependency Inversion Principle**), developers can extend clinical functionality with minimal friction and zero risk to existing modules.

---

## 2. Extensibility Guide: Adding New Features

### 2.1 Adding a New Clinical Interaction Strategy (Open/Closed Principle)
Audino's Clinical Decision Support engine (`SafetyEngine`) can ingest new safety dimensions (e.g. Renal Dosing, Pediatric Age Bounds, Pregnancy Contraindications) without altering `SafetyEngine.java`.

#### Step-by-Step Procedure:
1. Implement the `InteractionStrategy` interface in `com.audino.service.strategy`:
   ```java
   package com.audino.service.strategy;

   import com.audino.model.AlertLevel;
   import com.audino.model.AlertType;
   import com.audino.model.Patient;
   import com.audino.model.Prescription;
   import com.audino.model.PrescriptionAlert;
   import com.audino.model.PrescriptionItem;

   import java.util.ArrayList;
   import java.util.List;

   public class RenalClearanceStrategy implements InteractionStrategy {

       @Override
       public List<PrescriptionAlert> check(Prescription prescription, Patient patient) {
           List<PrescriptionAlert> alerts = new ArrayList<>();
           // Custom clinical evaluation:
           // e.g. If patient has CKD or eGFR < 30 and medication is renally cleared:
           for (PrescriptionItem item : prescription.getItems()) {
               if (item.getMedication().getBrandName().equalsIgnoreCase("Metformin")) {
                   alerts.add(new PrescriptionAlert(
                       AlertLevel.CRITICAL,
                       AlertType.DRUG_CONDITION,
                       "Renal Contraindication: Metformin requires eGFR > 30 mL/min.",
                       "Discontinue Metformin and substitute with Insulin or DPP-4 inhibitor."
                   ));
               }
           }
           return alerts;
       }
   }
   ```
2. Register the new strategy with the `SafetyEngine`:
   ```java
   safetyEngine.registerStrategy(new RenalClearanceStrategy());
   ```
3. *Verification:* The `SafetyEngine` immediately evaluates the new strategy during outpatient prescribing. (Verified in `SafetyEngineTest.java`).

---

### 2.2 Adding a New Medication Form (Delivery Mechanism)
To support a new delivery mechanism (e.g., Inhalers, Transdermal Patches):

1. **Update Enum:** Add the constant to `com.audino.model.MedicationType`:
   ```java
   INHALER("Inhaler / Metered Dose", "puffs")
   ```
2. **Subclass Medication:** Create `InhalerMedication.java` extending `Medication`:
   ```java
   public class InhalerMedication extends Medication {
       public InhalerMedication(...) { super(...); }

       @Override
       public boolean isValidDosage(String dosage) {
           // Validate "1 puff", "2 puffs"
           return dosage.toLowerCase().matches("^\\d+\\s*(puff|puffs|inhalation)$");
       }

       @Override
       public String getUnitLabel() { return "canisters"; }

       @Override
       public int calculateUnits(String dosage, int timesPerDay, int days) {
           // Calculate total actuations and round up to whole canisters (200 puffs/canister)
           int totalPuffs = parsePuffs(dosage) * timesPerDay * days;
           return (int) Math.ceil(totalPuffs / 200.0);
       }
   }
   ```
3. **Update Factory:** Add the branch in `MedicationFactory.createMedication()`.
4. *Zero UI Alteration:* The UI components, controllers, and PDF generators polymorphically query `med.calculateUnits()`, `med.getUnitLabel()`, and `med.isValidDosage()` without any code changes.

---

### 2.3 Adding a New Staff Role
To introduce a new role (e.g., `Pharmacist`, `LabTechnician`):
1. Add constant to `Role.java`.
2. Create subclass in `com.audino.model` (e.g., `Pharmacist extends Account`).
3. Implement polymorphic hooks:
   - `getDashboardFxml()`: return `/fxml/pharmacy_dashboard.fxml`.
   - `getPermissions()`: return `Set.of("DISPENSE_MEDICATION", "VIEW_FORMULARY")`.
4. Update `AccountFactory.createAccount()`.

---

## 3. Database Administration & Operations

### 3.1 Switching Database Modes
Configuration is driven by `src/main/resources/application.properties`:
```properties
# Database Connectivity Configuration
# If localhost:5432 is running, Audino attaches automatically.
# Otherwise, it boots Embedded PostgreSQL into ./data/pg-data
db.host=localhost
db.port=5432
db.name=audino
db.username=postgres
db.password=postgres
db.pool.max=10
db.pool.min=2
```

### 3.2 Clean Data Reset Procedure
To reset the clinical database to its clean, initial seeded state:
1. Stop the application.
2. Delete the `./data/` folder:
   ```bash
   Remove-Item -Recurse -Force ./data
   ```
3. If using an external PostgreSQL instance:
   ```sql
   DROP DATABASE audino;
   ```
4. Rerun `mvn clean javafx:run`. The system will automatically recreate the database, rebuild all 13 relational tables, and seed the demo clinical dataset.

---

## 4. Troubleshooting & Operational Guide

| Symptom | Probable Cause | Remediation Procedure |
|---|---|---|
| Port 5432 Conflict | External PG running with different credentials | Update `application.properties` with correct password, or stop the external service to let Embedded PG boot automatically. |
| Font Rendering Warning | Missing Inter TTF files on classpath | Verify that `Inter.ttf` exists in `src/main/resources/fonts/`. Audino falls back gracefully to system sans-serif. |
| Outdated Test Classes | Target folder stale | Execute `mvn clean test` to recompile all classes from clean bytecode. |
| Stage Not Visible | Headless environment | JavaFX requires a physical or virtual display server (`DISPLAY` on Linux, active session on Windows). |
