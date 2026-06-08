package com.example.photogallery.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DbCredentialsTest {

    @Test
    void parsesSecretJsonButLetsExplicitEnvVarsWin() {
        Map<String, String> env = Map.of(
                "DB_HOST", "db.internal",
                "DB_PORT", "5432",
                "DB_CREDENTIALS",
                "{\"username\":\"dbadmin\",\"password\":\"s3cr3t\",\"dbname\":\"photogallery\","
                        + "\"host\":\"ignored.rds.amazonaws.com\",\"port\":\"9999\"}");

        DbCredentials creds = DbCredentials.resolve(env);

        assertEquals("dbadmin", creds.username());
        assertEquals("s3cr3t", creds.password());
        assertEquals("photogallery", creds.database());
        // DB_HOST / DB_PORT env vars override the secret's host/port.
        assertEquals("db.internal", creds.host());
        assertEquals(5432, creds.port());
        assertEquals("jdbc:postgresql://db.internal:5432/photogallery", creds.jdbcUrl());
    }

    @Test
    void worksWithPlainLocalEnvVarsAndNoSecret() {
        Map<String, String> env = Map.of(
                "DB_HOST", "postgres",
                "DB_PORT", "5432",
                "DB_NAME", "photogallery",
                "DB_USERNAME", "photo",
                "DB_PASSWORD", "photo");

        DbCredentials creds = DbCredentials.resolve(env);

        assertEquals("postgres", creds.host());
        assertEquals("photo", creds.username());
        assertEquals("photo", creds.password());
        assertEquals("jdbc:postgresql://postgres:5432/photogallery", creds.jdbcUrl());
    }

    @Test
    void fallsBackToSensibleDefaults() {
        DbCredentials creds = DbCredentials.resolve(Map.of());

        assertEquals("localhost", creds.host());
        assertEquals(5432, creds.port());
        assertEquals("photogallery", creds.database());
    }
}
