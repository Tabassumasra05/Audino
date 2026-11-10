# Requirement Analysis Document
## Audino: Intelligent Clinical Decision Support & EMR System

**Document Version:** 1.0.0  
**Phase:** Requirements & Design Baseline (Phase 1)  
**Target Academic Evaluation:** 19 November 2026  
**Courses:** Object-Oriented Methodology (OOM) & Software Engineering (SE)

---

## 1. Stakeholder Analysis

The following stakeholder analysis identifies the direct and indirect participants interacting with or impacted by Audino, evaluating their organizational goals, operational pain points, and how Audino resolves each challenge.

| Stakeholder | Role in Clinical Workflow | Primary Objectives | Current Pain Points & Clinical Risks | Audino Solution |
|---|---|---|---|---|
| **Consulting Physician (`Doctor`)** | Outpatient diagnosis, clinical assessment, prescription authoring. | Rapid patient throughput; high diagnostic accuracy; error-free prescribing. | Alert fatigue from crude popups; accidental prescription of interacting drugs (e.g., Warfarin + Ibuprofen); slow EMR typing. | Keyboard-first composer; live non-blocking Safety Radar; typo-tolerant drug search; mandatory critical override protocol. |
| **Front-Desk Receptionist (`Receptionist`)** | Outpatient intake, queue coordination, vital signs collection. | Streamlined patient intake; orderly queue management; accurate baseline vitals. | Manual paper tokens causing waiting room chaos; transcription errors in vitals; duplicate patient files. | 4-step registration wizard; daily sequential token generator (`T-001`); auto BMI and physiological range checks; duplicate alerts. |
| **Clinic Administrator (`Admin`)** | Operations governance, staff credentialing, formulary curation. | Maintain regulatory compliance; ensure data security; monitor clinic throughput. | Difficulty auditing critical overrides; static, outdated formulary catalogs; unauthorized credential sharing. | Comprehensive RBAC; real-time interaction rule editor; unalterable forensic audit logs; operational KPI dashboards. |
| **Outpatient (`Patient`)** | Patient receiving medical consultation and therapy. | Clear understanding of medication schedule; minimal wait times; secure medical data. | Illegible handwritten prescriptions; confusion regarding pill timing (before/after food); lost prescription sheets. | Self-service touch Kiosk; intuitive 4-phase daily pill schedule (Morning, Afternoon, Evening, Night); downloadable PDF. |
| **Hospital Medical Director / Management** | Clinical governance, quality assurance, liability mitigation. | Prevent medical malpractice liabilities; achieve healthcare accreditation standards. | Adverse Drug Events (ADEs) leading to readmissions and legal action; lack of documented physician rationale for high-risk drugs. | Enforced override justification requirement ($\ge 15$ chars) persisted directly to audit logs and clinical records. |
| **Outpatient Pharmacist (Indirect)** | Dispensing prescribed pharmaceutical items. | Accurate dispensing; verifying dosage, frequency, and drug safety. | Deciphering physician handwriting; contacting clinic to clarify missing strength or frequency details. | Standardized RxNorm-aligned digital PDF prescriptions with explicit quantities, dosages, and daily schedules. |

---

## 2. Use Case Catalog

| Use Case ID | Use Case Name | Primary Actor | Secondary Actors | Module |
|---|---|---|---|---|
| **UC-01** | User Authentication & Dispatch | Staff Member (Any) | Audit System | Security / Auth |
| **UC-02** | Register Patient Record | Receptionist | Patient | Outpatient Intake |
| **UC-03** | Record Vitals & Issue Token | Receptionist | Patient, Queue System | Triage & Queue |
| **UC-04** | Call Next Patient | Doctor | Queue System, Patient | Clinical Consultation |
| **UC-05** | Author Prescription | Doctor | Safety Engine, Formulary | Clinical Consultation |
| **UC-06** | Override Critical Alert | Doctor | Audit System, Safety Engine | Clinical Safety |
| **UC-07** | Generate Prescription PDF | Doctor / Patient | File System / Printer | Output & Printing |
| **UC-08** | Kiosk View & Download Schedule | Patient | Token System, Verification Engine | Patient Kiosk |
| **UC-09** | Manage Staff Accounts | Admin | Database, Audit System | Administration |
| **UC-10** | Manage Interaction Rules | Admin | Safety Engine, Formulary | Clinical Governance |
| **UC-11** | Curate Formulary Catalog | Admin | Database | Administration |
| **UC-12** | Inspect Forensic Audit Logs | Admin | Database | Governance & Audit |

