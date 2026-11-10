package com.audino.dao.jdbc;

import com.audino.dao.VitalsDAO;
import com.audino.exception.DataAccessException;
import com.audino.model.PatientVitals;
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
 * JDBC implementation of VitalsDAO.
 */
public class VitalsDAOJdbc implements VitalsDAO {

    private static final Logger logger = LoggerFactory.getLogger(VitalsDAOJdbc.class);
    private final DataSource dataSource;

    public VitalsDAOJdbc(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public PatientVitals save(PatientVitals vitals) {
        String sql = "INSERT INTO patient_vitals (patient_id, recorded_at, height_cm, weight_kg, systolic_bp, diastolic_bp, pulse_bpm, temperature_c, spo2_percent) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, vitals.getPatientId());
            ps.setTimestamp(2, Timestamp.valueOf(vitals.getRecordedAt()));
            ps.setBigDecimal(3, BigDecimal.valueOf(vitals.getHeightCm()));
            ps.setBigDecimal(4, BigDecimal.valueOf(vitals.getWeightKg()));
            ps.setInt(5, vitals.getSystolicBp());
            ps.setInt(6, vitals.getDiastolicBp());
            ps.setInt(7, vitals.getPulseBpm());
            ps.setBigDecimal(8, BigDecimal.valueOf(vitals.getTemperatureC()));
            ps.setInt(9, vitals.getSpo2Percent());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    vitals.setId(keys.getLong(1));
                }
            }
            return vitals;
        } catch (SQLException e) {
            logger.error("Error saving patient vitals: {}", e.getMessage());
            throw new DataAccessException("Failed to persist patient vitals", e);
        }
    }

    @Override
    public Optional<PatientVitals> findLatestByPatientId(Long patientId) {
        String sql = "SELECT * FROM patient_vitals WHERE patient_id = ? ORDER BY recorded_at DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapVitals(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching latest vitals for patient {}: {}", patientId, e.getMessage());
            throw new DataAccessException("Failed to fetch latest vitals", e);
        }
        return Optional.empty();
    }

    @Override
    public List<PatientVitals> findHistoryByPatientId(Long patientId, int limit) {
        String sql = "SELECT * FROM patient_vitals WHERE patient_id = ? ORDER BY recorded_at ASC LIMIT ?";
        List<PatientVitals> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            ps.setInt(2, limit > 0 ? limit : 10);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapVitals(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching vitals history for patient {}: {}", patientId, e.getMessage());
            throw new DataAccessException("Failed to fetch vitals history", e);
        }
        return list;
    }

    private PatientVitals mapVitals(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long patId = rs.getLong("patient_id");
        Timestamp ts = rs.getTimestamp("recorded_at");
        LocalDateTime recordedAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        double height = rs.getDouble("height_cm");
        double weight = rs.getDouble("weight_kg");
        int systolic = rs.getInt("systolic_bp");
        int diastolic = rs.getInt("diastolic_bp");
        int pulse = rs.getInt("pulse_bpm");
        double temp = rs.getDouble("temperature_c");
        int spo2 = rs.getInt("spo2_percent");

        return new PatientVitals(id, patId, recordedAt, height, weight, systolic, diastolic, pulse, temp, spo2);
    }
}
