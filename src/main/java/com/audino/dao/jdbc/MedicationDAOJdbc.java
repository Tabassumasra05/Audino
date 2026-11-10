package com.audino.dao.jdbc;

import com.audino.dao.MedicationDAO;
import com.audino.exception.DataAccessException;
import com.audino.factory.MedicationFactory;
import com.audino.model.Medication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC implementation of MedicationDAO with active ingredients loading and polymorphic instantiation.
 */
public class MedicationDAOJdbc implements MedicationDAO {

    private static final Logger logger = LoggerFactory.getLogger(MedicationDAOJdbc.class);
    private final DataSource dataSource;

    public MedicationDAOJdbc(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public Optional<Medication> findById(Long id) {
        String sql = "SELECT * FROM medications WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    List<String> ingredients = findIngredientsByMedicationId(id);
                    return Optional.of(MedicationFactory.fromResultSet(rs, ingredients));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding medication ID {}: {}", id, e.getMessage());
            throw new DataAccessException("Failed to find medication", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Medication> findAll() {
        String sql = "SELECT * FROM medications ORDER BY brand_name, generic_name";
        List<Medication> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Long id = rs.getLong("id");
                List<String> ingredients = findIngredientsByMedicationId(id);
                list.add(MedicationFactory.fromResultSet(rs, ingredients));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving medications: {}", e.getMessage());
            throw new DataAccessException("Failed to fetch all medications", e);
        }
        return list;
    }

    @Override
    public List<Medication> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        String sql = "SELECT * FROM medications WHERE LOWER(brand_name) LIKE ? OR LOWER(generic_name) LIKE ? OR rxnorm_code LIKE ? ORDER BY brand_name";
        List<Medication> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    List<String> ingredients = findIngredientsByMedicationId(id);
                    list.add(MedicationFactory.fromResultSet(rs, ingredients));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching medications: {}", e.getMessage());
            throw new DataAccessException("Failed to search medications", e);
        }
        return list;
    }

    @Override
    public Medication save(Medication medication) {
        String medSql = "INSERT INTO medications (generic_name, brand_name, rxnorm_code, type, strength, price_per_unit) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
        String ingSql = "INSERT INTO medication_ingredients (medication_id, ingredient_name) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(medSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, medication.getGenericName().trim());
                    ps.setString(2, medication.getBrandName().trim());
                    ps.setString(3, medication.getRxNormCode());
                    ps.setString(4, medication.getType().name());
                    ps.setString(5, medication.getStrength().trim());
                    ps.setBigDecimal(6, medication.getPricePerUnit());

                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            medication.setId(keys.getLong(1));
                        }
                    }
                }

                if (!medication.getIngredients().isEmpty()) {
                    try (PreparedStatement ips = conn.prepareStatement(ingSql)) {
                        for (String ing : medication.getIngredients()) {
                            ips.setLong(1, medication.getId());
                            ips.setString(2, ing.trim());
                            ips.addBatch();
                        }
                        ips.executeBatch();
                    }
                }

                conn.commit();
                return medication;
            } catch (Exception e) {
                conn.rollback();
                logger.error("Error saving medication: {}", e.getMessage());
                throw new DataAccessException("Failed to save medication", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection error: {}", e.getMessage());
            throw new DataAccessException("Database failure saving medication", e);
        }
    }

    @Override
    public void update(Medication medication) {
        String sql = "UPDATE medications SET generic_name = ?, brand_name = ?, rxnorm_code = ?, type = ?, strength = ?, price_per_unit = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, medication.getGenericName().trim());
            ps.setString(2, medication.getBrandName().trim());
            ps.setString(3, medication.getRxNormCode());
            ps.setString(4, medication.getType().name());
            ps.setString(5, medication.getStrength().trim());
            ps.setBigDecimal(6, medication.getPricePerUnit());
            ps.setLong(7, medication.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating medication ID {}: {}", medication.getId(), e.getMessage());
            throw new DataAccessException("Failed to update medication", e);
        }
    }

    @Override
    public void updatePrice(Long medicationId, BigDecimal newPrice) {
        String sql = "UPDATE medications SET price_per_unit = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newPrice);
            ps.setLong(2, medicationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating price for medication {}: {}", medicationId, e.getMessage());
            throw new DataAccessException("Failed to update medication price", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM medications WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting medication {}: {}", id, e.getMessage());
            throw new DataAccessException("Failed to delete medication", e);
        }
    }

    @Override
    public List<String> findIngredientsByMedicationId(Long medicationId) {
        String sql = "SELECT ingredient_name FROM medication_ingredients WHERE medication_id = ? ORDER BY ingredient_name";
        List<String> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, medicationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("ingredient_name"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding ingredients for medication {}: {}", medicationId, e.getMessage());
            throw new DataAccessException("Failed to fetch medication ingredients", e);
        }
        return list;
    }
}