---

## 3. Detailed Use Case Specifications

### UC-01: User Authentication & Dispatch
- **ID:** `UC-01`
- **Primary Actor:** Staff Member (`Admin`, `Doctor`, or `Receptionist`)
- **Preconditions:** Audino is launched; database connection is established; account exists in the `accounts` table.
- **Main Success Scenario:**
  1. The staff member enters their unique username and password on the login screen.
  2. The system queries `AccountDAO.findByUsername(username)`.
  3. The system confirms the account exists and invokes `BCrypt.checkpw(enteredPassword, account.getPasswordHash())`.
  4. The system validates that `account.isActive() == true`.
  5. The system stores the account in `SessionManager.getInstance().setCurrentAccount(account)`.
  6. The system records an `AUDIT_LOGIN_SUCCESS` entry in the `audit_logs` table via `AuditService`.
  7. The system invokes `account.getDashboardFxml()` polymorphically to obtain the view resource path.
  8. `NavigationService` mounts the appropriate role dashboard shell into the primary window with a smooth fade transition.
- **Extensions / Alternate Flows:**
  - *3a. Invalid Credentials:*
    - The password hash fails to match, or username is not found.
    - System records `AUDIT_LOGIN_FAILURE` in the audit log.
    - Login card performs a subtle shake animation and displays "Invalid username or password" in red text.
  - *4a. Deactivated Account:*
    - `account.isActive()` is false.
    - System throws `AccountDisabledException`.
    - System displays modal message: "This account has been deactivated. Please contact your clinic administrator."
  - *1a. Caps Lock Active:*
    - System detects Caps Lock state and displays a warning badge adjacent to the password field.
- **Postconditions:** User is logged in, session is established, and the role-specific dashboard is active.

---

### UC-02: Register Patient Record
- **ID:** `UC-02`
- **Primary Actor:** Receptionist
- **Preconditions:** Receptionist is authenticated and viewing the Patient Registration wizard.
- **Main Success Scenario:**
  1. Receptionist inputs patient demographic details: First Name, Last Name, Date of Birth, Gender, 10-digit Phone Number, and optional Residential Address.
  2. System automatically calculates and displays the patient's age in years based on the entered Date of Birth.
  3. Receptionist navigates to the "Medical Background" step and enters confirmed drug allergies (e.g., "Penicillin", "Sulfa") and chronic medical conditions (e.g., "Peptic Ulcer", "Renal Failure", "Asthma") using the TagInput component.
  4. Receptionist clicks "Save Patient".
  5. System validates demographic inputs (phone format `^[6-9]\d{9}$`, non-empty names, valid DOB).
  6. System executes `PatientDAO.insert(patient)` and persists associated allergies and conditions.
  7. System displays a success Toast notification: "Patient registered successfully (ID: P-1024)".
- **Extensions / Alternate Flows:**
  - *5a. Duplicate Phone and Name Detected:*
    - System identifies an existing active record matching phone and name.
    - System prompts: "A patient with this name and phone already exists. Would you like to open the existing profile or continue?"
    - Receptionist may select "Open Existing" to abort duplicate creation.
  - *5b. Validation Error:*
    - Mandatory fields missing or invalid phone number entered.
    - System highlights the invalid field with inline red helper text; form submission is blocked.
