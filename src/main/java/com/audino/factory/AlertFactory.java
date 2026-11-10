package com.audino.factory;

import com.audino.model.AlertLevel;
import com.audino.model.AlertType;
import com.audino.model.PrescriptionAlert;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Factory class for creating clinical prescription alerts and UI alert cards.
 *
 * // OOM: Factory Pattern – standardizes creation of clinical interaction alert models.
 */
public class AlertFactory {

    private AlertFactory() {
        // Utility factory
    }

    public static PrescriptionAlert createAlert(AlertLevel level, AlertType type, String message) {
        return new PrescriptionAlert(level, type, message);
    }

    public static PrescriptionAlert fromResultSet(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long prescriptionId = rs.getLong("prescription_id");
        AlertLevel level = AlertLevel.valueOf(rs.getString("level"));
        AlertType type = AlertType.valueOf(rs.getString("type"));
        String message = rs.getString("message");
        boolean acknowledged = rs.getBoolean("acknowledged");
        String overrideNote = rs.getString("override_note");
        String overriddenBy = rs.getString("overridden_by");
        Timestamp ts = rs.getTimestamp("overridden_at");
        LocalDateTime overriddenAt = ts != null ? ts.toLocalDateTime() : null;

        return new PrescriptionAlert(id, prescriptionId, level, type, message, acknowledged, overrideNote, overriddenBy, overriddenAt);
    }
}
