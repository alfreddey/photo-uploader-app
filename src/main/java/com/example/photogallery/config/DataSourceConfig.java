package com.example.photogallery.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Builds the DataSource explicitly from {@link DbCredentials} rather than from
 * spring.datasource.* properties, because in production the connection is
 * assembled from a Secrets Manager JSON blob and separate env vars. Defining
 * this bean makes Spring Boot's DataSourceAutoConfiguration back off.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    public DataSource dataSource() {
        DbCredentials creds = DbCredentials.fromSystemEnv();
        log.info("Connecting to database {}:{}/{} as {}",
                creds.host(), creds.port(), creds.database(), creds.username());

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(creds.jdbcUrl());
        config.setUsername(creds.username());
        config.setPassword(creds.password());
        config.setPoolName("photo-gallery-pool");
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(10_000);
        return new HikariDataSource(config);
    }
}
