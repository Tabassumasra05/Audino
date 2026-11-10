package com.audino.dao.jdbc;

import com.audino.dao.PatientDAO;
import com.audino.exception.DataAccessException;
import com.audino.model.Gender;
import com.audino.model.Patient;
import com.audino.model.PatientAllergy;
import com.audino.model.PatientCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC implementation of PatientDAO supporting compositional allergies and conditions persistence.
 */
public class PatientDAOJdbc implements PatientDAO {

    private static final Logger logger = LoggerFactory.getLogger(PatientDAOJdbc.class);
    private final DataSource dataSource;

    public PatientDAOJdbc(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public Optional<Patient> findById(Long id) {
        String sql = "SELECT * FROM patients WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPatientWithDetails(conn, rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding patient by ID {}: {}", id, e.getMessage());
            throw new DataAccessException("Failed to find patient", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Patient> findByPatientId(String patientId) {
        String sql = "SELECT * FROM patients WHERE UPPER(patient_id) = UPPER(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patientId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPatientWithDetails(conn, rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding patient by code {}: {}", patientId, e.getMessage());
            throw new DataAccessException("Failed to find patient by ID code", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Patient> findAll() {
        String sql = "SELECT * FROM patients ORDER BY last_name, first_name";
        List<Patient> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapPatientWithDetails(conn, rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving patients: {}", e.getMessage());
            throw new DataAccessException("Failed to fetch all patients", e);
        }
        return list;
    }

    @Override
    public List<Patient> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        String sql = "SELECT * FROM patients WHERE LOWER(patient_id) LIKE ? OR LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? OR phone LIKE ? ORDER BY last_name";
        List<Patient> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPatientWithDetails(conn, rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching patients with query '{}': {}", query, e.getMessage());
            throw new DataAccessException("Failed to search patients", e);
        }
        return list;
    }

    @Override
    public Patient save(Patient patient) {
        String patSql = "INSERT INTO patients (patient_id, first_name, last_name, date_of_birth, gender, phone, address) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String allgSql = "INSERT INTO patient_allergies (patient_id, allergen, severity) VALUES (?, ?, ?)";
        String condSql = "INSERT INTO patient_conditions (patient_id, condition_name, diagnosed_date) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (patient.getPatientId() == null || patient.getPatientId().isBlank()) {
                    patient.setPatientId(generateNextPatientId(conn));
                }

                try (PreparedStatement ps = conn.prepareStatement(patSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, patient.getPatientId());
                    ps.setString(2, patient.getFirstName().trim());
                    ps.setString(3, patient.getLastName().trim());
                    ps.setDate(4, Date.valueOf(patient.getDateOfBirth()));
                    ps.setString(5, patient.getGender().name());
                    ps.setString(6, patient.getPhone());
                    ps.setString(7, patient.getAddress());

                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            patient.setId(keys.getLong(1));
                        }
                    }
                }

                // Persist composition allergies
                if (!patient.getAllergies().isEmpty()) {
                    try (PreparedStatement aps = conn.prepareStatement(allgSql, Statement.RETURN_GENERATED_KEYS)) {
                        for (PatientAllergy allg : patient.getAllergies()) {
                            aps.setLong(1, patient.getId());
                            aps.setString(2, allg.getAllergen().trim());
                            aps.setString(3, allg.getSeverity());
                            aps.addBatch();
                        }
                        aps.executeBatch();
                    }
                }

                // Persist composition chronic conditions
                if (!patient.getConditions().isEmpty()) {
                    try (PreparedStatement cps = conn.prepareStatement(condSql, Statement.RETURN_GENERATED_KEYS)) {
                        for (PatientCondition cond : patient.getConditions()) {
                            cps.setLong(1, patient.getId());
                            cps.setString(2, cond.getConditionName().trim());
                            cps.setDate(3, cond.getDiagnosedDate() != null ? Date.valueOf(cond.getDiagnosedDate()) : null);
                            cps.addBatch();
                        }
                        cps.executeBatch();
                    }
                }

                conn.commit();
                return patient;
            } catch (Exception e) {
                conn.rollback();
                logger.error("Transaction rolled back for patient save: {}", e.getMessage());
                throw new DataAccessException("Failed to persist patient and clinical history", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Connection failure during patient save: {}", e.getMessage());
            throw new DataAccessException("Database error saving patient", e);
        }
    }

    @Override
    public void update(Patient patient) {
        String sql = "UPDATE patients SET first_name = ?, last_name = ?, date_of_birth = ?, gender = ?, phone = ?, address = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patient.getFirstName().trim());
            ps.setString(2, patient.getLastName().trim());
            ps.setDate(3, Date.valueOf(patient.getDateOfBirth()));
            ps.setString(4, patient.getGender().name());
            ps.setString(5, patient.getPhone());
            ps.setString(6, patient.getAddress());
            ps.setLong(7, patient.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating patient ID {}: {}", patient.getId(), e.getMessage());
            throw new DataAccessException("Failed to update patient", e);
        }
    }

    @Override
    public List<PatientAllergy> findAllergiesByPatientId(Long patientId) {
        String sql = "SELECT * FROM patient_allergies WHERE patient_id = ? ORDER BY allergen";
        List<PatientAllergy> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    String allergen = rs.getString("allergen");
                    String severity = rs.getString("severity");
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                    list.add(new PatientAllergy(id, patientId, allergen, severity, createdAt));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding allergies for patient {}: {}", patientId, e.getMessage());
            throw new DataAccessException("Failed to fetch patient allergies", e);
        }
        return list;
    }

    @Override
    public List<PatientCondition> findConditionsByPatientId(Long patientId) {
        String sql = "SELECT * FROM patient_conditions WHERE patient_id = ? ORDER BY condition_name";
        List<PatientCondition> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    String name = rs.getString("condition_name");
                    Date d = rs.getDate("diagnosed_date");
                    LocalDate date = d != null ? d.toLocalDate() : null;
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                    list.add(new PatientCondition(id, patientId, name, date, createdAt));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding conditions for patient {}: {}", patientId, e.getMessage());
            throw new DataAccessException("Failed to fetch patient conditions", e);
        }
        return list;
    }

    @Override
    public boolean existsByPhoneAndName(String phone, String firstName, String lastName) {
        String sql = "SELECT 1 FROM patients WHERE phone = ? AND LOWER(first_name) = LOWER(?) AND LOWER(last_name) = LOWER(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            ps.setString(2, firstName.trim());
            ps.setString(3, lastName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking duplicate patient: {}", e.getMessage());
            throw new DataAccessException("Failed to check duplicate patient", e);
        }
    }

    @Override
    public String getNextPatientId() {
        try (Connection conn = dataSource.getConnection()) {
            return generateNextPatientId(conn);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to generate patient ID", e);
        }
    }

    private String generateNextPatientId(Connection conn) throws SQLException {
        String sql = "SELECT MAX(id) FROM patients";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            long maxId = 1000;
            if (rs.next()) {
                maxId = Math.max(maxId, rs.getLong(1));
            }
            return "P-" + (maxId + 1);
        }
    }

    private Patient mapPatientWithDetails(Connection conn, ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String patientId = rs.getString("patient_id");
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        LocalDate dob = rs.getDate("date_of_birth").toLocalDate();
        Gender gender = Gender.valueOf(rs.getString("gender"));
        String phone = rs.getString("phone");
        String address = rs.getString("address");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        List<PatientAllergy> allergies = findAllergiesByPatientId(id);
        List<PatientCondition> conditions = findConditionsByPatientId(id);

        return new Patient(id, patientId, firstName, lastName, dob, gender, phone, address, allergies, conditions, createdAt);
    }
}
