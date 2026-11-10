package com.audino.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton configuration and database connection manager.
 * Manages HikariCP connection pooling and provides zero-setup fallback
 * to embedded PostgreSQL when an external server is not detected on localhost:5432.
 *
 * // OOM: Singleton Pattern - guarantees single DataSource and Configuration instance across the application lifecycle.
 */
public class ConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private static volatile ConfigurationManager instance;

    private final Properties properties = new Properties();
    private HikariDataSource dataSource;
    private EmbeddedPostgres embeddedPostgres;
    private boolean isEmbedded = false;

    private ConfigurationManager() {
        loadProperties();
        initializeDataSource();
        registerShutdownHook();
    }

    public static ConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    instance = new ConfigurationManager();
                }
            }
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                properties.load(in);
                logger.info("Loaded configuration properties from application.properties");
            } else {
                logger.warn("application.properties not found on classpath, using defaults");
            }
        } catch (IOException e) {
            logger.error("Failed to load application.properties: {}", e.getMessage());
        }
    }

    private void initializeDataSource() {
        String dbUrl = properties.getProperty("db.url", "jdbc:postgresql://localhost:5432/audino");
        String dbUser = properties.getProperty("db.user", "postgres");
        String dbPass = properties.getProperty("db.password", "postgres");
        int poolSize = Integer.parseInt(properties.getProperty("db.pool.size", "10"));
        boolean fallbackEnabled = Boolean.parseBoolean(properties.getProperty("db.embedded.fallback", "true"));

        // Extract database name (default: audino)
        String targetDb = "audino";
        int lastSlash = dbUrl.lastIndexOf('/');
        if (lastSlash != -1) {
            String sub = dbUrl.substring(lastSlash + 1);
            int qMark = sub.indexOf('?');
            targetDb = (qMark != -1) ? sub.substring(0, qMark) : sub;
        }

        // Step 1: Probe external database on port 5432
        boolean externalAvailable = isPortAvailable("localhost", 5432);

        if (externalAvailable) {
            logger.info("Detected running PostgreSQL service on localhost:5432. Ensuring database '{}' exists...", targetDb);
            try {
                // Connect to admin postgres db first to ensure target database exists
                String adminUrl = "jdbc:postgresql://localhost:5432/postgres";
                try (Connection adminConn = java.sql.DriverManager.getConnection(adminUrl, dbUser, dbPass);
                     java.sql.Statement stmt = adminConn.createStatement()) {
                    
                    try (java.sql.ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + targetDb + "'")) {
                        if (!rs.next()) {
                            stmt.execute("CREATE DATABASE " + targetDb);
                            logger.info("Created target database '{}' on external PostgreSQL.", targetDb);
                        } else {
                            logger.info("Target database '{}' already exists on external PostgreSQL.", targetDb);
                        }
                    }
                }

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl("jdbc:postgresql://localhost:5432/" + targetDb);
                config.setUsername(dbUser);
                config.setPassword(dbPass);
                config.setMaximumPoolSize(poolSize);
                config.setConnectionTimeout(3000);
                config.setPoolName("Audino-External-Pool");

                HikariDataSource testDs = new HikariDataSource(config);
                try (Connection conn = testDs.getConnection()) {
                    logger.info("Successfully connected to external PostgreSQL database '{}'.", targetDb);
                    this.dataSource = testDs;
                    this.isEmbedded = false;
                    return;
                }
            } catch (Exception e) {
                logger.warn("External PostgreSQL on localhost:5432 failed handshake: {}. Falling back to embedded PostgreSQL.", e.getMessage());
            }
        }

        // Step 2: Fallback to Embedded PostgreSQL if enabled
        if (fallbackEnabled) {
            startEmbeddedPostgres(poolSize, targetDb);
        } else {
            throw new RuntimeException("PostgreSQL unavailable on localhost:5432 and embedded fallback is disabled.");
        }
    }

    private void startEmbeddedPostgres(int poolSize, String targetDb) {
        String dataDirPath = properties.getProperty("db.embedded.data.dir", "./data/pg-data");
        File dataDir = new File(dataDirPath);
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            logger.info("Created embedded PostgreSQL data directory: {} (success: {})", dataDirPath, created);
        }

        logger.info("Starting Zero-Setup Embedded PostgreSQL engine at {}...", dataDir.getAbsolutePath());
        try {
            EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder();
            builder.setDataDirectory(dataDir);
            builder.setCleanDataDirectory(false);

            // Attempt to bind to 5432; if taken by another process, let Zonky assign an ephemeral port
            if (!isPortAvailable("localhost", 5432)) {
                builder.setPort(5432);
            }

            this.embeddedPostgres = builder.start();
            this.isEmbedded = true;

            int actualPort = embeddedPostgres.getPort();
            logger.info("Embedded PostgreSQL running on port {}. Initializing HikariCP connection pool...", actualPort);

            // Ensure target database exists in embedded postgres
            try (Connection adminConn = embeddedPostgres.getPostgresDatabase().getConnection();
                 java.sql.Statement stmt = adminConn.createStatement()) {
                try (java.sql.ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + targetDb + "'")) {
                    if (!rs.next()) {
                        stmt.execute("CREATE DATABASE " + targetDb);
                        logger.info("Created database '{}' in embedded PostgreSQL.", targetDb);
                    }
                }
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:postgresql://localhost:" + actualPort + "/" + targetDb);
            config.setUsername("postgres");
            config.setPassword("postgres");
            config.setMaximumPoolSize(poolSize);
            config.setConnectionTimeout(5000);
            config.setPoolName("Audino-Embedded-Pool");

            this.dataSource = new HikariDataSource(config);
            logger.info("HikariCP pool established against Embedded PostgreSQL successfully.");
        } catch (Exception e) {
            logger.error("Failed to start Embedded PostgreSQL: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize Embedded PostgreSQL", e);
        }
    }

    private boolean isPortAvailable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Initiating Audino clean shutdown...");
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logger.info("HikariCP connection pool closed.");
            }
            if (embeddedPostgres != null) {
                try {
                    embeddedPostgres.close();
                    logger.info("Embedded PostgreSQL engine stopped cleanly.");
                } catch (IOException e) {
                    logger.error("Error shutting down embedded PostgreSQL: {}", e.getMessage());
                }
            }
        }));
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource has not been initialized");
        }
        return dataSource.getConnection();
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public boolean isEmbedded() {
        return isEmbedded;
    }
}