- **Postconditions:** New patient record with medical history is permanently persisted in the database.

---

### UC-03: Record Visit Vitals & Issue Token
- **ID:** `UC-03`
- **Primary Actor:** Receptionist
- **Preconditions:** Patient is registered in the system; clinic session is active for the current calendar date.
- **Main Success Scenario:**
  1. Receptionist selects the registered patient and enters current visit vitals: Height (cm), Weight (kg), Systolic BP (mmHg), Diastolic BP (mmHg), Pulse (bpm), Temperature (°C), SpO₂ (%).
  2. System dynamically computes and displays BMI alongside its classification category (e.g., "22.4 kg/m² - Normal").
  3. System validates that all entered vitals lie within realistic physiological bounds.
  4. Receptionist selects the consulting doctor (or chooses "General / Any Doctor").
  5. Receptionist clicks "Issue Token".
  6. System invokes `TokenService.issueToken(patientId, doctorUsername)`:
     - Generates the next sequential token number for today (e.g., `T-014`).
     - Inserts record into `tokens` with `status = WAITING` and current timestamp.
     - Persists vitals record linked to the patient visit via `VitalsDAO`.
  7. `QueueService` notifies all registered `QueueObserver` instances; doctor and receptionist queue tables update immediately.
  8. System displays confirmation card with token number and estimated queue wait time.
- **Extensions / Alternate Flows:**
  - *3a. Physiological Out-of-Bounds Vitals:*
    - Systolic BP $< 50$ or $> 260$, Pulse $< 30$ or $> 220$, Temperature $< 34$ or $> 43$ °C, or SpO₂ $< 50$ or $> 100\%$.
    - System displays inline warning: "Extreme vitals value entered. Please re-verify."
- **Postconditions:** New token in `WAITING` status is active for the day; visit vitals are persisted.

---

### UC-04: Call Next Patient
- **ID:** `UC-04`
- **Primary Actor:** Doctor
- **Preconditions:** Doctor is logged in; there is at least one token in `WAITING` status assigned to the doctor or unassigned.
- **Main Success Scenario:**
  1. Doctor clicks the primary "Call Next" button (or presses `Ctrl + Enter`).
  2. System queries `TokenDAO.findNextWaitingToken(doctorUsername, today)`.
  3. System executes token state machine transition: `token.transitionTo(TokenStatus.IN_CONSULTATION)`.
  4. System updates token timestamp `called_at = CURRENT_TIMESTAMP` and saves via `TokenDAO`.
  5. `QueueService` notifies observers; waiting room displays reflect that token `T-014` is entering Consultation.
  6. The Doctor consultation workspace loads the patient's record:
     - Sticky Patient Banner populates with Name, ID, Age, Gender, Token No., and prominent Allergy/Condition chips.
     - Overview tab renders latest visit vitals and sparkline trends from previous visits.
     - Prescribe composer initializes to clean DRAFT state.
- **Extensions / Alternate Flows:**
  - *2a. No Patients Waiting:*
    - Queue is empty.
    - System displays an empty-state illustration with the text: "All caught up! No patients currently waiting in your queue."
- **Postconditions:** Oldest waiting token transitions to `IN_CONSULTATION`; patient medical records load into the clinical pad.

---

### UC-05: Author Prescription
- **ID:** `UC-05`
- **Primary Actor:** Doctor
- **Preconditions:** Active consultation session loaded (`TokenStatus.IN_CONSULTATION`).
- **Main Success Scenario:**
  1. Doctor focuses the Medication Search bar and begins typing a medicine brand name, generic name, or RxNorm code.
  2. System provides live instant suggestions dropdown matching the input query.
  3. Doctor selects the desired medication (e.g., "Dolo 650" - Paracetamol).
  4. The dosage field pre-populates with standard default dosage and unit.
  5. Doctor inputs/selects dosage, Frequency (e.g., "Twice daily"), Duration (e.g., "5 days"), and Food Instructions (e.g., "After food").
  6. Doctor presses `Enter` or clicks "Add Item".
  7. System invokes `medication.isValidDosage(dosage)` polymorphically:
     - If valid, creates a `PrescriptionItem` owned by the active `Prescription`.
  8. System recalculates item cost ($units \times pricePerUnit$) and updates aggregate total in INR (₹).
  9. System asynchronously triggers `SafetyEngine.evaluate()` passing active items, patient allergies, and conditions.
  10. Safety Radar displays updated count badges (Critical, Warning, Info) and alert cards.
