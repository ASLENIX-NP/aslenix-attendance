package com.aslenix.attendance;

import com.aslenix.attendance.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    void testPostgresUrlParsing() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "databaseUrl", "postgres://testuser:testpass@dpg-c1234:5432/aslenix_db");

        HikariConfig ds = config.buildHikariConfig();
        assertEquals("jdbc:postgresql://dpg-c1234:5432/aslenix_db", ds.getJdbcUrl());
        assertEquals("testuser", ds.getUsername());
        assertEquals("testpass", ds.getPassword());
        assertEquals("org.postgresql.Driver", ds.getDriverClassName());
        assertEquals(10, ds.getMaximumPoolSize());
    }

    @Test
    void testPostgresUrlWithSpecialCharPassword() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "databaseUrl", "postgresql://admin:p%40ss%3A123@db.render.com:5432/attendance");

        HikariConfig ds = config.buildHikariConfig();
        assertEquals("jdbc:postgresql://db.render.com:5432/attendance", ds.getJdbcUrl());
        assertEquals("admin", ds.getUsername());
        assertEquals("p@ss:123", ds.getPassword());
        assertEquals("org.postgresql.Driver", ds.getDriverClassName());
    }

    @Test
    void testMysqlUrlParsing() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "databaseUrl", "mysql://myuser:mypass@mysql.render.com:3306/attendance_db");

        HikariConfig ds = config.buildHikariConfig();
        assertTrue(ds.getJdbcUrl().startsWith("jdbc:mysql://mysql.render.com:3306/attendance_db"));
        assertEquals("myuser", ds.getUsername());
        assertEquals("mypass", ds.getPassword());
        assertEquals("com.mysql.cj.jdbc.Driver", ds.getDriverClassName());
    }

    @Test
    void testDiscreteHostNameConfiguration() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "dbHost", "db.example.com");
        ReflectionTestUtils.setField(config, "dbPort", "3306");
        ReflectionTestUtils.setField(config, "dbName", "aslenix_prod");
        ReflectionTestUtils.setField(config, "dbUsername", "cloud_admin");
        ReflectionTestUtils.setField(config, "dbPassword", "cloud_secret");

        HikariConfig ds = config.buildHikariConfig();
        assertTrue(ds.getJdbcUrl().startsWith("jdbc:mysql://db.example.com:3306/aslenix_prod"));
        assertEquals("cloud_admin", ds.getUsername());
        assertEquals("cloud_secret", ds.getPassword());
        assertEquals("com.mysql.cj.jdbc.Driver", ds.getDriverClassName());
    }
}
