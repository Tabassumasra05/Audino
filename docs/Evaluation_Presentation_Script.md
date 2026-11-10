# Audino EMR: University Evaluation & Defense Presentation Script

**Document Version:** 1.0.0  
**Target Evaluation Date:** 19-November-2026  
**Courses:** Object-Oriented Methodology (OOM) & Software Engineering (SE)  
**System:** Audino: Intelligent Clinical Decision Support & EMR System  
**Presentation Format:** 5-Minute Live Interactive Defense & Technical Q&A  

---

## 1. Opening Pitch (00:00 – 01:00)

> *"Good morning, respected professors and evaluation committee members.*
> 
> *Today, we present **Audino: Intelligent Clinical Decision Support and Electronic Medical Record System**.*
> 
> *In outpatient clinical practice, Adverse Drug Events (ADEs) are among the leading causes of preventable morbidity and hospital readmissions worldwide. The root problem is cognitive overload: physicians are pressured to examine dozens of patients each day, making it easy to miss subtle cross-reactivities between a new prescription, existing medications, chronic conditions, and patient allergies.*
> 
> *Audino was engineered from the ground up to eliminate preventable ADEs using a real-time, multi-dimensional safety engine. Crucially, the software was designed without heavyweight commercial web frameworks or ORMs; it is a pure, modular Java 17 and JavaFX 19 application architected strictly around Gang of Four design patterns, SOLID principles, and native JDBC with ACID transaction guarantees.*
> 
> *Let us walk you through the end-to-end clinical workflow and show you how our object-oriented architecture directly enforces patient safety."*

---

## 2. Interactive Live Demonstration (01:00 – 04:00)

### Phase A: Triage & Outpatient Intake (The Receptionist Persona)
- **Action:** Open application $\rightarrow$ Click demo button **"Reception Neha"** $\rightarrow$ Log in.
- **Narrative:**
  > *"We begin at the front desk. Notice that login authentication uses adaptive **jBCrypt with cost factor 12**, resisting GPU brute-force attacks. Upon sign-in, our `NavigationService` routes polymorphically to `/fxml/receptionist_dashboard.fxml` because the `Receptionist` subclass defines its own workspace without any `if (role == ...)` conditional chains.*
  > 
  > *Here, the receptionist registers a patient or enters today's physiological vitals. Watch as we enter height of 175 cm and weight of 78 kg: the system instantly computes a BMI of 25.5 and classifies the patient as 'Overweight' via encapsulated methods in `PatientVitals`. When we issue a queue token assigned to Dr. Arjun Mehta, it creates Token **`T-001`** and notifies all subscribed observers across the clinic."*

### Phase B: Doctor Consultation & Clinical Decision Support (The Doctor Persona)
- **Action:** Sign out $\rightarrow$ Click demo button **"Dr. Arjun Mehta"** $\rightarrow$ Log in.
- **Narrative:**
  > *"Now we log in as Dr. Arjun Mehta, Cardiologist. Dr. Mehta's left queue drawer already reflects Token `T-001` in real-time, demonstrating the **Observer Pattern** where `QueueService` acts as the Subject.*
  > 
  > *Dr. Mehta clicks **'Call Next Patient'**. Token `T-001` transitions from `WAITING` to `IN_CONSULTATION`. Notice the top sticky clinical banner: it displays Ramesh Sharma's demographic details, blood pressure sparkline history, and high-visibility red badges for his documented allergies: **Penicillin** and **NSAIDs**.*
  > 
  > *Now let us prescribe. When typing medicine names, our **Search Service** features typo-tolerant autocorrect using Levenshtein distance: typing `'amoxcilin'` instantly suggests `'Did you mean: Amoxicillin?'`.*
  > 
  > *Now, watch the Clinical Decision Support engine in action. Dr. Mehta prescribes **Warfarin 5mg**, an anticoagulant. Next, he adds **Aspirin 75mg**, an antiplatelet.*
  > 
  > *Instantly, our **Safety Radar** flashes a high-visibility crimson blocking banner: **CRITICAL Drug-Drug Interaction Detected: Aspirin + Warfarin carries severe risk of gastrointestinal hemorrhage**. Notice that the 'Approve & Sign Prescription' button is **hard-disabled**. The system prevents the doctor from finalizing an unsafe prescription.*
  > 
  > *To proceed, the physician must acknowledge the alert and provide a clinical justification. If the doctor types a lazy note like `'ok'`, the system rejects it—our domain logic mandates a **minimum of 15 characters**. Dr. Mehta types: `'Daily INR monitored; indicated for acute co-morbidity under hospital observation'`.*
  > 
  > *The override is accepted, and Dr. Mehta approves the prescription. This single click triggers an atomic native JDBC transaction in `PrescriptionDAOJdbc`, persisting the header, line items, and override rationale into PostgreSQL, while logging the physician's action into an immutable forensic audit trail.*
  > 
  > *Finally, Dr. Mehta clicks **'Download Official Prescription PDF'**, generating an official, signed medical document via iText 5."*