- **Extensions / Alternate Flows:**
  - *1a. Doctor Misspells Drug Name:*
    - Doctor types "amoxcilin".
    - System calculates Levenshtein distance and character-trigram similarity against formulary entries.
    - Search field displays an inline suggestion: "Did you mean: Amoxicillin (Mox 500)?". Doctor clicks to auto-fill.
  - *3a. Medicine Has No Price in Formulary:*
    - Selected drug has `price_per_unit = null` or `0.0`.
    - System opens a modal dialog: "Missing Price: Please specify the unit price (₹) for [Drug Name] to update the clinic formulary."
    - Doctor inputs price (e.g., `4.50`). System saves to `medications` table and resumes item addition.
  - *7a. Invalid Dosage String:*
    - Doctor enters "50 tablets" for a single dose or invalid syntax.
    - System rejects entry with `InvalidDosageException` and highlights dosage field with corrective hint.
  - *8a. Item Removal & Undo:*
    - Doctor clicks trash icon to remove an item.
    - Item is removed from table; transient snackbar appears: "Removed [Drug Name]. [Undo]".
    - Clicking [Undo] restores the item and re-runs safety checks.
- **Postconditions:** Item is appended to prescription DRAFT; safety radar reflects interaction profile.

---

### UC-06: Override Critical Interaction Alert
- **ID:** `UC-06`
- **Primary Actor:** Doctor
- **Preconditions:** Prescribed items trigger at least one `CRITICAL` interaction alert in `SafetyEngine`.
- **Main Success Scenario:**
  1. System detects a critical interaction (e.g., Warfarin + Ibuprofen: "Severe risk of life-threatening gastrointestinal hemorrhage").
  2. Safety Radar highlights alert in flashing red; composer renders a prominent top warning banner: "1 Critical Interaction must be resolved before approval."
  3. The "Approve & Save" button is disabled with tooltip: "Cannot approve with unresolved critical alerts."
  4. Doctor clicks "Override" on the critical alert card.
  5. System presents the "Clinical Safety Override Justification" modal dialog, displaying drug details and risks.
  6. Doctor enters a thorough clinical justification note (e.g., "Patient monitored with daily INR; short-term co-administration indicated for acute trauma under close hospital supervision.").
  7. Doctor clicks "Confirm Override".
  8. System validates that the justification note is at least 15 non-whitespace characters in length.
  9. System marks the alert as acknowledged and records `override_note`, `overridden_by = doctor.getUsername()`, and `overridden_at = CURRENT_TIMESTAMP`.
  10. The top blocking banner clears; "Approve & Save" button activates.
- **Extensions / Alternate Flows:**
  - *8a. Insufficient Justification Note:*
    - Doctor enters a brief or trivial note (e.g., "ok", "known risk", 8 characters).
    - System displays error: "Clinical justification must be at least 15 characters to satisfy medical-legal audit requirements."
    - Modal remains open; override is not registered.
  - *1a. Doctor Resolves by Removing Drug:*
    - Doctor decides the risk is unacceptable and removes the conflicting drug.
    - `SafetyEngine` re-evaluates; the critical alert is cleared automatically.
- **Postconditions:** Critical alert is officially overridden with stored forensic justification; prescription is eligible for approval.

---

