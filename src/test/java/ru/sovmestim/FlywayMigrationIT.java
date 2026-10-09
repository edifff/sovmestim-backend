package ru.sovmestim;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Integration tests verifying that Flyway migrations apply cleanly to the test database.
 */
class FlywayMigrationIT extends PostgresIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void appliesBaseAndApplicationMigrations() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            Assertions.assertThat(count(statement, "select count(*) from flyway_schema_history")).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void baseSchemaIsPresent() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            for (String table : new String[] {
                "app_user", "allergy", "allergy_user", "active_substance", "medicine",
                "interaction_substances", "danger_level", "course_medicine"
            }) {
                Assertions.assertThat(count(statement, "select count(*) from " + table)).isGreaterThanOrEqualTo(0);
            }
        }
    }

    @Test
    void demoSampleDataIsSeeded() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            Assertions.assertThat(count(statement, "select count(*) from active_substance")).isGreaterThan(0);
            Assertions.assertThat(count(statement, "select count(*) from danger_level")).isEqualTo(4);
            Assertions.assertThat(count(statement, "select count(*) from substance_in_medicine")).isGreaterThan(0);
        }
    }

    private static int count(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
