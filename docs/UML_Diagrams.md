# UML Modeling & Architecture Diagrams
## Audino: Intelligent Clinical Decision Support & EMR System

**Document Version:** 1.0.0  
**Phase:** Requirements & Design Baseline (Phase 1)  
**Academic Modules:** Object-Oriented Methodology (OOM) & Software Engineering (SE)

---

## 1. Overview and Rendering Instructions

This document compiles the formal Unified Modeling Language (UML) structural, behavioral, and architectural models designed for **Audino**. The models adhere to the official UML 2.5 specification and are maintained as version-controlled `.puml` files in the [`docs/diagrams/`](file:///c:/Users/tabas/Downloads/clinicalprescriber/docs/diagrams/) directory.

### Rendering PlantUML Diagrams to High-Resolution PNG/SVG:
To compile the raw `.puml` source files into vector images:
1. **Via PlantUML CLI:**
   ```bash
   # Download plantuml.jar and graphviz (dot)
   java -jar plantuml.jar -tpng docs/diagrams/*.puml
   ```
2. **Via Visual Studio Code / JetBrains IDE:**
   Install the official **PlantUML** extension (`jebbs.plantuml`), open any `.puml` file, and press `Alt + D` to preview or right-click to export.
3. **Via Online Server:**
   Paste the PlantUML code directly into [PlantText](https://www.planttext.com/) or the official [PlantUML Web Server](http://www.plantuml.com/plantuml/).

---

## 2. Use Case Model

### 2.1 Use Case Diagram Description
The use case diagram illustrates the functional boundaries of the system across four human user classes (`Administrator`, `Doctor`, `Receptionist`, `Patient`) inheriting from an abstract `Staff Member` actor where appropriate, and one external `Audit System` actor. The diagram employs `<<include>>` and `<<extend>>` stereotyping to represent mandatory sub-routines (e.g., duplicate checking during registration, safety radar during prescription addition) and optional/conditional pathways (e.g., brute-force lockout during kiosk authentication, typo autocorrect).

### 2.2 PlantUML Specification
```plantuml
@startuml use_case_diagram
title Audino EMR - Comprehensive Use Case Diagram
left to right direction
skinparam packageStyle rectangle
skinparam shadowing false
skinparam defaultFontName "Inter"
skinparam defaultFontSize 12

actor "Staff Member" as Staff <<Abstract>>
actor "Administrator" as Admin
actor "Doctor" as Doctor
actor "Receptionist" as Receptionist
actor "Patient" as Patient
actor "Audit System" as System <<System>>

Staff <|-- Admin
Staff <|-- Doctor
Staff <|-- Receptionist

rectangle "Audino Clinical Decision Support & EMR System" {
    package "Authentication & Profile" {
        usecase "Login with Credentials" as UC_Login
        usecase "Change Password" as UC_ChangePass
        usecase "Dispatch Role Dashboard" as UC_Dispatch
        usecase "Log Authentication Event" as UC_AuditAuth
    }

    package "Patient Administration & Triage" {
        usecase "Register Patient Profile" as UC_RegPatient
        usecase "Capture Visit Vitals & BMI" as UC_Vitals
        usecase "Issue Daily Outpatient Token" as UC_IssueToken
        usecase "Check Duplicate Patient" as UC_DupCheck
    }

    package "Clinical Consultation & Prescribing" {
        usecase "Call Next Patient" as UC_CallNext
        usecase "Review Patient Medical Banner" as UC_ReviewBanner
        usecase "Search Formulary Medications" as UC_SearchMed
        usecase "Perform Typo Autocorrect" as UC_Autocorrect
        usecase "Add Prescription Line Item" as UC_AddItem
        usecase "Run Clinical Safety Engine" as UC_SafetyCheck
        usecase "Override Critical Interaction" as UC_Override
        usecase "Record Clinical Justification" as UC_Justification
        usecase "Approve & Save Prescription" as UC_ApproveRx
        usecase "Complete Consultation" as UC_CompleteConsult
    }

    package "Self-Service Patient Kiosk" {
        usecase "Authenticate Token & Phone/DOB" as UC_KioskAuth
        usecase "View 4-Phase Daily Pill Schedule" as UC_ViewSchedule
        usecase "Download Prescription PDF" as UC_DownloadPdf
        usecase "Enforce Brute-Force Lockout" as UC_Lockout
    }

    package "Formulary & Governance" {
        usecase "Manage Staff Accounts" as UC_ManageStaff
        usecase "Maintain Drug Formulary" as UC_Formulary
        usecase "Define Interaction Rules" as UC_Rules
        usecase "Inspect Forensic Audit Trail" as UC_AuditTrail
        usecase "View Clinical Operational KPI" as UC_KPI
    }
}

Staff --> UC_Login
Staff --> UC_ChangePass
UC_Login ..> UC_Dispatch : <<include>>
UC_Login ..> UC_AuditAuth : <<include>>

Admin --> UC_ManageStaff
Admin --> UC_Formulary
Admin --> UC_Rules
Admin --> UC_AuditTrail
Admin --> UC_KPI

Receptionist --> UC_RegPatient
Receptionist --> UC_Vitals
Receptionist --> UC_IssueToken
UC_RegPatient ..> UC_DupCheck : <<include>>
UC_IssueToken ..> UC_Vitals : <<include>>

Doctor --> UC_CallNext
Doctor --> UC_ReviewBanner
Doctor --> UC_AddItem
Doctor --> UC_Override
Doctor --> UC_ApproveRx
Doctor --> UC_CompleteConsult

UC_AddItem ..> UC_SearchMed : <<include>>
UC_SearchMed ..> UC_Autocorrect : <<extend>>
UC_AddItem ..> UC_SafetyCheck : <<include>>
UC_Override ..> UC_Justification : <<include>>
UC_ApproveRx ..> UC_SafetyCheck : <<include>>

Patient --> UC_KioskAuth
Patient --> UC_ViewSchedule
Patient --> UC_DownloadPdf
UC_KioskAuth ..> UC_Lockout : <<extend>>
UC_ViewSchedule ..> UC_KioskAuth : <<include>>

UC_Override ..> System : <<notifies>>
UC_ApproveRx ..> System : <<notifies>>
@enduml
```

---

## 3. Structural Class Diagram (OOM Rubric Showcase)

### 3.1 Class Architecture & Relationships
The class diagram demonstrates rigorous object-oriented design principles:
1. **Inheritance & Abstraction Hierarchy 1 (`Account`):**
   - Abstract base `Account` encapsulates security identity (`username`, `passwordHash`, `role`, `active`).
   - Declares pure abstract methods: `getDashboardFxml()` and `getPermissions()`.
   - Subclasses: `Admin`, `Doctor` (adding clinical specialization and NMC registration number), and `Receptionist`.
2. **Inheritance & Abstraction Hierarchy 2 (`Medication`):**
   - Abstract base `Medication` encapsulates pharmacopeia metadata (`genericName`, `brandName`, `rxNormCode`, `strength`, `pricePerUnit`).
   - Declares pure polymorphic operations: `isValidDosage(String)`, `getUnitLabel()`, and `calculateUnits(dosage, timesPerDay, days)`.
   - Subclasses: `TabletMedication`, `CapsuleMedication`, `LiquidMedication` (dosed in ml), `InjectionMedication` (vials/ml).
3. **UML Multiplicities and Relationships:**
   - **Composition (`*--`):** `Prescription` strictly owns its `PrescriptionItem`s and `PrescriptionAlert`s. If a prescription draft is deleted, its items and alerts cease to exist. Similarly, `Patient` owns `PatientAllergy` and `PatientCondition`.
   - **Aggregation (`o--`):** `Prescription` aggregates `Medication` references. Medications exist independently in the clinic formulary and outlive any single prescription.
   - **Association (`--`):** `Doctor` associates with `Patient` through `Token` consultations and `Prescription.prescribedBy`.
4. **Design Pattern Integration:**
   - **Strategy:** `InteractionStrategy` interface with four concrete strategy implementations.
   - **Observer:** `QueueSubject` / `QueueObserver` for real-time queue broadcasting.
   - **DAO:** Interface-driven persistence abstraction with pure JDBC implementations.
   - **Factory:** `AccountFactory`, `MedicationFactory`, `AlertFactory`.
   - **Singleton:** `ConfigurationManager` (HikariCP pool) and `SessionManager`.

### 3.2 PlantUML Specification
See [`docs/diagrams/class_diagram.puml`](file:///c:/Users/tabas/Downloads/clinicalprescriber/docs/diagrams/class_diagram.puml) for the full source.

---

## 4. Behavioral Sequence Diagrams

### 4.1 Sequence Diagram 1: Prescribing Conflict Detection, Override Justification, and Atomic Save
This scenario models the critical clinical safety pathway:
1. A doctor enters `Warfarin 5mg` while `Ibuprofen` is already present on the prescription.
2. The `PrescriptionController` triggers the `SafetyEngine`.
3. The engine iterates over registered `InteractionStrategy` objects.
4. `DrugDrugStrategy` executes keyword matching against active database rules and flags a `CRITICAL` interaction.
5. The `AlertFactory` instantiates an alert UI model; the prescription pad disables the save button and renders a blocking top banner.
6. The doctor acknowledges the clinical necessity and provides a justification note exceeding the 15-character regulatory requirement.
7. Upon clicking `Approve & Save`, `PrescriptionService` validates the override note, opens an explicit JDBC transaction, writes the prescription header, items, alerts, updates token status, and writes an entry to `audit_logs`.

```plantuml
@startuml sequence_diagram_prescription_safety_override
title Sequence Diagram 1: Prescribing Conflict Detection, Override Justification, and Atomic Save
autonumber
skinparam defaultFontName "Inter"

actor Doctor
boundary "DoctorConsultationView\n(Prescription Pad)" as View
control "PrescriptionController" as Controller
control "SafetyEngine" as SafetyEngine
entity "DrugDrugStrategy" as Strategy
entity "AlertFactory" as AlertFactory
control "PrescriptionService" as PrescService
database "PostgreSQL DB\n(Transaction)" as DB
control "AuditService" as AuditService

== Step 1: Doctor Prescribes Interacting Drug (Warfarin + Ibuprofen) ==
Doctor -> View : Inputs 'Warfarin 5mg' & clicks 'Add Item'
View -> Controller : handleAddItem(medication, dosage, frequency)
Controller -> Controller : prescription.addItem(newItem)

== Step 2: Asynchronous Safety Engine Evaluation ==
Controller -> SafetyEngine : evaluate(prescription, patient)
activate SafetyEngine

loop for each registered InteractionStrategy
    SafetyEngine -> Strategy : check(items, patient, activeRules)
    activate Strategy
    Strategy -> Strategy : matchKeywords("Warfarin", "Ibuprofen")
    Strategy --> SafetyEngine : return [PrescriptionAlert(CRITICAL, "Warfarin + Ibuprofen: Risk of severe hemorrhage")]
    deactivate Strategy
end

SafetyEngine --> Controller : return List<PrescriptionAlert>
deactivate SafetyEngine

Controller -> AlertFactory : createAlertCard(criticalAlert)
activate AlertFactory
AlertFactory --> Controller : return AlertCard (Red Badge, Mandatory Override)
deactivate AlertFactory

Controller -> View : renderSafetyRadar(alerts)\nrenderBlockingBanner("1 Critical Alert requires resolution")
View -> View : disableSaveButton(true)

== Step 3: Clinical Override Protocol ==
Doctor -> View : Clicks 'Override' on Critical Alert Card
View -> Controller : openOverrideDialog(criticalAlert)
Doctor -> View : Enters Justification Note: "Daily INR monitored; short-term trauma use" (>= 15 chars)
Doctor -> View : Confirms Override
View -> Controller : handleConfirmOverride(alertId, justificationNote)

Controller -> Controller : alert.setOverrideNote(justificationNote)\nalert.setOverriddenBy(doctor.getUsername())\nalert.setAcknowledged(true)
Controller -> View : updateSafetyRadar()\nclearBlockingBanner()\nenableSaveButton(true)

== Step 4: Atomic Persistence & Forensic Audit ==
Doctor -> View : Clicks 'Approve & Save' (Ctrl+S)
View -> Controller : handleApprovePrescription()
Controller -> PrescService : savePrescription(prescription, doctor)
activate PrescService

PrescService -> PrescService : validateNoUnjustifiedCriticals(prescription)
PrescService -> DB : beginTransaction()
activate DB
PrescService -> DB : INSERT INTO prescriptions (...)
PrescService -> DB : INSERT INTO prescription_items (...) [Cascade / Batch]
PrescService -> DB : INSERT INTO prescription_alerts (...) [Includes override note & doctor]
PrescService -> DB : UPDATE tokens SET status = 'IN_CONSULTATION' WHERE id = ...
PrescService -> DB : commitTransaction()
deactivate DB

PrescService -> AuditService : logCriticalOverride(doctor, alert, justificationNote)
activate AuditService
AuditService -> DB : INSERT INTO audit_logs (CRITICAL_OVERRIDE, ...)
deactivate AuditService

PrescService -> AuditService : logPrescriptionSaved(doctor, prescription.getId())
activate AuditService
AuditService -> DB : INSERT INTO audit_logs (PRESCRIPTION_APPROVED, ...)
deactivate AuditService

PrescService --> Controller : return savedPrescription
deactivate PrescService

Controller -> View : showSuccessToast("Prescription approved & saved")\nupdateUiState(APPROVED)
@enduml
```

---

### 4.2 Sequence Diagram 2: Outpatient Token Issuance & Observer Notification
This scenario models real-time queue synchronization:
1. Receptionist captures vitals and issues token `T-014`.
2. `QueueService` acts as a `QueueSubject`.
3. It updates the database via `TokenDAO` and notifies all registered `QueueObserver` instances.
4. Each observer schedules its UI refresh on the JavaFX Application Thread via `Platform.runLater()`.
5. Background scheduled polling allows decoupled instances on other clinic machines to stay synchronized every 3 seconds.

```plantuml
@startuml sequence_diagram_queue_token_observer
title Sequence Diagram 2: Outpatient Token Issuance & Real-Time Observer Notification
autonumber
skinparam defaultFontName "Inter"

actor Receptionist
boundary "ReceptionistQueueView" as RecView
control "ReceptionistController" as RecController
control "QueueService\n(QueueSubject)" as QueueService
entity "TokenDAO" as TokenDAO
database "PostgreSQL DB" as DB
control "DoctorQueueObserver\n(QueueObserver)" as DocObserver
boundary "DoctorPortalView" as DocView

== Step 1: Token Issuance at Reception ==
Receptionist -> RecView : Selects patient & clicks 'Issue Token'
RecView -> RecController : handleIssueToken(patientId, doctorUsername)
RecController -> QueueService : issueToken(patientId, doctorUsername)
activate QueueService

QueueService -> TokenDAO : generateNextTokenNo(today)
activate TokenDAO
TokenDAO -> DB : SELECT MAX(token_no) FROM tokens WHERE token_date = CURRENT_DATE
DB --> TokenDAO : return "T-013"
TokenDAO --> QueueService : return "T-014"
deactivate TokenDAO

QueueService -> TokenDAO : save(Token("T-014", patientId, doctorUsername, WAITING))
activate TokenDAO
TokenDAO -> DB : INSERT INTO tokens (...) VALUES ('T-014', ...)
DB --> TokenDAO : success
TokenDAO --> QueueService : return Token(id=1014, status=WAITING)
deactivate TokenDAO

== Step 2: Subject Notifies Registered Observers ==
QueueService -> QueueService : notifyObservers()

loop for each registered QueueObserver
    QueueService -> DocObserver : onQueueUpdated(latestTokensList)
    activate DocObserver
    DocObserver -> DocObserver : Platform.runLater(() -> refreshQueueUi(latestTokensList))
    DocObserver -> DocView : updateTokenTable(latestTokensList)
    DocView -> DocView : playSubtleHighlightAnimation("T-014")
    deactivate DocObserver
end

QueueService --> RecController : return Token(id=1014)
deactivate QueueService

RecController -> RecView : showSuccessToast("Token T-014 issued")\nrenderTokenReceiptCard("T-014")

== Step 3: Background Scheduled Polling (Cross-Instance Sync) ==
QueueService -> DB : SELECT * FROM tokens WHERE token_date = CURRENT_DATE
DB --> QueueService : return token snapshot
QueueService -> QueueService : if (hasQueueChanged(snapshot)) { notifyObservers(); }
@enduml
```

---

## 5. Activity Diagram: Outpatient Clinical Lifecycle

The activity diagram models the end-to-end patient progression across 4 swimlanes: `Patient / Kiosk`, `Receptionist Desk`, `Consulting Doctor`, and `Audino Engine`.

```plantuml
@startuml activity_diagram_patient_lifecycle
title Activity Diagram: End-to-End Outpatient Clinical Lifecycle
skinparam defaultFontName "Inter"

|#F8FAFC|Patient / Kiosk|
|#F1F5F9|Receptionist Desk|
|#E2E8F0|Consulting Doctor|
|#CBD5E1|Audino Engine (Safety & Persistence)|

|Patient / Kiosk|
start
:Patient arrives at clinic;

|Receptionist Desk|
:Receptionist searches patient database;
if (Patient already registered?) then (yes)
    :Retrieve existing patient record;
else (no)
    :Enter demographics (Name, DOB, Phone, Address);
    :Input known drug allergies & chronic conditions;
    :Save new patient record;
endif

:Record visit vitals\n(Height, Weight, BP, Pulse, Temp, SpO2);
:Auto-calculate BMI & validate physiological limits;
:Select consulting doctor & issue daily token (e.g. T-001);

|Audino Engine (Safety & Persistence)|
:Persist token (Status: WAITING);
:Notify QueueObservers across clinic network;

|Doctor|
:Doctor clicks 'Call Next' (Ctrl+Enter);

|Audino Engine (Safety & Persistence)|
:Transition token to IN_CONSULTATION;
:Load patient medical profile & past visit history;

|Doctor|
:Review patient banner (Allergies & Conditions chips);
:Conduct physical examination & history taking;

repeat
    :Search medication in formulary\n(Typo-tolerant autocomplete);
    :Select drug, specify dosage, frequency, & duration;
    
    |Audino Engine (Safety & Persistence)|
    :Validate dosage polymorphically;
    :Execute SafetyEngine strategies;
    
    |Doctor|
    if (Safety Alerts Triggered?) then (yes)
        if (Alert Level is CRITICAL?) then (yes)
            :View Critical Alert on Safety Radar;
            if (Can substitute or remove drug?) then (yes)
                :Remove offending medication;
            else (no - clinical necessity)
                :Click 'Override Alert';
                :Enter clinical justification (>= 15 characters);
                :Submit override audit rationale;
            endif
        else (WARNING / INFO)
            :Review recommendation & acknowledge;
        endif
    else (no)
        :Clean safety status;
    endif
repeat while (More medications to prescribe?) is (yes)
->no;

:Doctor clicks 'Approve & Save' (Ctrl+S);

|Audino Engine (Safety & Persistence)|
:Atomic transaction: Save prescription, items, alerts, audit log;
:Generate digital pill schedule (Morning/Afternoon/Evening/Night);

|Doctor|
:Doctor clicks 'Complete Consultation';

|Audino Engine (Safety & Persistence)|
:Transition token to COMPLETED;
:Generate official Prescription PDF;

|Patient / Kiosk|
:Patient proceeds to waiting area / Self-Service Kiosk;
:Enter Token ID & verify with Phone Last-4 or DOB;
:View 4-phase daily pill schedule on high-contrast screen;
:Download / Print Prescription PDF;
:Tap 'Done' or allow 120s inactivity auto-reset;

stop
@enduml
```

---

## 6. State Machine Diagrams

### 6.1 Token Lifecycle State Machine
```plantuml
@startuml state_diagram_token
title State Machine Diagram: Outpatient Token Lifecycle
skinparam defaultFontName "Inter"
skinparam state {
    BackgroundColor #F8FAFC
    BorderColor #0F766E
    FontColor #0F172A
    ArrowColor #0F766E
}

[*] --> WAITING : issueToken()\n[Assigned sequential T-XXX]

state WAITING {
    WAITING : Token queued for today's clinic session
    WAITING : Observable by Receptionist & Doctor
}

WAITING --> IN_CONSULTATION : callNext() /\nconsultingDoctor = loggedInDoctor,\ncalled_at = CURRENT_TIMESTAMP

state IN_CONSULTATION {
    IN_CONSULTATION : Patient currently with doctor
    IN_CONSULTATION : Clinical pad actively editing
}

IN_CONSULTATION --> COMPLETED : completeConsultation() /\n[Prescription Approved OR 'No Rx Needed'],\ncompleted_at = CURRENT_TIMESTAMP

state COMPLETED {
    COMPLETED : Consultation finished
    COMPLETED : Accessible at Patient Kiosk
}

WAITING --> CANCELLED : cancelToken() /\n[Patient left or no-show],\ncancelled_at = CURRENT_TIMESTAMP

state CANCELLED {
    CANCELLED : Token revoked
    CANCELLED : Ineligible for consultation
}

COMPLETED --> [*]
CANCELLED --> [*]

note right of WAITING
    <b>Illegal Transitions (throw InvalidTokenStateException):</b>
    * WAITING -> COMPLETED (cannot bypass consultation)
    * IN_CONSULTATION -> WAITING (cannot return to queue)
    * COMPLETED -> ANY (terminal state)
    * CANCELLED -> ANY (terminal state)
end note
@enduml
```

### 6.2 Prescription Lifecycle State Machine
```plantuml
@startuml state_diagram_prescription
title State Machine Diagram: Clinical Prescription Lifecycle
skinparam defaultFontName "Inter"
skinparam state {
    BackgroundColor #F8FAFC
    BorderColor #0F766E
    FontColor #0F172A
    ArrowColor #0F766E
}

[*] --> DRAFT : createPrescription()\n[Associated with Patient & Token]

state DRAFT {
    DRAFT : Items added, modified, or removed
    DRAFT : Real-time Safety Radar executing
    DRAFT : Dynamic pricing recalculating
    
    state "Evaluating Safety" as EvalSafety
    state "Critical Alert Blocking" as CritBlock
    state "Ready for Approval" as Ready
    
    [*] --> EvalSafety
    EvalSafety --> CritBlock : Critical alert detected\n[requires justification note >= 15 chars]
    CritBlock --> Ready : Critical overridden OR drug removed
    EvalSafety --> Ready : No critical alerts detected
}

DRAFT --> APPROVED : approveAndSave() /\n[Guard: No unjustified criticals],\n[Prescriber = logged-in doctor],\napproved_at = CURRENT_TIMESTAMP

state APPROVED {
    APPROVED : Immutable legal document
    APPROVED : Printed with Doctor Reg Number
    APPROVED : Renderable at Patient Kiosk
}

DRAFT --> CANCELLED : cancelPrescription() /\n[Consultation discarded or abandoned]

state CANCELLED {
    CANCELLED : Discarded clinical draft
}

APPROVED --> [*]
CANCELLED --> [*]

note bottom of APPROVED
    <b>Integrity Invariants:</b>
    * Cannot edit items once APPROVED.
    * Persisted atomically with items, alerts, and audit log.
    * Only APPROVED prescriptions generate patient Kiosk pill schedules.
end note
@enduml
```

---

## 7. Relational Database Entity-Relationship (ER) Diagram

See [`docs/diagrams/er_diagram.puml`](file:///c:/Users/tabas/Downloads/clinicalprescriber/docs/diagrams/er_diagram.puml) for the relational schema representing 12 core tables: `accounts`, `patients`, `patient_allergies`, `patient_conditions`, `patient_vitals`, `medications`, `medication_ingredients`, `interaction_rules`, `tokens`, `prescriptions`, `prescription_items`, `prescription_alerts`, and `audit_logs`.

---

## 8. Component & Deployment Architecture

See [`docs/diagrams/component_and_deployment_diagram.puml`](file:///c:/Users/tabas/Downloads/clinicalprescriber/docs/diagrams/component_and_deployment_diagram.puml) illustrating the multi-tiered JavaFX desktop application architecture, integration with the HikariCP connection pool, and the dual deployment model (External PostgreSQL port 5432 vs. Zero-Setup Embedded PostgreSQL).