### UC-07: Generate Prescription PDF
- **ID:** `UC-07`
- **Primary Actor:** Doctor or Patient (via Kiosk)
- **Preconditions:** Prescription has been saved and approved in the database.
- **Main Success Scenario:**
  1. User clicks "Download PDF" (or "Print Prescription").
  2. System invokes `PrescriptionPdfService.generatePdf(prescriptionId, destinationPath, isDoctorCopy)`.
  3. System retrieves prescription header, doctor credentials (full name, specialization, registration number), patient vitals, and items.
  4. System formats document using iText 5:
     - Header: Clinic logo, title, contact details, date, token number.
     - Prescriber block: Doctor name, medical degree, NMC registration number.
     - Patient block: Name, Age/Gender, Patient ID, Visit Vitals (BP, Pulse, SpO₂, BMI).
     - Rx Table: Drug name, strength, dosage, frequency, duration, total count, meal instructions.
     - Chronological Daily Pill Schedule grid: 4 columns (Morning, Afternoon, Evening, Night) with timing icons and meal markers.
     - Overridden Warnings (included on Doctor/EMR copy only).
     - Doctor signature line and electronic validation stamp.
  5. System writes PDF to target file destination and displays success toast with action "Open File".
- **Extensions / Alternate Flows:**
  - *4a. File System Write Failure:*
    - Target directory is read-only or locked.
    - System catches `IOException` and prompts the user to select an alternate directory.
- **Postconditions:** Validated, high-fidelity PDF document is written to the local storage.

---

### UC-08: Kiosk View & Download Schedule
- **ID:** `UC-08`
- **Primary Actor:** Patient
- **Preconditions:** Kiosk terminal is active; patient holds a valid Token ID issued today.
- **Main Success Scenario:**
  1. Patient approaches the kiosk and taps the touch screen.
  2. Patient enters their Token ID (e.g., `T-014`) using the on-screen numeric keypad.
  3. System prompts for the second verification factor: Last 4 digits of registered phone number (or Date of Birth).
  4. Patient enters the 4 digits and presses "Access Prescription".
  5. System verifies that Token ID exists for today AND matching phone digits correspond to the token's patient.
  6. System loads and displays the patient's approved prescription:
     - Patient greeting and consulting doctor's name.
     - Large, high-contrast 4-Phase Daily Pill Schedule (Morning ☀, Afternoon ⛅, Evening ☕, Night 🌙) indicating which pill to take and whether before or after meals.
     - Clear lifestyle precautions and drug storage notes.
  7. Patient clicks "Download / Print Prescription PDF" to receive a patient-friendly copy.
  8. Patient taps the large green "Done" button.
  9. System immediately wipes displayed clinical data and returns to the initial verification screen.
- **Extensions / Alternate Flows:**
  - *5a. Verification Mismatch:*
    - Token number does not match phone digits, or token does not exist.
    - System increments failed attempts counter.
    - Displays: "Verification failed. Please check your token number and phone digits."
  - *5b. Brute-Force Lockout (3 Failed Attempts):*
    - Patient enters invalid credentials 3 consecutive times.
    - System locks the kiosk keypad for 60 seconds, displaying a prominent countdown timer.
  - *8a. Patient Walks Away (Inactivity Timeout):*
    - Patient leaves the terminal without clicking "Done".
    - After 120 seconds of zero touch/keyboard activity, system automatically triggers the "Done" handler to preserve medical confidentiality.
- **Postconditions:** Patient successfully reviews medication schedule; session terminates securely without residual data exposure.

---

