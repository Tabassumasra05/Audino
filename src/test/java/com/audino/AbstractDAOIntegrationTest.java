package com.audino;

import com.audino.util.ConfigurationManager;
import com.audino.util.DataSeeder;
import com.audino.util.DatabaseInitializer;
import org.junit.jupiter.api.BeforeAll;

import javax.sql.DataSource;

/**
 * Base class for database integration tests using native PostgreSQL JDBC DAOs.
 * Ensures the schema is initialized and base seed data is populated.
 */
public abstract class AbstractDAOIntegrationTest {

    protected static DataSource dataSource;

    @BeforeAll
    static void initDatabase() {
        dataSource = ConfigurationManager.getInstance().getDataSource();
        DatabaseInitializer initializer = new DatabaseInitializer(dataSource);
        initializer.initialize();

        DataSeeder seeder = new DataSeeder(dataSource);
        seeder.seedIfEmpty();
    }
}
