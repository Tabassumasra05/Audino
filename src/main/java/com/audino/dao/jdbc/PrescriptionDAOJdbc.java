package com.audino.dao.jdbc;

import com.audino.dao.MedicationDAO;
import com.audino.dao.PrescriptionDAO;
import com.audino.exception.DataAccessException;
import com.audino.factory.AlertFactory;
import com.audino.model.Frequency;
import com.audino.model.Medication;
import com.audino.model.Prescription;
import com.audino.model.PrescriptionAlert;
import com.audino.model.PrescriptionItem;
import com.audino.model.PrescriptionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC implementation of PrescriptionDAO ensuring ACID transactional integrity across
 * prescriptions, owned prescription items, and clinical safety alerts.
 *
 * // OOM: Composition & Transactions – atomically commits or rolls back Prescription and its strictly owned components.
 */
public class PrescriptionDAOJdbc implements PrescriptionDAO {

    private static final Logger logger = LoggerFactory.getLogger(PrescriptionDAOJdbc.class);
    private final DataSource dataSource;
    private final MedicationDAO medicationDAO;

    public PrescriptionDAOJdbc(DataSource dataSource, MedicationDAO medicationDAO) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.medicationDAO = Objects.requireNonNull(medicationDAO);
    }

    @Override
    public Prescription saveWithTransaction(Prescription prescription) {
        String pSql = "INSERT INTO prescriptions (patient_id, doctor_username, token_id, status, total_cost, created_at) " +
                      "VALUES (?, ?, ?, ?, ?, ?)";
        String iSql = "INSERT INTO prescription_items (prescription_id, medication_id, dosage, frequency, duration_days, instructions, unit_cost, total_units, line_cost) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String aSql = "INSERT INTO prescription_alerts (prescription_id, level, type, message, acknowledged, override_note, overridden_by, overridden_at) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insert Prescription Header
                try (PreparedStatement ps = conn.prepareStatement(pSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, prescription.getPatientId());
                    ps.setString(2, prescription.getDoctorUsername());
                    ps.setLong(3, prescription.getTokenId());
                    ps.setString(4, prescription.getStatus().name());
                    ps.setBigDecimal(5, prescription.getTotalCost());
                    ps.setTimestamp(6, Timestamp.valueOf(prescription.getCreatedAt()));

                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            prescription.setId(keys.getLong(1));
                        }
                    }
                }

                // 2. Insert Owned Items (Composition)
                if (!prescription.getItems().isEmpty()) {
                    try (PreparedStatement ips = conn.prepareStatement(iSql, Statement.RETURN_GENERATED_KEYS)) {
                        for (PrescriptionItem item : prescription.getItems()) {
                            ips.setLong(1, prescription.getId());
                            ips.setLong(2, item.getMedicationId());
                            ips.setString(3, item.getDosage());
                            ips.setString(4, item.getFrequency().name());
                            ips.setInt(5, item.getDurationDays());
                            ips.setString(6, item.getInstructions());
                            ips.setBigDecimal(7, item.getUnitCost());
                            ips.setInt(8, item.getTotalUnits());
                            ips.setBigDecimal(9, item.getLineCost());
                            ips.addBatch();
                        }
                        ips.executeBatch();
                        try (ResultSet itemKeys = ips.getGeneratedKeys()) {
                            int idx = 0;
                            while (itemKeys.next() && idx < prescription.getItems().size()) {
                                prescription.getItems().get(idx++).setId(itemKeys.getLong(1));
                            }
                        }
                    }
                }

                // 3. Insert Owned Alerts (Composition)
                if (!prescription.getAlerts().isEmpty()) {
                    try (PreparedStatement aps = conn.prepareStatement(aSql, Statement.RETURN_GENERATED_KEYS)) {
                        for (PrescriptionAlert alert : prescription.getAlerts()) {
                            aps.setLong(1, prescription.getId());
                            aps.setString(2, alert.getLevel().name());
                            aps.setString(3, alert.getType().name());
                            aps.setString(4, alert.getMessage());
                            aps.setBoolean(5, alert.isAcknowledged());
                            aps.setString(6, alert.getOverrideNote());
                            aps.setString(7, alert.getOverriddenBy());
                            aps.setTimestamp(8, alert.getOverriddenAt() != null ? Timestamp.valueOf(alert.getOverriddenAt()) : null);
                            aps.addBatch();
                        }
                        aps.executeBatch();
                        try (ResultSet alertKeys = aps.getGeneratedKeys()) {
                            int idx = 0;
                            while (alertKeys.next() && idx < prescription.getAlerts().size()) {
                                prescription.getAlerts().get(idx++).setId(alertKeys.getLong(1));
                            }
                        }
                    }
                }

                conn.commit();
                logger.info("Prescription #{} successfully persisted atomically with {} items and {} alerts.",
                        prescription.getId(), prescription.getItems().size(), prescription.getAlerts().size());
                return prescription;
            } catch (Exception e) {
                conn.rollback();
                logger.error("Transaction rolled back for prescription save: {}", e.getMessage());
                throw new DataAccessException("Failed to persist prescription transaction", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection failure: {}", e.getMessage());
            throw new DataAccessException("Database error saving prescription", e);
        }
    }

    @Override
    public Optional<Prescription> findById(Long id) {
        String sql = "SELECT * FROM prescriptions WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPrescriptionDetails(conn, rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding prescription ID {}: {}", id, e.getMessage());
            throw new DataAccessException("Failed to find prescription", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Prescription> findByTokenId(Long tokenId) {
        String sql = "SELECT * FROM prescriptions WHERE token_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tokenId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPrescriptionDetails(conn, rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding prescription for token {}: {}", tokenId, e.getMessage());
            throw new DataAccessException("Failed to find prescription by token", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Prescription> findByPatientId(Long patientId) {
        String sql = "SELECT * FROM prescriptions WHERE patient_id = ? ORDER BY created_at DESC";
        List<Prescription> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPrescriptionDetails(conn, rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding prescriptions for patient {}: {}", patientId, e.getMessage());
            throw new DataAccessException("Failed to fetch patient prescriptions", e);
        }
        return list;
    }

    @Override
    public int countTodayPrescriptions() {
        String sql = "SELECT COUNT(*) FROM prescriptions WHERE DATE(created_at) = CURRENT_DATE AND status = 'APPROVED'";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting today's prescriptions: {}", e.getMessage());
            throw new DataAccessException("Failed to count prescriptions", e);
        }
        return 0;
    }

    @Override
    public int countTodayCriticalOverrides() {
        String sql = "SELECT COUNT(*) FROM prescription_alerts a " +
                     "JOIN prescriptions p ON a.prescription_id = p.id " +
                     "WHERE DATE(p.created_at) = CURRENT_DATE AND a.level = 'CRITICAL' AND a.override_note IS NOT NULL";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting critical overrides: {}", e.getMessage());
            throw new DataAccessException("Failed to count overrides", e);
        }
        return 0;
    }

    private Prescription mapPrescriptionDetails(Connection conn, ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long patId = rs.getLong("patient_id");
        String doc = rs.getString("doctor_username");
        Long tokId = rs.getLong("token_id");
        PrescriptionStatus status = PrescriptionStatus.valueOf(rs.getString("status"));
        BigDecimal cost = rs.getBigDecimal("total_cost");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        Prescription prescription = new Prescription(id, patId, doc, tokId, status, cost, createdAt);

        // Load composition items
        String itemSql = "SELECT * FROM prescription_items WHERE prescription_id = ? ORDER BY id ASC";
        try (PreparedStatement ips = conn.prepareStatement(itemSql)) {
            ips.setLong(1, id);
            try (ResultSet irs = ips.executeQuery()) {
                while (irs.next()) {
                    Long itemId = irs.getLong("id");
                    Long medId = irs.getLong("medication_id");
                    String dosage = irs.getString("dosage");
                    Frequency freq = Frequency.valueOf(irs.getString("frequency"));
                    int days = irs.getInt("duration_days");
                    String instructions = irs.getString("instructions");
                    BigDecimal uCost = irs.getBigDecimal("unit_cost");
                    int totalUnits = irs.getInt("total_units");
                    BigDecimal lineCost = irs.getBigDecimal("line_cost");

                    PrescriptionItem item = new PrescriptionItem(itemId, id, medId, dosage, freq, days, instructions, uCost, totalUnits, lineCost);
                    Optional<Medication> medOpt = medicationDAO.findById(medId);
                    medOpt.ifPresent(item::setMedication);
                    prescription.addItem(item);
                }
            }
        }

        // Load composition alerts
        String alertSql = "SELECT * FROM prescription_alerts WHERE prescription_id = ? ORDER BY id ASC";
        try (PreparedStatement aps = conn.prepareStatement(alertSql)) {
            aps.setLong(1, id);
            try (ResultSet ars = aps.executeQuery()) {
                while (ars.next()) {
                    prescription.addAlert(AlertFactory.fromResultSet(ars));
                }
            }
        }

        return prescription;
    }
}