### UC-09: Manage Staff Accounts
- **ID:** `UC-09`
- **Primary Actor:** Admin
- **Preconditions:** Admin is authenticated and viewing the Staff Management module.
- **Main Success Scenario:**
  1. Admin views list of all staff accounts with status chips, roles, and action buttons.
  2. Admin clicks "Add New Staff".
  3. Admin fills account form: Username, Full Name, Role (`Admin`, `Doctor`, `Receptionist`), Temporary Password.
  4. If `Doctor` role is chosen, system reveals mandatory fields: Medical Specialization and Medical Registration Number.
  5. Admin clicks "Create Account".
  6. System hashes password using jBCrypt (cost 12), invokes `AccountFactory.createAccount(...)`, and persists record via `AccountDAO`.
  7. System writes `AUDIT_STAFF_CREATE` event into `audit_logs`.
  8. Staff table refreshes immediately; toast notification confirms account provisioning.
- **Extensions / Alternate Flows:**
  - *5a. Duplicate Username:*
    - Username already exists in `accounts`.
    - System reports: "Username '[username]' is already taken. Please choose another."
  - *1a. Admin Attempts Self-Deactivation:*
    - Admin clicks "Deactivate" on their own username.
    - System intercepts request and displays modal error: "Security Restriction: Administrators cannot deactivate their own active account."
- **Postconditions:** Staff member account is created, modified, or status-toggled in compliance with RBAC rules.

---

### UC-10: Manage Interaction Rules
- **ID:** `UC-10`
- **Primary Actor:** Admin
- **Preconditions:** Admin is authenticated and viewing the Clinical Interaction Rules panel.
- **Main Success Scenario:**
  1. Admin inspects list of existing interaction rules with severity badges (`CRITICAL`, `WARNING`, `INFO`).
  2. Admin clicks "New Interaction Rule".
  3. Admin specifies: Rule Type (`DRUG_DRUG`, `DRUG_ALLERGY`, `DRUG_CONDITION`), Keyword 1 (e.g., "Aspirin"), Keyword 2 (e.g., "Warfarin"), Severity Level, Clinical Description, and Recommendation.
  4. Admin clicks "Save Rule".
  5. System persists the rule to `interaction_rules` table via `InteractionRuleDAO`.
  6. System triggers immediate cache invalidation and reload in `SafetyEngine`.
  7. System records `AUDIT_RULE_CREATE` in audit logs.
  8. The new rule is immediately active in all open prescribing pads across the clinic.
- **Postconditions:** New clinical safety rule is active without requiring application reboot.

---

## 4. Business Rule Matrix

| Rule ID | Rule Statement | Enforcement Component | Violation Consequence |
|---|---|---|---|
| **BR-01** | Passwords must never be stored in cleartext and must use adaptive salted hashing. | `BCryptPasswordHasher` | Reject plain storage; enforce BCrypt work factor 12. |
| **BR-02** | An active CRITICAL interaction cannot be approved without a justification note of $\ge 15$ characters. | `PrescriptionService.save()` | Throws `CriticalInteractionException`; transaction rolled back. |
| **BR-03** | Daily outpatient tokens must reset sequence to `T-001` at 00:00 each calendar day. | `TokenDAOJdbc.getNextTokenNo()` | Guarantees daily sequential domain numbering. |
| **BR-04** | A token may only transition along legal directed graph states (`WAITING → IN_CONSULTATION → COMPLETED`, `WAITING → CANCELLED`). | `TokenStatus.canTransitionTo()` | Throws `InvalidTokenStateException`; transition aborted. |
| **BR-05** | Doctors must possess a valid Medical Council Registration Number to approve prescriptions. | `PrescriptionPdfService`, `AccountDAO` | Required field on `Doctor` entity; printed on official output. |
| **BR-06** | Prescription item deletion must support immediate single-step undo. | `PrescriptionController` | Restores removed `PrescriptionItem` and re-evaluates safety radar. |
| **BR-07** | Deactivated staff accounts must be refused immediate access. | `AuthService.authenticate()` | Throws `AccountDisabledException`; login rejected. |
| **BR-08** | Multi-table prescription saving (header, items, alerts, token update, audit log) must be strictly atomic. | `PrescriptionDAOJdbc` | Enclosed in single JDBC transaction; rollback on error. |
