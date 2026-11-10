package com.audino.dao;

import com.audino.model.Account;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for staff account entities.
 *
 * // OOM: DAO Pattern & Interface Segregation – abstracts account persistence operations.
 */
public interface AccountDAO {

    Optional<Account> findById(Long id);

    Optional<Account> findByUsername(String username);

    List<Account> findAll();

    Account save(Account account);

    void update(Account account);

    void updatePassword(String username, String newPasswordHash);

    void setActiveStatus(String username, boolean active);

    int countActiveStaff();
}
