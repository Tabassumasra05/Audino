package com.audino.util;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Idempotently seeds initial clinical data (accounts, formulary, interaction rules,
 * patients, and outpatient tokens) in the Indian healthcare context.
 * Only executes if the accounts table is empty.
 */
public class DataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final DataSource dataSource;

    public DataSeeder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Seeds initial database records if empty.
     */
    public void seedIfEmpty() {
        try (Connection conn = dataSource.getConnection()) {
            if (isAlreadySeeded(conn)) {
                logger.info("Database already populated. Skipping data seeding.");
                return;
            }

            logger.info("Empty database detected. Seeding clinical initial dataset (Indian context)...");
            conn.setAutoCommit(false);

            try {
                seedAccounts(conn);
                seedMedications(conn);
                seedInteractionRules(conn);
                seedPatientsAndVitals(conn);
                seedTokensAndPrescriptions(conn);

                conn.commit();
                logger.info("Database successfully seeded with clinical demo records.");
            } catch (Exception e) {
                conn.rollback();
                logger.error("Failed to seed initial data. Transaction rolled back: {}", e.getMessage(), e);
                throw new RuntimeException("Data seeding failed", e);
            }
        } catch (SQLException e) {
            logger.error("Database connection error during seeding: {}", e.getMessage(), e);
        }
    }

    private boolean isAlreadySeeded(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM accounts")) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private void seedAccounts(Connection conn) throws SQLException {
        logger.info("Seeding staff accounts...");
        String defaultHash = BCrypt.hashpw("1234567890", BCrypt.gensalt(12));

        String sql = "INSERT INTO accounts (username, password_hash, full_name, role, specialization, registration_no, active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // 1. Admin
            ps.setString(1, "admin");
            ps.setString(2, defaultHash);
            ps.setString(3, "Dr. Sanjay Gupta (Admin)");
            ps.setString(4, "ADMIN");
            ps.setString(5, "Health Informatics & Hospital Admin");
            ps.setString(6, "ADMIN-001");
            ps.setBoolean(7, true);
            ps.addBatch();

            // 2. Doctor 1 (General Physician)
            ps.setString(1, "doctor");
            ps.setString(2, defaultHash);
            ps.setString(3, "Dr. Arjun Mehta");
            ps.setString(4, "DOCTOR");
            ps.setString(5, "General Physician & Internal Medicine");
            ps.setString(6, "MCI-48291");
            ps.setBoolean(7, true);
            ps.addBatch();

            // 3. Doctor 2 (Cardiologist)
            ps.setString(1, "doctor2");
            ps.setString(2, defaultHash);
            ps.setString(3, "Dr. Kavya Rao");
            ps.setString(4, "DOCTOR");
            ps.setString(5, "Cardiologist");
            ps.setString(6, "MCI-59102");
            ps.setBoolean(7, true);
            ps.addBatch();

            // 4. Receptionist
            ps.setString(1, "reception");
            ps.setString(2, defaultHash);
            ps.setString(3, "Neha Verma");
            ps.setString(4, "RECEPTIONIST");
            ps.setString(5, null);
            ps.setString(6, null);
            ps.setBoolean(7, true);
            ps.addBatch();

            ps.executeBatch();
        }
    }

    private void seedMedications(Connection conn) throws SQLException {
        logger.info("Seeding Indian formulary medications...");
        String medSql = "INSERT INTO medications (id, generic_name, brand_name, rxnorm_code, type, strength, price_per_unit) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String ingSql = "INSERT INTO medication_ingredients (medication_id, ingredient_name) VALUES (?, ?)";

        Object[][] meds = {
            {1L, "Paracetamol", "Dolo 650", "3222", "TABLET", "650mg", new BigDecimal("30.00"), new String[]{"Paracetamol", "Acetaminophen"}},
            {2L, "Amoxicillin", "Mox 500", "723", "CAPSULE", "500mg", new BigDecimal("75.00"), new String[]{"Amoxicillin", "Penicillin"}},
            {3L, "Ibuprofen", "Brufen 400", "5640", "TABLET", "400mg", new BigDecimal("25.00"), new String[]{"Ibuprofen", "NSAID"}},
            {4L, "Warfarin", "Warf 5", "11289", "TABLET", "5mg", new BigDecimal("85.00"), new String[]{"Warfarin", "Anticoagulant"}},
            {5L, "Aspirin", "Ecosprin 75", "1191", "TABLET", "75mg", new BigDecimal("18.00"), new String[]{"Aspirin", "Acetylsalicylic Acid", "NSAID"}},
            {6L, "Azithromycin", "Azee 500", "18631", "TABLET", "500mg", new BigDecimal("120.00"), new String[]{"Azithromycin", "Macrolide"}},
            {7L, "Metformin", "Glycomet 500", "6809", "TABLET", "500mg", new BigDecimal("45.00"), new String[]{"Metformin", "Biguanide"}},
            {8L, "Atorvastatin", "Atorva 10", "83367", "TABLET", "10mg", new BigDecimal("68.00"), new String[]{"Atorvastatin", "Statin"}},
            {9L, "Amlodipine", "Amlong 5", "17767", "TABLET", "5mg", new BigDecimal("35.00"), new String[]{"Amlodipine", "Calcium Channel Blocker"}},
            {10L, "Omeprazole", "Omez 20", "7646", "CAPSULE", "20mg", new BigDecimal("55.00"), new String[]{"Omeprazole", "Proton Pump Inhibitor"}},
            {11L, "Clopidogrel", "Deplatt 75", "32968", "TABLET", "75mg", new BigDecimal("95.00"), new String[]{"Clopidogrel", "Antiplatelet"}},
            {12L, "Celecoxib", "Cobix 200", "140587", "CAPSULE", "200mg", new BigDecimal("110.00"), new String[]{"Celecoxib", "Sulfa", "NSAID"}},
            {13L, "Cetirizine", "Cetzine 10", "20610", "TABLET", "10mg", new BigDecimal("22.00"), new String[]{"Cetirizine", "Antihistamine"}},
            {14L, "Pantoprazole", "Pan 40", "40790", "TABLET", "40mg", new BigDecimal("80.00"), new String[]{"Pantoprazole", "Proton Pump Inhibitor"}},
            {15L, "Montelukast + Levocetirizine", "Montair LC", "61245", "TABLET", "10mg/5mg", new BigDecimal("140.00"), new String[]{"Montelukast", "Levocetirizine"}},
            {16L, "Ciprofloxacin", "Ciplox 500", "2551", "TABLET", "500mg", new BigDecimal("60.00"), new String[]{"Ciprofloxacin", "Fluoroquinolone"}},
            {17L, "Losartan", "Repace 50", "52175", "TABLET", "50mg", new BigDecimal("58.00"), new String[]{"Losartan", "ARB"}},
            {18L, "Telmisartan", "Telma 40", "73494", "TABLET", "40mg", new BigDecimal("72.00"), new String[]{"Telmisartan", "ARB"}},
            {19L, "Propranolol", "Ciplar 40", "8787", "TABLET", "40mg", new BigDecimal("42.00"), new String[]{"Propranolol", "Beta Blocker"}},
            {20L, "Ranitidine", "Rantac 150", "9143", "TABLET", "150mg", new BigDecimal("28.00"), new String[]{"Ranitidine", "H2 Blocker"}},
            {21L, "Levothyroxine", "Thyronorm 50", "10582", "TABLET", "50mcg", new BigDecimal("125.00"), new String[]{"Levothyroxine", "Thyroid"}},
            {22L, "Dextromethorphan Cough Syrup", "Ascoril D", "3289", "LIQUID", "100ml", new BigDecimal("110.00"), new String[]{"Dextromethorphan", "Phenylephrine"}},
            {23L, "Paracetamol Pediatric Syrup", "Calpol 120", "3222", "LIQUID", "60ml", new BigDecimal("40.00"), new String[]{"Paracetamol"}},
            {24L, "Oral Rehydration Salts (ORS)", "Electral", "7832", "LIQUID", "200ml", new BigDecimal("32.00"), new String[]{"Sodium Chloride", "Potassium Chloride", "Glucose"}},
            {25L, "Sucralfate Suspension", "Sucrafil", "10156", "LIQUID", "200ml", new BigDecimal("185.00"), new String[]{"Sucralfate"}},
            {26L, "Amoxicillin Oral Suspension", "Novamox 125", "723", "LIQUID", "60ml", new BigDecimal("65.00"), new String[]{"Amoxicillin", "Penicillin"}},
            {27L, "Insulin Glargine", "Lantus SoloStar", "274783", "INJECTION", "100IU/ml (3ml)", new BigDecimal("650.00"), new String[]{"Insulin Glargine"}},
            {28L, "Ceftriaxone Injection", "Monocef 1g", "2193", "INJECTION", "1g vial", new BigDecimal("75.00"), new String[]{"Ceftriaxone", "Cephalosporin", "Beta Lactam"}},
            {29L, "Diclofenac Sodium", "Voveran 75", "3355", "INJECTION", "75mg/1ml", new BigDecimal("30.00"), new String[]{"Diclofenac", "NSAID"}},
            {30L, "Ondansetron Injection", "Emeset 2ml", "26225", "INJECTION", "2mg/ml", new BigDecimal("28.00"), new String[]{"Ondansetron", "Antiemetic"}},
            {31L, "Tramadol Injection", "Tramazac 100", "10689", "INJECTION", "100mg/2ml", new BigDecimal("48.00"), new String[]{"Tramadol", "Opioid"}},
            {32L, "Methylprednisolone", "Solu-Medrol 500", "6902", "INJECTION", "500mg vial", new BigDecimal("520.00"), new String[]{"Methylprednisolone", "Corticosteroid"}},
            {33L, "Doxycycline", "Doxicip 100", "3640", "CAPSULE", "100mg", new BigDecimal("45.00"), new String[]{"Doxycycline", "Tetracycline"}},
            {34L, "Rabeprazole", "Razo 20", "84813", "TABLET", "20mg", new BigDecimal("90.00"), new String[]{"Rabeprazole", "Proton Pump Inhibitor"}},
            {35L, "Hydrochlorothiazide", "Aquazide 12.5", "5487", "TABLET", "12.5mg", new BigDecimal("28.00"), new String[]{"Hydrochlorothiazide", "Thiazide Diuretic"}},
            {36L, "Digoxin", "Lanoxin 0.25", "3407", "TABLET", "0.25mg", new BigDecimal("15.00"), new String[]{"Digoxin", "Cardiac Glycoside"}},
            {37L, "Spironolactone", "Aldactone 25", "9997", "TABLET", "25mg", new BigDecimal("42.00"), new String[]{"Spironolactone", "Diuretic"}},
            {38L, "Rosuvastatin", "Rozavel 10", "301542", "TABLET", "10mg", new BigDecimal("115.00"), new String[]{"Rosuvastatin", "Statin"}},
            {39L, "Furosemide", "Lasix 40", "4603", "TABLET", "40mg", new BigDecimal("14.00"), new String[]{"Furosemide", "Loop Diuretic"}},
            // Unpriced medications to demonstrate live price prompt dialog in doctor pad
            {40L, "Vitamin D3 60K", "Calcirol 60K", "2418", "CAPSULE", "60000 IU", null, new String[]{"Cholecalciferol", "Vitamin D"}},
            {41L, "Multivitamin Tonic", "Becozinc", "9823", "LIQUID", "200ml", null, new String[]{"Vitamin B Complex", "Zinc"}}
        };

        try (PreparedStatement medPs = conn.prepareStatement(medSql);
             PreparedStatement ingPs = conn.prepareStatement(ingSql)) {
            for (Object[] row : meds) {
                Long id = (Long) row[0];
                medPs.setLong(1, id);
                medPs.setString(2, (String) row[1]);
                medPs.setString(3, (String) row[2]);
                medPs.setString(4, (String) row[3]);
                medPs.setString(5, (String) row[4]);
                medPs.setString(6, (String) row[5]);
                medPs.setBigDecimal(7, (BigDecimal) row[6]);
                medPs.addBatch();

                String[] ingredients = (String[]) row[7];
                for (String ing : ingredients) {
                    ingPs.setLong(1, id);
                    ingPs.setString(2, ing);
                    ingPs.addBatch();
                }
            }
            medPs.executeBatch();
            ingPs.executeBatch();
        }
        
        // Advance sequence
        try (Statement s = conn.createStatement()) {
            s.execute("SELECT setval('medications_id_seq', (SELECT MAX(id) FROM medications))");
        }
    }

    private void seedInteractionRules(Connection conn) throws SQLException {
        logger.info("Seeding clinical interaction rules...");
        String sql = "INSERT INTO interaction_rules (type, keyword1, keyword2, severity, description, recommendation) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        Object[][] rules = {
            // Drug - Drug interactions
            {"DRUG_DRUG", "Warfarin", "Ibuprofen", "CRITICAL",
             "Severe risk of life-threatening gastrointestinal hemorrhage. NSAIDs inhibit platelet aggregation and may displace warfarin from protein binding.",
             "Avoid concurrent use. Use Paracetamol for analgesia or monitor INR daily with gastroprotection."},
            {"DRUG_DRUG", "Warfarin", "Aspirin", "CRITICAL",
             "Synergistic anticoagulant and antiplatelet inhibition markedly increases intracranial and systemic bleeding risk.",
             "Avoid combination unless indicated for mechanical heart valves under close specialized INR monitoring."},
            {"DRUG_DRUG", "Clopidogrel", "Omeprazole", "CRITICAL",
             "Omeprazole strongly inhibits CYP2C19, decreasing active clopidogrel metabolite formation and increasing recurrent myocardial infarction risk.",
             "Substitute Omeprazole with Pantoprazole or Rabeprazole, which exhibit minimal CYP2C19 inhibition."},
            {"DRUG_DRUG", "Digoxin", "Furosemide", "CRITICAL",
             "Loop diuretic-induced hypokalemia severely potentiates digoxin cardiotoxicity, triggering lethal ventricular arrhythmias.",
             "Monitor serum potassium levels; co-prescribe potassium-sparing agents or potassium supplements."},
            {"DRUG_DRUG", "Tramadol", "SSRI", "CRITICAL",
             "Concurrent administration precipitates life-threatening Serotonin Syndrome and decreases seizure threshold.",
             "Do not co-prescribe. Substitute analgesia with paracetamol or non-serotonergic agents."},
            {"DRUG_DRUG", "Atorvastatin", "Azithromycin", "WARNING",
             "Macrolide inhibits CYP3A4-mediated statin clearance, increasing risk of acute myopathy and rhabdomyolysis.",
             "Temporarily pause statin therapy during 3-5 day azithromycin course, or monitor for muscle pain."},
            {"DRUG_DRUG", "Amlodipine", "Simvastatin", "WARNING",
             "Amlodipine increases systemic simvastatin exposure. Risk of statin-associated muscle symptoms.",
             "Limit simvastatin dosage to maximum 20mg daily or switch to Rosuvastatin."},
            {"DRUG_DRUG", "Metformin", "Contrast", "WARNING",
             "Iodinated radiocontrast combined with metformin increases risk of acute kidney injury and lactic acidosis.",
             "Withhold metformin 48 hours prior to and post contrast administration."},

            // Drug - Allergy contraindications
            {"DRUG_ALLERGY", "Penicillin", "Amoxicillin", "CRITICAL",
             "Anaphylactic cross-reactivity: Amoxicillin is a beta-lactam penicillin derivative and may trigger fatal anaphylaxis.",
             "Absolute contraindication. Prescribe Macrolides (Azithromycin) or Fluoroquinolones."},
            {"DRUG_ALLERGY", "Sulfa", "Celecoxib", "WARNING",
             "Celecoxib molecule contains a sulfonamide substituent; potential risk of severe cutaneous adverse reactions.",
             "Exercise high caution in patients with history of severe sulfonamide erythema multiforme/SJS."},
            {"DRUG_ALLERGY", "Aspirin", "Ibuprofen", "CRITICAL",
             "NSAID hypersensitivity cross-reactivity: triggers acute severe bronchospasm, urticaria, or angioedema.",
             "Avoid all cyclooxygenase-1 inhibiting NSAIDs. Use Paracetamol."},
            {"DRUG_ALLERGY", "Cephalosporin", "Ceftriaxone", "CRITICAL",
             "Known hypersensitivity to cephalosporin antibiotics risk severe anaphylactic shock.",
             "Avoid all cephalosporins. Select alternative antibiotic class guided by culture."},

            // Drug - Condition contraindications
            {"DRUG_CONDITION", "Peptic Ulcer", "Ibuprofen", "CRITICAL",
             "NSAID-induced inhibition of gastric mucosal prostaglandins triggers acute bleeding and gastric perforation.",
             "Absolute contraindication in active peptic ulcer disease. Use Paracetamol."},
            {"DRUG_CONDITION", "Peptic Ulcer", "Aspirin", "CRITICAL",
             "Direct topical and systemic platelet inhibition causes severe mucosal ulcer recurrence and GI bleeding.",
             "Avoid unless strictly required for secondary cardiovascular prevention with high-dose PPI co-therapy."},
            {"DRUG_CONDITION", "Renal Failure", "Metformin", "CRITICAL",
             "Reduced renal clearance of metformin results in dangerous drug accumulation and fatal lactic acidosis.",
             "Contraindicated if eGFR < 30 mL/min/1.73m². Discontinue and switch to insulin."},
            {"DRUG_CONDITION", "Asthma", "Propranolol", "WARNING",
             "Non-selective beta-2 receptor blockade triggers refractory bronchoconstriction and fatal status asthmaticus.",
             "Absolute contraindication in reactive airway disease. Use cardioselective beta-1 blockers or CCBs."},
            {"DRUG_CONDITION", "Heart Failure", "Ibuprofen", "WARNING",
             "NSAIDs promote renal sodium retention, blunt ACE-inhibitor and loop diuretic actions, exacerbating congestive failure.",
             "Avoid NSAIDs in decompensated NYHA Class III-IV heart failure."},
            {"DRUG_CONDITION", "Hypertension", "Phenylephrine", "WARNING",
             "Alpha-1 adrenergic vasoconstriction elevates systemic peripheral resistance and exacerbates arterial hypertension.",
             "Use non-sympathomimetic nasal sprays or steam inhalation."},

            // Duplicate Therapy warnings
            {"DUPLICATE_THERAPY", "Paracetamol", "Dolo", "WARNING",
             "Duplicate acetaminophen therapy detected. Aggregate dose exceeding 4000mg/day risks severe acute liver failure.",
             "Consolidate prescription into a single acetaminophen agent."},
            {"DUPLICATE_THERAPY", "Omeprazole", "Pantoprazole", "WARNING",
             "Dual proton pump inhibitor therapy provides zero additive clinical suppression and increases hypomagnesemia risk.",
             "Remove redundant PPI agent."}
        };

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] r : rules) {
                ps.setString(1, (String) r[0]);
                ps.setString(2, (String) r[1]);
                ps.setString(3, (String) r[2]);
                ps.setString(4, (String) r[3]);
                ps.setString(5, (String) r[4]);
                ps.setString(6, (String) r[5]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void seedPatientsAndVitals(Connection conn) throws SQLException {
        logger.info("Seeding Indian clinical patient profiles and vitals history...");
        String patSql = "INSERT INTO patients (id, patient_id, first_name, last_name, date_of_birth, gender, phone, address) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String allgSql = "INSERT INTO patient_allergies (patient_id, allergen, severity) VALUES (?, ?, ?)";
        String condSql = "INSERT INTO patient_conditions (patient_id, condition_name, diagnosed_date) VALUES (?, ?, ?)";
        String vitSql = "INSERT INTO patient_vitals (patient_id, recorded_at, height_cm, weight_kg, systolic_bp, diastolic_bp, pulse_bpm, temperature_c, spo2_percent) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Object[][] patients = {
            {1L, "P-1001", "Ramesh", "Sharma", Date.valueOf("1968-05-14"), "MALE", "9820112345", "Flat 402, Gokul Dham, Andheri East, Mumbai",
             new String[][]{{"Penicillin", "CRITICAL"}}, new String[][]{{"Peptic Ulcer", "2021-03-10"}, {"Hypertension", "2018-06-15"}}},
            {2L, "P-1002", "Priya", "Patel", Date.valueOf("1985-09-22"), "FEMALE", "9879154321", "12 Shanti Nagar, SG Highway, Ahmedabad",
             new String[][]{{"Sulfa", "MODERATE"}}, new String[][]{{"Asthma", "2015-11-20"}}},
            {3L, "P-1003", "Sunita", "Deshmukh", Date.valueOf("1954-12-05"), "FEMALE", "9422098765", "Plot 18, Shivaji Housing Society, Pune",
             new String[][]{{"Aspirin", "SEVERE"}}, new String[][]{{"Renal Failure", "2022-01-14"}, {"Type 2 Diabetes", "2010-04-05"}}},
            {4L, "P-1004", "Amit", "Verma", Date.valueOf("1992-03-18"), "MALE", "9811234567", "B-44 Defence Colony, New Delhi",
             new String[][]{}, new String[][]{{"Hypertension", "2023-08-19"}}},
            {5L, "P-1005", "Rajesh", "Nair", Date.valueOf("1972-07-30"), "MALE", "9447012389", "TC 14/220, Kowdiar, Thiruvananthapuram",
             new String[][]{{"Penicillin", "SEVERE"}}, new String[][]{{"Atrial Fibrillation", "2020-05-12"}}},
            {6L, "P-1006", "Meena", "Iyer", Date.valueOf("1988-11-10"), "FEMALE", "9840234561", "3rd Cross, Indira Nagar, Bengaluru",
             new String[][]{}, new String[][]{{"Hypothyroidism", "2019-02-14"}}},
            {7L, "P-1007", "Vikram", "Malhotra", Date.valueOf("1965-01-25"), "MALE", "9814098712", "55 Model Town, Jalandhar, Punjab",
             new String[][]{{"Sulfa", "SEVERE"}}, new String[][]{{"Coronary Artery Disease", "2017-09-01"}, {"Hypertension", "2012-06-11"}}},
            {8L, "P-1008", "Ananya", "Roy", Date.valueOf("1996-06-15"), "FEMALE", "9830145678", "Salt Lake Sector II, Kolkata",
             new String[][]{}, new String[][]{{"Asthma", "2018-07-22"}}},
            {9L, "P-1009", "Mohammed", "Farooq", Date.valueOf("1958-04-02"), "MALE", "9849012345", "Banjara Hills Road No 10, Hyderabad",
             new String[][]{}, new String[][]{{"Type 2 Diabetes", "2008-01-15"}, {"Renal Failure", "2023-04-10"}}},
            {10L, "P-1010", "Lakshmi", "Bai", Date.valueOf("1970-08-19"), "FEMALE", "9425098765", "Mahalaxmi Nagar, Indore",
             new String[][]{{"Cephalosporin", "CRITICAL"}}, new String[][]{{"Peptic Ulcer", "2022-09-05"}}},
            {11L, "P-1011", "Suresh", "Gupta", Date.valueOf("1980-10-12"), "MALE", "9829012399", "C-Scheme, Jaipur, Rajasthan",
             new String[][]{}, new String[][]{{"Hypertension", "2020-08-10"}}},
            {12L, "P-1012", "Deepa", "Nambiar", Date.valueOf("1990-02-28"), "FEMALE", "9848011223", "Marine Drive, Kochi, Kerala",
             new String[][]{}, new String[][]{}},
            {13L, "P-1013", "Harish", "Rao", Date.valueOf("1962-09-14"), "MALE", "9822019988", "Tilak Road, Pune",
             new String[][]{{"Aspirin", "MODERATE"}}, new String[][]{{"Congestive Heart Failure", "2021-11-03"}}},
            {14L, "P-1014", "Pooja", "Singh", Date.valueOf("1994-07-04"), "FEMALE", "9910023456", "Sector 62, Noida",
             new String[][]{}, new String[][]{}},
            {15L, "P-1015", "Rahul", "Kulkarni", Date.valueOf("1978-12-20"), "MALE", "9823014567", "Samarth Nagar, Aurangabad",
             new String[][]{{"Penicillin", "CRITICAL"}}, new String[][]{{"Hypertension", "2016-04-12"}}}
        };

        try (PreparedStatement pPs = conn.prepareStatement(patSql);
             PreparedStatement aPs = conn.prepareStatement(allgSql);
             PreparedStatement cPs = conn.prepareStatement(condSql);
             PreparedStatement vPs = conn.prepareStatement(vitSql)) {

            for (Object[] pat : patients) {
                Long id = (Long) pat[0];
                pPs.setLong(1, id);
                pPs.setString(2, (String) pat[1]);
                pPs.setString(3, (String) pat[2]);
                pPs.setString(4, (String) pat[3]);
                pPs.setDate(5, (Date) pat[4]);
                pPs.setString(6, (String) pat[5]);
                pPs.setString(7, (String) pat[6]);
                pPs.setString(8, (String) pat[7]);
                pPs.addBatch();

                String[][] allergies = (String[][]) pat[8];
                for (String[] allg : allergies) {
                    aPs.setLong(1, id);
                    aPs.setString(2, allg[0]);
                    aPs.setString(3, allg[1]);
                    aPs.addBatch();
                }

                String[][] conditions = (String[][]) pat[9];
                for (String[] cond : conditions) {
                    cPs.setLong(1, id);
                    cPs.setString(2, cond[0]);
                    cPs.setDate(3, Date.valueOf(cond[1]));
                    cPs.addBatch();
                }

                // Add 2-3 historical vitals entries per patient for sparklines
                vPs.setLong(1, id);
                vPs.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusMonths(3)));
                vPs.setBigDecimal(3, new BigDecimal("168.0"));
                vPs.setBigDecimal(4, new BigDecimal("72.5"));
                vPs.setInt(5, 138);
                vPs.setInt(6, 88);
                vPs.setInt(7, 82);
                vPs.setBigDecimal(8, new BigDecimal("36.8"));
                vPs.setInt(9, 98);
                vPs.addBatch();

                vPs.setLong(1, id);
                vPs.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusWeeks(2)));
                vPs.setBigDecimal(3, new BigDecimal("168.0"));
                vPs.setBigDecimal(4, new BigDecimal("71.8"));
                vPs.setInt(5, 132);
                vPs.setInt(6, 84);
                vPs.setInt(7, 78);
                vPs.setBigDecimal(8, new BigDecimal("36.6"));
                vPs.setInt(9, 99);
                vPs.addBatch();

                // Latest visit vitals
                vPs.setLong(1, id);
                vPs.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusHours(1)));
                vPs.setBigDecimal(3, new BigDecimal("168.0"));
                vPs.setBigDecimal(4, new BigDecimal("71.0"));
                vPs.setInt(5, 126);
                vPs.setInt(6, 82);
                vPs.setInt(7, 74);
                vPs.setBigDecimal(8, new BigDecimal("36.7"));
                vPs.setInt(9, 99);
                vPs.addBatch();
            }

            pPs.executeBatch();
            aPs.executeBatch();
            cPs.executeBatch();
            vPs.executeBatch();
        }

        try (Statement s = conn.createStatement()) {
            s.execute("SELECT setval('patients_id_seq', (SELECT MAX(id) FROM patients))");
        }
    }

    private void seedTokensAndPrescriptions(Connection conn) throws SQLException {
        logger.info("Seeding outpatient tokens and consultation queue...");
        String tokenSql = "INSERT INTO tokens (id, token_no, token_date, patient_id, doctor_username, status, issued_at, called_at, completed_at) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Object[][] tokens = {
            // Completed consultations today
            {1L, "T-001", Date.valueOf(today), 4L, "doctor", "COMPLETED",
             Timestamp.valueOf(now.minusHours(4)), Timestamp.valueOf(now.minusHours(3).minusMinutes(50)), Timestamp.valueOf(now.minusHours(3).minusMinutes(20))},
            {2L, "T-002", Date.valueOf(today), 6L, "doctor", "COMPLETED",
             Timestamp.valueOf(now.minusHours(3)), Timestamp.valueOf(now.minusHours(2).minusMinutes(45)), Timestamp.valueOf(now.minusHours(2).minusMinutes(15))},
            
            // In Consultation right now
            {3L, "T-003", Date.valueOf(today), 1L, "doctor", "IN_CONSULTATION",
             Timestamp.valueOf(now.minusHours(2)), Timestamp.valueOf(now.minusMinutes(20)), null},
            
            // Waiting in queue
            {4L, "T-004", Date.valueOf(today), 2L, "doctor", "WAITING",
             Timestamp.valueOf(now.minusMinutes(45)), null, null},
            {5L, "T-005", Date.valueOf(today), 3L, "doctor", "WAITING",
             Timestamp.valueOf(now.minusMinutes(30)), null, null},
            {6L, "T-006", Date.valueOf(today), 5L, "doctor", "WAITING",
             Timestamp.valueOf(now.minusMinutes(15)), null, null},
            {7L, "T-007", Date.valueOf(today), 7L, "doctor2", "WAITING",
             Timestamp.valueOf(now.minusMinutes(10)), null, null},
            {8L, "T-008", Date.valueOf(today), 8L, "doctor2", "WAITING",
             Timestamp.valueOf(now.minusMinutes(5)), null, null}
        };

        try (PreparedStatement ps = conn.prepareStatement(tokenSql)) {
            for (Object[] row : tokens) {
                ps.setLong(1, (Long) row[0]);
                ps.setString(2, (String) row[1]);
                ps.setDate(3, (Date) row[2]);
                ps.setLong(4, (Long) row[3]);
                ps.setString(5, (String) row[4]);
                ps.setString(6, (String) row[5]);
                ps.setTimestamp(7, (Timestamp) row[6]);
                ps.setTimestamp(8, (Timestamp) row[7]);
                ps.setTimestamp(9, (Timestamp) row[8]);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Seed 1 completed prescription for T-001 so kiosk and history have data
        String prescSql = "INSERT INTO prescriptions (id, patient_id, doctor_username, token_id, status, total_cost, created_at) " +
                          "VALUES (1, 4, 'doctor', 1, 'APPROVED', 142.00, ?)";
        String itemSql = "INSERT INTO prescription_items (prescription_id, medication_id, dosage, frequency, duration_days, instructions, unit_cost, total_units, line_cost) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pPs = conn.prepareStatement(prescSql);
             PreparedStatement iPs = conn.prepareStatement(itemSql)) {
            pPs.setTimestamp(1, Timestamp.valueOf(now.minusHours(3).minusMinutes(25)));
            pPs.executeUpdate();

            // Item 1: Dolo 650 (Paracetamol) - Twice daily for 5 days = 10 tablets @ 3.00 = 30.00
            iPs.setLong(1, 1L);
            iPs.setLong(2, 1L);
            iPs.setString(3, "1 tablet");
            iPs.setString(4, "TWICE_DAILY");
            iPs.setInt(5, 5);
            iPs.setString(6, "After food");
            iPs.setBigDecimal(7, new BigDecimal("3.00"));
            iPs.setInt(8, 10);
            iPs.setBigDecimal(9, new BigDecimal("30.00"));
            iPs.addBatch();

            // Item 2: Telma 40 (Telmisartan) - Once daily for 30 days = 30 tablets @ 2.40 = 72.00
            iPs.setLong(1, 1L);
            iPs.setLong(2, 18L);
            iPs.setString(3, "1 tablet");
            iPs.setString(4, "ONCE_DAILY");
            iPs.setInt(5, 30);
            iPs.setString(6, "Before breakfast");
            iPs.setBigDecimal(7, new BigDecimal("2.40"));
            iPs.setInt(8, 30);
            iPs.setBigDecimal(9, new BigDecimal("72.00"));
            iPs.addBatch();

            // Item 3: Pantoprazole 40 - Once daily for 10 days = 10 tablets @ 4.00 = 40.00
            iPs.setLong(1, 1L);
            iPs.setLong(2, 14L);
            iPs.setString(3, "1 tablet");
            iPs.setString(4, "ONCE_DAILY");
            iPs.setInt(5, 10);
            iPs.setString(6, "30 minutes before food");
            iPs.setBigDecimal(7, new BigDecimal("4.00"));
            iPs.setInt(8, 10);
            iPs.setBigDecimal(9, new BigDecimal("40.00"));
            iPs.addBatch();

            iPs.executeBatch();
        }

        try (Statement s = conn.createStatement()) {
            s.execute("SELECT setval('tokens_id_seq', (SELECT MAX(id) FROM tokens))");
            s.execute("SELECT setval('prescriptions_id_seq', (SELECT MAX(id) FROM prescriptions))");
        }
    }
}
