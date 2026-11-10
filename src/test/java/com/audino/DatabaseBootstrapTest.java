package com.audino;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Configuration & Resource Tests")
class DatabaseBootstrapTest {

    @Test
    @DisplayName("application.properties should exist and contain database configurations")
    void testApplicationProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertThat(in).isNotNull();
            props.load(in);
        }

        assertThat(props.getProperty("app.name")).contains("Audino");
        assertThat(props.getProperty("db.url")).isNotEmpty();
        assertThat(props.getProperty("db.embedded.fallback")).isEqualTo("true");
        assertThat(props.getProperty("security.bcrypt.cost")).isEqualTo("12");
    }

    @Test
    @DisplayName("schema.sql should exist on classpath and contain table definitions")
    void testSchemaSqlExists() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("database/schema.sql")) {
            assertThat(in).isNotNull();
            String content = new String(in.readAllBytes());
            assertThat(content).contains("CREATE TABLE IF NOT EXISTS accounts");
            assertThat(content).contains("CREATE TABLE IF NOT EXISTS medications");
            assertThat(content).contains("CREATE TABLE IF NOT EXISTS interaction_rules");
            assertThat(content).contains("CREATE TABLE IF NOT EXISTS prescriptions");
            assertThat(content).contains("CREATE TABLE IF NOT EXISTS audit_logs");
        }
    }

    @Test
    @DisplayName("Inter typography font should exist in resources/fonts/")
    void testInterFontResourceExists() {
        InputStream in = getClass().getClassLoader().getResourceAsStream("fonts/Inter-Regular.ttf");
        assertThat(in).isNotNull();
    }
}
