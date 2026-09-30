package com.aslenix.attendance.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Cloud-ready DataSource configuration supporting:
 * 1. Render Managed PostgreSQL (DATABASE_URL starting with postgres:// or postgresql://)
 * 2. Cloud MySQL (DATABASE_URL starting with mysql:// or jdbc:mysql://)
 * 3. Discrete environment variables (DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD)
 * 4. Local development fallbacks with connection pool tuning for 512MB RAM cloud environments.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DATABASE_URL:${DB_URL:}}")
    private String databaseUrl;

    @Value("${spring.datasource.url:}")
    private String springDatasourceUrl;

    @Value("${spring.datasource.username:}")
    private String springDatasourceUsername;

    @Value("${spring.datasource.password:}")
    private String springDatasourcePassword;

    @Value("${DB_HOST:}")
    private String dbHost;

    @Value("${DB_PORT:3306}")
    private String dbPort = "3306";

    @Value("${DB_NAME:}")
    private String dbName;

    @Value("${DB_USERNAME:}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize = 10;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle = 2;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = buildHikariConfig();
        return new HikariDataSource(config);
    }

    public HikariConfig buildHikariConfig() {
        HikariConfig config = new HikariConfig();

        String rawUrl = StringUtils.hasText(databaseUrl) ? databaseUrl.trim() : "";

        if (!StringUtils.hasText(rawUrl) && StringUtils.hasText(springDatasourceUrl)) {
            rawUrl = springDatasourceUrl.trim();
        }

        if (StringUtils.hasText(rawUrl)) {
            configureFromUrl(config, rawUrl);
        } else if (StringUtils.hasText(dbHost) && StringUtils.hasText(dbName)) {
            String jdbcUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
                    + "?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kathmandu";
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(StringUtils.hasText(dbUsername) ? dbUsername : springDatasourceUsername);
            config.setPassword(StringUtils.hasText(dbPassword) ? dbPassword : springDatasourcePassword);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            log.info("Configured MySQL DataSource from discrete DB_HOST/DB_NAME: {}", dbHost);
        } else {
            config.setJdbcUrl(springDatasourceUrl);
            config.setUsername(springDatasourceUsername);
            config.setPassword(springDatasourcePassword);
            log.info("Configured default DataSource from spring.datasource properties.");
        }

        // Optimized pool settings for cloud containers (e.g. Render 512MB RAM tier)
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(20000);
        // Retry connection for up to 60s while cloud DB is booting
        config.setInitializationFailTimeout(60000);
        config.setPoolName("AslenixHikariPool");

        return config;
    }

    private void configureFromUrl(HikariConfig config, String rawUrl) {
        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                URI uri = new URI(rawUrl);
                String userInfo = uri.getUserInfo();
                String username = null;
                String password = null;
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                    if (parts.length > 1) {
                        password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                    }
                }

                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                if (path != null && path.startsWith("/")) {
                    path = path.substring(1);
                }

                String query = uri.getQuery();
                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(uri.getHost())
                        .append(":")
                        .append(port)
                        .append("/")
                        .append(path);

                if (StringUtils.hasText(query)) {
                    jdbcUrl.append("?").append(query);
                }

                config.setJdbcUrl(jdbcUrl.toString());
                if (StringUtils.hasText(username)) {
                    config.setUsername(username);
                } else if (StringUtils.hasText(dbUsername)) {
                    config.setUsername(dbUsername);
                }
                if (password != null) {
                    config.setPassword(password);
                } else if (StringUtils.hasText(dbPassword)) {
                    config.setPassword(dbPassword);
                }
                config.setDriverClassName("org.postgresql.Driver");
                log.info("Configured PostgreSQL DataSource for host: {} (database: {})", uri.getHost(), path);
            } catch (Exception e) {
                log.warn("Failed to parse PostgreSQL URI, falling back to direct jdbc: prefix: {}", e.getMessage());
                String directUrl = rawUrl.replaceFirst("^postgres(ql)?://", "jdbc:postgresql://");
                config.setJdbcUrl(directUrl);
                config.setDriverClassName("org.postgresql.Driver");
            }
        } else if (rawUrl.startsWith("mysql://")) {
            try {
                URI uri = new URI(rawUrl);
                String userInfo = uri.getUserInfo();
                String username = null;
                String password = null;
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                    if (parts.length > 1) {
                        password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                    }
                }

                int port = uri.getPort() == -1 ? 3306 : uri.getPort();
                String path = uri.getPath();
                if (path != null && path.startsWith("/")) {
                    path = path.substring(1);
                }

                String query = uri.getQuery();
                StringBuilder jdbcUrl = new StringBuilder("jdbc:mysql://")
                        .append(uri.getHost())
                        .append(":")
                        .append(port)
                        .append("/")
                        .append(path);

                if (StringUtils.hasText(query)) {
                    jdbcUrl.append("?").append(query);
                } else {
                    jdbcUrl.append("?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kathmandu");
                }

                config.setJdbcUrl(jdbcUrl.toString());
                if (StringUtils.hasText(username)) {
                    config.setUsername(username);
                } else if (StringUtils.hasText(dbUsername)) {
                    config.setUsername(dbUsername);
                }
                if (password != null) {
                    config.setPassword(password);
                } else if (StringUtils.hasText(dbPassword)) {
                    config.setPassword(dbPassword);
                }
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                log.info("Configured MySQL DataSource for host: {} (database: {})", uri.getHost(), path);
            } catch (Exception e) {
                log.warn("Failed to parse MySQL URI, falling back to direct jdbc: prefix: {}", e.getMessage());
                String directUrl = rawUrl.replaceFirst("^mysql://", "jdbc:mysql://");
                config.setJdbcUrl(directUrl);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }
        } else {
            config.setJdbcUrl(rawUrl);
            if (rawUrl.contains("postgresql")) {
                config.setDriverClassName("org.postgresql.Driver");
            } else if (rawUrl.contains("mysql")) {
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }

            if (StringUtils.hasText(dbUsername)) {
                config.setUsername(dbUsername);
            } else if (StringUtils.hasText(springDatasourceUsername)) {
                config.setUsername(springDatasourceUsername);
            }

            if (StringUtils.hasText(dbPassword)) {
                config.setPassword(dbPassword);
            } else if (StringUtils.hasText(springDatasourcePassword)) {
                config.setPassword(springDatasourcePassword);
            }
            log.info("Configured DataSource directly from JDBC URL.");
        }
    }
}
