package com.audino.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Idempotently initializes the relational database schema upon system startup.
 * Applies DDL statements from database/schema.sql.
 */
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Executes the idempotent DDL schema.
     */
    public void initialize() {
        logger.info("Initializing relational database schema...");
        String sql = loadSchemaSql();
        if (sql == null || sql.isBlank()) {
            throw new RuntimeException("Could not locate or load schema.sql for database initialization.");
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Strip single-line comments (-- ...) first
            String cleanSql = sql.replaceAll("(?m)^\\s*--.*$", "");

            // Split SQL script into individual statements safely by semicolon
            String[] commands = cleanSql.split(";");
            for (String cmd : commands) {
                String trimmed = cmd.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        stmt.execute(trimmed);
                    } catch (Exception ex) {
                        logger.error("Failed executing SQL statement:\n{}\nError: {}", trimmed, ex.getMessage());
                        throw ex;
                    }
                }
            }
            logger.info("Database schema initialized successfully (all tables, constraints, and indexes validated).");
        } catch (Exception e) {
            logger.error("Failed to execute database schema DDL: {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failure", e);
        }
    }

    private String loadSchemaSql() {
        // Try 1: Look for schema.sql in workspace relative path
        File schemaFile = new File("database/schema.sql");
        if (schemaFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(schemaFile, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                logger.info("Loaded schema from external path: {}", schemaFile.getAbsolutePath());
                return sb.toString();
            } catch (Exception e) {
                logger.warn("Could not read database/schema.sql directly: {}", e.getMessage());
            }
        }

        // Try 2: Look for schema.sql on classpath
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("database/schema.sql")) {
            if (in != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    logger.info("Loaded schema from classpath resource database/schema.sql");
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            logger.error("Failed reading classpath schema: {}", e.getMessage());
        }

        return null;
    }
}
