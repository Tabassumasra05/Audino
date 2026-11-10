package com.audino.factory;

import com.audino.model.CapsuleMedication;
import com.audino.model.InjectionMedication;
import com.audino.model.LiquidMedication;
import com.audino.model.Medication;
import com.audino.model.MedicationType;
import com.audino.model.TabletMedication;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Factory class encapsulating the polymorphic creation of Medication subclasses.
 *
 * // OOM: Factory Pattern – encapsulates polymorphic instantiation of Medication subclasses based on delivery type.
 */
public class MedicationFactory {

    private MedicationFactory() {
        // Utility factory
    }

    /**
     * Creates a Medication instance based on its delivery type.
     */
    public static Medication createMedication(MedicationType type, Long id, String genericName,
                                              String brandName, String rxNormCode, String strength,
                                              List<String> ingredients, BigDecimal pricePerUnit,
                                              LocalDateTime createdAt) {
        if (type == null) {
            throw new IllegalArgumentException("Medication type must not be null");
        }
        switch (type) {
            case TABLET:
                return new TabletMedication(id, genericName, brandName, rxNormCode, strength, ingredients, pricePerUnit, createdAt);
            case CAPSULE:
                return new CapsuleMedication(id, genericName, brandName, rxNormCode, strength, ingredients, pricePerUnit, createdAt);
            case LIQUID:
                return new LiquidMedication(id, genericName, brandName, rxNormCode, strength, ingredients, pricePerUnit, createdAt);
            case INJECTION:
                return new InjectionMedication(id, genericName, brandName, rxNormCode, strength, ingredients, pricePerUnit, createdAt);
            default:
                throw new IllegalArgumentException("Unsupported medication type: " + type);
        }
    }

    public static Medication createMedication(Long id, String genericName, String brandName,
                                              String rxNormCode, MedicationType type, String strength,
                                              BigDecimal pricePerUnit, List<String> ingredients) {
        return createMedication(type, id, genericName, brandName, rxNormCode, strength, ingredients, pricePerUnit, LocalDateTime.now());
    }

    public static Medication fromResultSet(ResultSet rs, List<String> ingredients) throws SQLException {
        Long id = rs.getLong("id");
        String genericName = rs.getString("generic_name");
        String brandName = rs.getString("brand_name");
        String rxNormCode = rs.getString("rxnorm_code");
        MedicationType type = MedicationType.valueOf(rs.getString("type"));
        String strength = rs.getString("strength");
        BigDecimal pricePerUnit = rs.getBigDecimal("price_per_unit");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        return createMedication(type, id, genericName, brandName, rxNormCode, strength, ingredients, pricePerUnit, createdAt);
    }
}
