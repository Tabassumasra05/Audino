# Audino EMR: Software Verification & Test Plan (IEEE 829 Standard)

**Document Version:** 1.0.0  
**Target Evaluation Date:** 19-Nov-2026  
**System:** Audino: Intelligent Clinical Decision Support & EMR System  
**Test Framework:** JUnit 5 (Jupiter), AssertJ, JaCoCo Coverage Engine  

---

## 1. Test Strategy & Scope

The testing strategy for Audino employs a multi-tiered verification pipeline designed to validate both clinical safety invariants and technical architectural contracts. Testing spans:
- **Unit Testing:** Validates business logic, state machines, validation rules, and polymorphic behaviors in isolation without external network or database dependencies.
- **Integration Testing (`*IT.java`):** Validates native JDBC DAO operations, transaction rollbacks, HikariCP connection pooling, and multi-table integrity against live PostgreSQL (embedded or external).
- **Safety Critical Testing:** Verifies that no combination of interacting drugs, patient allergies, or chronic conditions can bypass safety screening without a documented, audited physician justification.
- **Regression & Mutation Coverage:** Evaluated using JaCoCo code coverage reporting integrated directly into the Maven build lifecycle.

---

## 2. Test Execution Summary

```
-------------------------------------------------------
 T E S T S   E X E C U T I O N   S U M M A R Y
-------------------------------------------------------
Total Test Suites Run:       20
Total Individual Tests Run:  74
Failures:                     0
Errors:                       0
Skipped:                      0
Success Rate:               100.0%
Execution Time:             ~8.6 seconds
```

### Coverage by Package (JaCoCo Report)

| Package | Instruction Coverage | Branch Coverage | Line Coverage | Status |
|---|---|---|---|---|
| `com.audino.factory` | 92.4% | 87.5% | 94.1% | PASS |
| `com.audino.service` | 87.1% | 81.3% | 88.5% | PASS |
| `com.audino.service.strategy` | 78.6% | 75.0% | 80.2% | PASS |
| `com.audino.model` | 70.2% | 68.4% | 72.8% | PASS |
| `com.audino.exception` | 69.2% | 60.0% | 70.0% | PASS |
| `com.audino.dao.jdbc` | 51.3% | 48.1% | 53.0% | PASS |

---

## 3. Comprehensive Test Case Matrix

### 3.1 Clinical Safety Engine (`SafetyEngineTest.java`)
| Test ID | Test Description | Target Invariant | Expected Outcome |
|---|---|---|---|
| `TC-SAFE-01` | Drug-Drug Interaction Detection | Aspirin + Warfarin co-prescription | Triggers `CRITICAL` alert with bleeding risk description |
| `TC-SAFE-02` | Drug-Allergy Cross Reactivity | Amoxicillin prescribed to Penicillin-allergic patient | Triggers `CRITICAL` alert with anaphylaxis warning |
| `TC-SAFE-03` | Drug-Condition Contraindication | Metformin prescribed to patient with Chronic Kidney Disease | Triggers `CRITICAL` alert with lactic acidosis warning |
| `TC-SAFE-04` | Duplicate Therapy Detection | Dolo 650 + Crocin Advance co-prescribed | Triggers `WARNING` alert for duplicate Paracetamol |
| `TC-SAFE-05` | Safe Combination Clearance | Paracetamol + Cetirizine | Returns empty alert list (`0` alerts) |
| `TC-SAFE-06` | Dynamic Rule Ingestion at Runtime | Adding rule for newly approved chemical entity | Immediate evaluation without JVM restart |
| `TC-SAFE-07` | Open/Closed Principle (Custom Strategy) | Registering custom Pediatric Dosage strategy | Engine executes new strategy polymorphically |

### 3.2 Prescription Lifecycle & Override Rules (`PrescriptionServiceTest.java`)
| Test ID | Test Description | Target Invariant | Expected Outcome |
|---|---|---|---|
| `TC-RX-01` | Unjustified Critical Alert Blocking | Saving prescription with unacknowledged `CRITICAL` alert | Throws `CriticalInteractionException` |
| `TC-RX-02` | Short Override Note Rejection | Entering override note with $<15$ characters (e.g. "ok") | Throws `IllegalArgumentException` |
| `TC-RX-03` | Valid Clinical Override Approval | Entering valid rationale ($\ge 15$ chars) | Successfully persists prescription and records audit |
| `TC-RX-04` | Multi-Item Financial Totaling | Aggregating multiple line items across duration | Accurately calculates sum in INR ($\pm 0.00$) |
| `TC-RX-05` | Empty Prescription Rejection | Attempting to save prescription with zero items | Throws `ValidationException` |

### 3.3 Outpatient Token State Machine (`TokenStatusTest.java`)
| Test ID | Test Description | Valid Transitions | Expected Outcome |
|---|---|---|---|
| `TC-TOK-01` | Initial Token Issuance | Creation $\rightarrow$ `WAITING` | Token is active in waiting queue |
| `TC-TOK-02` | Doctor Consultation Call | `WAITING` $\rightarrow$ `IN_CONSULTATION` | Token status transitions cleanly |
| `TC-TOK-03` | Consultation Completion | `IN_CONSULTATION` $\rightarrow$ `COMPLETED` | Consultation is closed |
| `TC-TOK-04` | Front Desk Cancellation | `WAITING` $\rightarrow$ `CANCELLED` | Token cancelled |
| `TC-TOK-05` | Illegal State Jump: Completed to Waiting | `COMPLETED` $\rightarrow$ `WAITING` | Throws `InvalidTokenStateException` |
| `TC-TOK-06` | Illegal State Jump: Cancelled to In Consultation | `CANCELLED` $\rightarrow$ `IN_CONSULTATION` | Throws `InvalidTokenStateException` |
| `TC-TOK-07` | Terminal State Invariant | Completed tokens cannot transition | Transition rejected |

