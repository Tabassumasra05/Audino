# UML Diagrams — Audino

## 1. Use Case Diagram

Shows the main actors and their interactions with the Audino system, including authentication, patient management, consultation, prescribing, kiosk access, and administration.

![Use Case Diagram](diagrams/use_case_diagram.png)

---

## 2. Class Diagram

Shows the main classes, inheritance, associations, composition, aggregation, and object-oriented design patterns used in Audino.

![Class Diagram](diagrams/class_diagram.png)

---

## 3. Sequence Diagram — Prescription Safety

Shows the flow of prescribing a medication, detecting a critical interaction, overriding the alert with justification, and saving the prescription.

![Sequence Diagram - Prescription Safety Override](diagrams/sequence_diagram_prescription_safety_override.png)

---

## 4. Sequence Diagram — Queue Token Observer

Shows outpatient token generation, database storage, observer notification, and real-time queue updates.

![Sequence Diagram - Queue Token Observer](diagrams/sequence_diagram_queue_token_observer.png)

---

## 5. Activity Diagram — Patient Lifecycle

Shows the complete outpatient workflow from patient registration and triage through consultation, prescription, safety checks, and kiosk access.

![Activity Diagram - Patient Lifecycle](diagrams/activity_diagram_patient_lifecycle.png)

---

## 6. State Diagram — Token Lifecycle

Shows the lifecycle of a patient token from `WAITING` to `IN_CONSULTATION`, `COMPLETED`, or `CANCELLED`.

![Token Lifecycle State Machine](diagrams/state_diagram_token.png)

---

## 7. State Diagram — Prescription Lifecycle

Shows the prescription states from `DRAFT` through safety evaluation to `APPROVED` or `CANCELLED`.

![Prescription Lifecycle State Machine](diagrams/state_diagram_prescription.png)

---

## 8. ER Diagram

Shows the main database entities and their relationships in the Audino system.

![ER Diagram](diagrams/er_diagram.png)

---

## 9. Component & Deployment Diagram

Shows the major software components, database connectivity, and deployment architecture of Audino.

![Component & Deployment Architecture](diagrams/component_and_deployment_diagram.png)