### Phase C: Patient Self-Service Kiosk (Visual 4-Phase Chrono-Grid)
- **Action:** Sign out $\rightarrow$ Click **"Open Patient Kiosk (No Login Required)"**.
- **Narrative:**
  > *"Patients often misunderstand complex Latin medical abbreviations like 'TDS' or 'BD'. To prevent at-home medication errors, Audino provides a touch-friendly self-service kiosk.*
  > 
  > *The patient enters Token **`T-001`** and verifies their registered phone number using our on-screen numeric keypad. The system synthesizes their prescription into a **4-Phase Chronological Pill Grid**: Morning (8 AM), Afternoon (1 PM), Evening (6 PM), and Night (9 PM), complete with meal precautions such as 'Take strictly after meals'. The patient can print this schedule or download it as a PDF."*

### Phase D: Governance & Extensibility (The Administrator Persona)
- **Action:** Sign out $\rightarrow$ Click **"Admin Gupta"** $\rightarrow$ Log in.
- **Narrative:**
  > *"Finally, hospital administrators log in to review clinic KPIs, inspect JavaFX activity charts, and audit staff actions. In the Forensic Audit Log tab, we can see the permanent record of Dr. Mehta's critical override, including his exact justification note.*
  > 
  > *In the Interaction Rules tab, administrators can add new drug rules at runtime. Because our `SafetyEngine` implements the **Open/Closed Principle**, newly registered rules take effect immediately without restarting the application."*

---

## 3. Defense Q&A: Anticipated Examiner Questions & Model Answers

### Question 1: *"Why did you avoid Spring Boot and Hibernate/JPA?"*
> **Answer:**  
> *"In software engineering, frameworks like Spring and Hibernate introduce significant runtime reflection, hidden query generation, high memory overhead, and dependency bloat. By implementing native JDBC with the **DAO Pattern**, we maintain explicit, deterministic control over every database query and connection socket via HikariCP. More importantly for an academic evaluation, this demonstrates mastery of core architectural patterns (DAO, Factory, Singleton, Strategy) and explicit transaction management (`conn.setAutoCommit(false)` and `conn.rollback()`), rather than delegating architectural responsibilities to framework annotations."*

---

### Question 2: *"How does your codebase uphold the Open/Closed Principle (OCP)?"*
> **Answer:**  
> *"Our Clinical Decision Support engine in `com.audino.service.strategy.SafetyEngine` is open for extension but closed for modification. It maintains a collection of `InteractionStrategy` interfaces. When new clinical guidelines arise—such as a pediatric dosage check or renal clearance rule—a developer simply creates a new class implementing `InteractionStrategy` and calls `safetyEngine.registerStrategy(new RenalStrategy())`. The engine evaluates it polymorphically in its loop without modifying a single line of `SafetyEngine.java`. We have verified this directly in our test suite with `SafetyEngineTest.testOpenClosedPrincipleCustomStrategy()`."*

---

### Question 3: *"Explain the difference between Aggregation and Composition using your actual classes."*
> **Answer:**  
> *"In our system:
> 1. **Aggregation (Weak association, independent lifecycles):** `Prescription` aggregates `Medication`. Medications exist independently in the hospital formulary database. If a prescription is deleted or cancelled, the `Medication` records remain intact.
> 2. **Composition (Strong ownership, cascading lifecycle):** `Prescription` composes `PrescriptionItem` and `PrescriptionAlert`. A `PrescriptionItem` cannot exist without its owning `Prescription`. In our code, items can only be created and attached via `prescription.addItem()`, and in our database schema, they are governed by `ON DELETE CASCADE`. If the prescription is destroyed, all of its items and alerts are destroyed simultaneously. The same composition relationship exists between `Patient` and `PatientAllergy`."*

---

### Question 4: *"How did you achieve polymorphic login routing without using switch statements?"*
> **Answer:**  
> *"In `com.audino.model.Account`, we declared an abstract hook method: `public abstract String getDashboardFxml()`.  
> Each subclass overrides this method: `Doctor` returns `/fxml/doctor_consultation.fxml`, `Receptionist` returns `/fxml/receptionist_dashboard.fxml`, and `Admin` returns `/fxml/admin_dashboard.fxml`.  
> In `AppShellController.java`, line 159 simply calls:
> ```java
> String initialFxml = account.getDashboardFxml();
> NavigationService.getInstance().navigateTo(initialFxml, initialTitle);
> ```
> The JVM resolves the destination screen dynamically via virtual method dispatch, completely eliminating brittle `if (role == Role.DOCTOR)` chains."*

---

### Question 5: *"How do you ensure zero-setup portability on an examiner's computer?"*
> **Answer:**  
> *"Our `ConfigurationManager` probes port 5432 at startup. If a PostgreSQL instance is running on `localhost`, it connects directly. If none is found, it automatically boots **Embedded PostgreSQL** (`io.zonky.test:embedded-postgres`) with data stored locally in `./data/pg-data`. All tables are generated on-the-fly by `DatabaseInitializer`, and demo data is seeded by `DataSeeder`. A teammate or evaluator can clone the repository on any computer with only JDK 17 and Maven and execute `mvn clean javafx:run` with zero manual configuration."*

---

## 4. Closing Statement (04:45 – 05:00)

> *"In conclusion, Audino proves that enterprise software engineering and clinical safety can be achieved through disciplined object-oriented architecture. With 74 passing tests, zero external theme dependencies, and full SDLC documentation, Audino stands complete, stable, and ready for evaluation. Thank you, and we welcome your questions."*