### 3.4 Live Queue Observer Pattern (`QueueServiceObserverTest.java`)
| Test ID | Test Description | Expected Outcome |
|---|---|---|
| `TC-OBS-01` | Observer Notification on Token Issuance | Registered observer receives updated snapshot with new token |
| `TC-OBS-02` | Observer Notification on Status Transition | Observer notified when doctor calls token into consultation |
| `TC-OBS-03` | Observer Deregistration Isolation | Unregistered observer receives zero further event notifications |

### 3.5 Polymorphic Medication Dosage Grammar (`MedicationDosageTest.java`)
| Test ID | Test Description | Input Syntax | Expected Outcome |
|---|---|---|---|
| `TC-MED-01` | Tablet Dosage Validation | `"1 tablet"`, `"2 tabs"`, `"650mg"` | Validated `true` |
| `TC-MED-02` | Tablet Invalid Syntax Rejection | `"50 ml"`, `"-1 tabs"`, `"abc"` | Validated `false` |
| `TC-MED-03` | Capsule Dosage Validation | `"1 capsule"`, `"2 caps"` | Validated `true` |
| `TC-MED-04` | Liquid Oral Suspension Validation | `"5ml"`, `"10 ml"`, `"1 tsp"` | Validated `true`, bottles rounded up |
| `TC-MED-05` | Injectable Ampoule / Vial Validation | `"1 vial"`, `"2ml"` | Validated `true` |

### 3.6 Search & Typo-Tolerant Autocorrect (`MedicationSearchTest.java`)
| Test ID | Search Input | Expected Match | Algorithmic Mechanism |
|---|---|---|---|
| `TC-SRCH-01` | Brand name prefix (`"dol"`) | Dolo 650 | Prefix substring match |
| `TC-SRCH-02` | Generic molecule (`"paracetamol"`) | Dolo 650, Calpol 500 | Exact molecule match |
| `TC-SRCH-03` | RxNorm Concept Code (`"3222"`) | Dolo 650 | Exact concept match |
| `TC-SRCH-04` | Typo: `"amoxcilin"` | Amoxicillin | Levenshtein edit distance $\le 2$ |
| `TC-SRCH-05` | Typo: `"brufn"` | Brufen 400 | Token-level edit distance $\le 2$ |

### 3.7 Chronological Pill Schedule Mapping (`PillScheduleServiceTest.java`)
| Test ID | Test Description | Target Invariant | Expected Outcome |
|---|---|---|---|
| `TC-PILL-01` | 4-Phase Grid Allocation | Items mapped across Morning (8 AM), Afternoon (1 PM), Evening (6 PM), Night (9 PM) | Accurate placement per frequency |
| `TC-PILL-02` | Meal Precaution Extraction | Instructions with "before" and "after" food | Extracted into patient-friendly cautionary warnings |

### 3.8 Authentication & Security (`AuthServiceTest.java`)
| Test ID | Test Description | Input Credentials | Expected Outcome |
|---|---|---|---|
| `TC-AUTH-01` | Valid Doctor Authentication | `"doctor"` / `"1234567890"` | Returns active `Doctor` account |
| `TC-AUTH-02` | Session Binding | Valid login | SessionManager reflects active account |
| `TC-AUTH-03` | Invalid Password Rejection | `"doctor"` / `"wrongpass"` | Throws `AuthenticationException` |
| `TC-AUTH-04` | Non-Existent User Rejection | `"ghost_user"` | Throws `AuthenticationException` |
| `TC-AUTH-05` | Deactivated Account Rejection | Deactivated user account | Throws `AccountDisabledException` |
| `TC-AUTH-06` | Password Change Match Verification | Password update with valid current password | Password hash updated in database |
| `TC-AUTH-07` | Password Mismatch Guard | New password and confirmation do not match | Throws `ValidationException` |
| `TC-AUTH-08` | Short Password Guard | New password $< 8$ characters | Throws `ValidationException` |
| `TC-AUTH-09` | Wrong Current Password Guard | Password change with incorrect old password | Throws `AuthenticationException` |

### 3.9 Native JDBC DAO Integration Tests (`*DAOIT.java`)
| Test ID | DAO Suite | Operations Tested | ACID / Database Verification |
|---|---|---|---|
| `TC-DAO-01` | `AccountDAOIT` | Save, find by username, update profile, update password, active toggle | Queries against PostgreSQL `accounts` table |
| `TC-DAO-02` | `PatientDAOIT` | Save patient, add allergies/conditions, search by name/phone/ID, sequence `P-XXXX` | Multi-table join against `patient_allergies` and `conditions` |
| `TC-DAO-03` | `TokenDAOIT` | Sequential numbering (`T-001`, `T-002`), doctor queue filtering, status updates | Atomic serial numbering per calendar date |
| `TC-DAO-04` | `PrescriptionDAOIT` | Atomic multi-table write (header + items + alerts), foreign key integrity | Transactional commit and rollback verification |
| `TC-DAO-05` | `InteractionRuleDAOIT` | CRUD operations for dynamic interaction rules | Immediate query reflection |

### 3.10 PDF Document Generation (`PrescriptionPdfServiceTest.java`)
| Test ID | Test Description | Target Invariant | Expected Outcome |
|---|---|---|---|
| `TC-PDF-01` | iText 5 Medical Prescription PDF Export | Generates binary file from prescription + patient vitals | Output starts with `%PDF-`, valid page count, non-zero file size |

---

## 4. Test Execution Instructions

To execute the entire test suite and generate the JaCoCo coverage report:

```bash
mvn clean test
```

To view the generated HTML coverage report:
```
target/site/jacoco/index.html
```
