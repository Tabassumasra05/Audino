package com.audino;

import com.audino.factory.AlertFactory;
import com.audino.model.AlertLevel;
import com.audino.model.AlertType;
import com.audino.model.PrescriptionAlert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Test: AlertFactory Pattern")
class AlertFactoryTest {

    @Test
    @DisplayName("Factory instantiates PrescriptionAlert with specified severity and type")
    void testCreateAlert() {
        PrescriptionAlert alert = AlertFactory.createAlert(AlertLevel.CRITICAL, AlertType.DRUG_DRUG, "Warfarin + Ibuprofen");

        assertThat(alert.getLevel()).isEqualTo(AlertLevel.CRITICAL);
        assertThat(alert.getType()).isEqualTo(AlertType.DRUG_DRUG);
        assertThat(alert.getMessage()).isEqualTo("Warfarin + Ibuprofen");
        assertThat(alert.isCritical()).isTrue();
        assertThat(alert.isAcknowledged()).isFalse();
    }
}
