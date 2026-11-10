-- ============================================================================
-- Audino: Intelligent Clinical Decision Support & EMR System
-- Relational Database Schema DDL (PostgreSQL 14+)
-- Idempotent Execution: Safe for multiple runs
-- ============================================================================

-- 1. Accounts Table (Staff Authentication & Role-Based Access)
CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'DOCTOR', 'RECEPTIONIST')),
    specialization VARCHAR(100),
    registration_no VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_accounts_username ON accounts(username);
CREATE INDEX IF NOT EXISTS idx_accounts_role ON accounts(role);

-- 2. Patients Table (Demographic Identification)
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    patient_id VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    phone VARCHAR(15) NOT NULL,
    address TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patients_patient_id ON patients(patient_id);
CREATE INDEX IF NOT EXISTS idx_patients_phone ON patients(phone);
CREATE INDEX IF NOT EXISTS idx_patients_names ON patients(last_name, first_name);

-- 3. Patient Allergies Table (Composition: ON DELETE CASCADE)
CREATE TABLE IF NOT EXISTS patient_allergies (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    allergen VARCHAR(100) NOT NULL,
    severity VARCHAR(20) DEFAULT 'MODERATE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patient_allergies_patient ON patient_allergies(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_allergies_allergen ON patient_allergies(allergen);

-- 4. Patient Chronic Conditions Table (Composition: ON DELETE CASCADE)
CREATE TABLE IF NOT EXISTS patient_conditions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    condition_name VARCHAR(100) NOT NULL,
    diagnosed_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patient_conditions_patient ON patient_conditions(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_conditions_name ON patient_conditions(condition_name);

-- 5. Patient Vitals Table (Longitudinal Physiological History)
CREATE TABLE IF NOT EXISTS patient_vitals (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    height_cm NUMERIC(5,2) NOT NULL CHECK (height_cm BETWEEN 30 AND 250),
    weight_kg NUMERIC(5,2) NOT NULL CHECK (weight_kg BETWEEN 2 AND 300),
    systolic_bp INT NOT NULL CHECK (systolic_bp BETWEEN 50 AND 260),
    diastolic_bp INT NOT NULL CHECK (diastolic_bp BETWEEN 30 AND 180),
    pulse_bpm INT NOT NULL CHECK (pulse_bpm BETWEEN 30 AND 220),
    temperature_c NUMERIC(4,2) NOT NULL CHECK (temperature_c BETWEEN 33.0 AND 43.0),
    spo2_percent INT NOT NULL CHECK (spo2_percent BETWEEN 50 AND 100)
);

CREATE INDEX IF NOT EXISTS idx_patient_vitals_patient ON patient_vitals(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_vitals_recorded ON patient_vitals(recorded_at);

-- 6. Medications Formulary Table
CREATE TABLE IF NOT EXISTS medications (
    id BIGSERIAL PRIMARY KEY,
    generic_name VARCHAR(100) NOT NULL,
    brand_name VARCHAR(100) NOT NULL,
    rxnorm_code VARCHAR(20),
    type VARCHAR(20) NOT NULL CHECK (type IN ('TABLET', 'CAPSULE', 'LIQUID', 'INJECTION')),
    strength VARCHAR(50) NOT NULL,
    price_per_unit NUMERIC(10,2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_medications_generic ON medications(generic_name);
CREATE INDEX IF NOT EXISTS idx_medications_brand ON medications(brand_name);
CREATE INDEX IF NOT EXISTS idx_medications_rxnorm ON medications(rxnorm_code);

-- 7. Medication Ingredients Table (Composition: ON DELETE CASCADE)
CREATE TABLE IF NOT EXISTS medication_ingredients (
    id BIGSERIAL PRIMARY KEY,
    medication_id BIGINT NOT NULL REFERENCES medications(id) ON DELETE CASCADE,
    ingredient_name VARCHAR(100) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_med_ingredients_med ON medication_ingredients(medication_id);
CREATE INDEX IF NOT EXISTS idx_med_ingredients_name ON medication_ingredients(ingredient_name);

-- 8. Clinical Interaction Rules Table
CREATE TABLE IF NOT EXISTS interaction_rules (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(30) NOT NULL CHECK (type IN ('DRUG_DRUG', 'DRUG_ALLERGY', 'DRUG_CONDITION', 'DUPLICATE_THERAPY')),
    keyword1 VARCHAR(100) NOT NULL,
    keyword2 VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('CRITICAL', 'WARNING', 'INFO')),
    description TEXT NOT NULL,
    recommendation TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rules_type ON interaction_rules(type);
CREATE INDEX IF NOT EXISTS idx_rules_keywords ON interaction_rules(keyword1, keyword2);

-- 9. Outpatient Queue Tokens Table
CREATE TABLE IF NOT EXISTS tokens (
    id BIGSERIAL PRIMARY KEY,
    token_no VARCHAR(10) NOT NULL,
    token_date DATE NOT NULL,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    doctor_username VARCHAR(50) REFERENCES accounts(username),
    status VARCHAR(20) NOT NULL CHECK (status IN ('WAITING', 'IN_CONSULTATION', 'COMPLETED', 'CANCELLED')),
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    called_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_token_date_number UNIQUE (token_date, token_no)
);

CREATE INDEX IF NOT EXISTS idx_tokens_date_status ON tokens(token_date, status);
CREATE INDEX IF NOT EXISTS idx_tokens_patient ON tokens(patient_id);
CREATE INDEX IF NOT EXISTS idx_tokens_doctor ON tokens(doctor_username);

-- 10. Prescriptions Table
CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    doctor_username VARCHAR(50) NOT NULL REFERENCES accounts(username),
    token_id BIGINT NOT NULL REFERENCES tokens(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'APPROVED', 'CANCELLED')),
    total_cost NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prescriptions_patient ON prescriptions(patient_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_doctor ON prescriptions(doctor_username);
CREATE INDEX IF NOT EXISTS idx_prescriptions_token ON prescriptions(token_id);

-- 11. Prescription Items Table (Composition: ON DELETE CASCADE)
CREATE TABLE IF NOT EXISTS prescription_items (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    medication_id BIGINT NOT NULL REFERENCES medications(id),
    dosage VARCHAR(50) NOT NULL,
    frequency VARCHAR(30) NOT NULL CHECK (frequency IN ('ONCE_DAILY', 'TWICE_DAILY', 'THRICE_DAILY', 'FOUR_TIMES_DAILY', 'BEDTIME', 'SOS')),
    duration_days INT NOT NULL CHECK (duration_days > 0),
    instructions VARCHAR(100),
    unit_cost NUMERIC(10,2) NOT NULL,
    total_units INT NOT NULL CHECK (total_units > 0),
    line_cost NUMERIC(10,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_presc_items_prescription ON prescription_items(prescription_id);
CREATE INDEX IF NOT EXISTS idx_presc_items_medication ON prescription_items(medication_id);

-- 12. Prescription Alerts & Overrides Table (Composition: ON DELETE CASCADE)
CREATE TABLE IF NOT EXISTS prescription_alerts (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    level VARCHAR(20) NOT NULL CHECK (level IN ('CRITICAL', 'WARNING', 'INFO')),
    type VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    override_note TEXT,
    overridden_by VARCHAR(50) REFERENCES accounts(username),
    overridden_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_presc_alerts_prescription ON prescription_alerts(prescription_id);
CREATE INDEX IF NOT EXISTS idx_presc_alerts_level ON prescription_alerts(level);

-- 13. Audit Logs Table (Forensic Security & Override Trail)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    operator_username VARCHAR(50) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50),
    details TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_operator ON audit_logs(operator_username);
CREATE INDEX IF NOT EXISTS idx_audit_logs_operation ON audit_logs(operation_type);
