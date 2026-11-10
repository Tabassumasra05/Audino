package com.audino;

import com.audino.factory.AccountFactory;
import com.audino.model.Account;
import com.audino.model.Admin;
import com.audino.model.Doctor;
import com.audino.model.Receptionist;
import com.audino.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit Test: AccountFactory Pattern & Polymorphic Routing")
class AccountFactoryTest {

    @Test
    @DisplayName("Factory instantiates Admin with correct permissions and FXML dashboard")
    void testCreateAdmin() {
        Account acc = AccountFactory.createAccount(1L, "admin", "hash", "Admin User", Role.ADMIN, null, null, true, LocalDateTime.now());

        assertThat(acc).isInstanceOf(Admin.class);
        assertThat(acc.getRole()).isEqualTo(Role.ADMIN);
        assertThat(acc.getDashboardFxml()).isEqualTo("/fxml/admin_dashboard.fxml");
        assertThat(acc.getPermissions()).contains("STAFF_MANAGE");
    }

    @Test
    @DisplayName("Factory instantiates Doctor with clinical specialization and registration number")
    void testCreateDoctor() {
        Account acc = AccountFactory.createAccount(2L, "doctor", "hash", "Dr. Mehta", Role.DOCTOR, "Cardiology", "MCI-12345", true, LocalDateTime.now());

        assertThat(acc).isInstanceOf(Doctor.class);
        Doctor doc = (Doctor) acc;
        assertThat(doc.getSpecialization()).isEqualTo("Cardiology");
        assertThat(doc.getRegistrationNumber()).isEqualTo("MCI-12345");
        assertThat(doc.getDashboardFxml()).isEqualTo("/fxml/doctor_consultation.fxml");
    }

    @Test
    @DisplayName("Factory instantiates Receptionist with triage permissions")
    void testCreateReceptionist() {
        Account acc = AccountFactory.createAccount(3L, "reception", "hash", "Neha Verma", Role.RECEPTIONIST, null, null, true, LocalDateTime.now());

        assertThat(acc).isInstanceOf(Receptionist.class);
        assertThat(acc.getDashboardFxml()).isEqualTo("/fxml/receptionist_dashboard.fxml");
        assertThat(acc.getPermissions()).contains("TOKEN_ISSUE");
    }

    @Test
    @DisplayName("Null role throws IllegalArgumentException")
    void testNullRoleThrows() {
        assertThatThrownBy(() -> AccountFactory.createAccount(4L, "user", "hash", "Name", null, null, null, true, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
