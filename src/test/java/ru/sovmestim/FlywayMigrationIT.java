package ru.sovmestim;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.sovmestim.support.PostgresIntegrationTest;

class FlywayMigrationIT extends PostgresIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void appliesBaseAndApplicationMigrations() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            assertThat(count(statement, "select count(*) from flyway_schema_history")).isGreaterThanOrEqualTo(2);
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
                assertThat(count(statement, "select count(*) from " + table)).isGreaterThanOrEqualTo(0);
            }
        }
    }

    @Test
    void demoSampleDataIsSeeded() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            assertThat(count(statement, "select count(*) from active_substance")).isGreaterThan(0);
            assertThat(count(statement, "select count(*) from danger_level")).isEqualTo(4);
            assertThat(count(statement, "select count(*) from substance_in_medicine")).isGreaterThan(0);
        }
    }

    private static int count(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
