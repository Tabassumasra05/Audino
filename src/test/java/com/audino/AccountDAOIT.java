package com.audino;

import com.audino.dao.AccountDAO;
import com.audino.dao.jdbc.AccountDAOJdbc;
import com.audino.model.Account;
import com.audino.model.Doctor;
import com.audino.model.Receptionist;
import com.audino.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration Test: AccountDAO Native JDBC Operations")
class AccountDAOIT extends AbstractDAOIntegrationTest {

    private AccountDAO accountDAO;

    @BeforeEach
    void setUp() {
        accountDAO = new AccountDAOJdbc(dataSource);
    }

    @Test
    @DisplayName("findByUsername() retrieves seeded doctor account with correct specialization")
    void testFindSeededDoctor() {
        Optional<Account> docOpt = accountDAO.findByUsername("doctor");
        assertThat(docOpt).isPresent();

        Account account = docOpt.get();
        assertThat(account.getRole()).isEqualTo(Role.DOCTOR);
        assertThat(account).isInstanceOf(Doctor.class);

        Doctor doctor = (Doctor) account;
        assertThat(doctor.getSpecialization()).contains("Internal Medicine");
        assertThat(doctor.getRegistrationNumber()).isEqualTo("MCI-48291");
        assertThat(doctor.isActive()).isTrue();
    }

    @Test
    @DisplayName("save() persists new doctor and receptionist accounts with generated IDs")
    void testSaveAndFindAccount() {
        String uniqueUser = "doc_" + UUID.randomUUID().toString().substring(0, 8);
        String hash = BCrypt.hashpw("Password123", BCrypt.gensalt(4));

        Doctor newDoctor = new Doctor(null, uniqueUser, hash, "Dr. Test Integration", true, "Pediatrics", "REG-88221");
        Account saved = accountDAO.save(newDoctor);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isPositive();

        Optional<Account> fetched = accountDAO.findById(saved.getId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getUsername()).isEqualTo(uniqueUser);
        assertThat(((Doctor) fetched.get()).getSpecialization()).isEqualTo("Pediatrics");

        // Save Receptionist
        String recUser = "rec_" + UUID.randomUUID().toString().substring(0, 8);
        Receptionist newRec = new Receptionist(null, recUser, hash, "Desk Assistant", true);
        Account savedRec = accountDAO.save(newRec);

        assertThat(savedRec.getId()).isNotNull();
        assertThat(savedRec.getRole()).isEqualTo(Role.RECEPTIONIST);
    }

    @Test
    @DisplayName("update() modifies staff profile data")
    void testUpdateAccount() {
        String uniqueUser = "doc_update_" + UUID.randomUUID().toString().substring(0, 8);
        String hash = BCrypt.hashpw("Password123", BCrypt.gensalt(4));

        Doctor doc = new Doctor(null, uniqueUser, hash, "Dr. Initial Name", true, "General", "REG-1111");
        Account saved = accountDAO.save(doc);

        Doctor toUpdate = new Doctor(saved.getId(), uniqueUser, hash, "Dr. Updated Name", true, "Cardiology", "REG-2222");
        accountDAO.update(toUpdate);

        Optional<Account> updatedOpt = accountDAO.findById(saved.getId());
        assertThat(updatedOpt).isPresent();
        Doctor updated = (Doctor) updatedOpt.get();
        assertThat(updated.getFullName()).isEqualTo("Dr. Updated Name");
        assertThat(updated.getSpecialization()).isEqualTo("Cardiology");
        assertThat(updated.getRegistrationNumber()).isEqualTo("REG-2222");
    }

    @Test
    @DisplayName("updatePassword() updates cryptographic hash in database")
    void testUpdatePassword() {
        String uniqueUser = "doc_pwd_" + UUID.randomUUID().toString().substring(0, 8);
        String initialHash = BCrypt.hashpw("OldPassword", BCrypt.gensalt(4));

        accountDAO.save(new Doctor(null, uniqueUser, initialHash, "Dr. Password Test", true, "General", "REG-3333"));

        String newHash = BCrypt.hashpw("NewPassword456", BCrypt.gensalt(4));
        accountDAO.updatePassword(uniqueUser, newHash);

        Optional<Account> fetched = accountDAO.findByUsername(uniqueUser);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getPasswordHash()).isEqualTo(newHash);
        assertThat(BCrypt.checkpw("NewPassword456", fetched.get().getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("setActiveStatus() activates and deactivates accounts, affecting countActiveStaff()")
    void testSetActiveAndCount() {
        int initialActive = accountDAO.countActiveStaff();

        String uniqueUser = "staff_toggle_" + UUID.randomUUID().toString().substring(0, 8);
        String hash = BCrypt.hashpw("Password123", BCrypt.gensalt(4));
        Account saved = accountDAO.save(new Receptionist(null, uniqueUser, hash, "Toggle Staff", true));

        assertThat(accountDAO.countActiveStaff()).isEqualTo(initialActive + 1);

        accountDAO.setActiveStatus(uniqueUser, false);

        Optional<Account> toggled = accountDAO.findById(saved.getId());
        assertThat(toggled).isPresent();
        assertThat(toggled.get().isActive()).isFalse();
        assertThat(accountDAO.countActiveStaff()).isEqualTo(initialActive);
    }
}
