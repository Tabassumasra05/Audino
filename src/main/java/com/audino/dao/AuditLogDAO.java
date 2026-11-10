package com.audino.dao;

import com.audino.model.AuditLog;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object contract for forensic audit logs.
 */
public interface AuditLogDAO {

    AuditLog save(AuditLog log);

    List<AuditLog> findRecent(int limit);

    List<AuditLog> search(String operator, String operationType, LocalDate startDate, LocalDate endDate, int limit);
}
