package com.example.photogallery.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Resolves the PostgreSQL connection from the environment, supporting both
 * runtime shapes this app is deployed in:
 *
 *  - Production (ECS/Fargate): the task definition injects a {@code DB_CREDENTIALS}
 *    secret (the full Secrets Manager JSON with username/password/host/port/dbname),
 *    plus plain {@code DB_HOST} and {@code DB_PORT} env vars.
 *  - Local (docker-compose): plain {@code DB_HOST / DB_PORT / DB_NAME /
 *    DB_USERNAME / DB_PASSWORD} env vars and no secret.
 *
 * Individual env vars always take precedence over the secret, so either shape
 * works on its own.
 */
public record DbCredentials(String host, int port, String database, String username, String password) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String jdbcUrl() {
        return "jdbc:postgresql://%s:%d/%s".formatted(host, port, database);
    }

    public static DbCredentials fromSystemEnv() {
        return resolve(System.getenv());
    }

    public static DbCredentials resolve(Map<String, String> env) {
        JsonNode secret = parseSecret(env.get("DB_CREDENTIALS"));

        String host = firstNonBlank(env.get("DB_HOST"), text(secret, "host"), "localhost");
        int port = parsePort(firstNonBlank(env.get("DB_PORT"), text(secret, "port"), "5432"));
        String database = firstNonBlank(env.get("DB_NAME"), text(secret, "dbname"), "photogallery");
        String username = firstNonBlank(env.get("DB_USERNAME"), text(secret, "username"), "postgres");
        String password = firstNonBlank(env.get("DB_PASSWORD"), text(secret, "password"), "");

        return new DbCredentials(host, port, database, username, password);
    }

    private static JsonNode parseSecret(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("DB_CREDENTIALS is set but is not valid JSON", e);
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static int parsePort(String port) {
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException e) {
            return 5432;
        }
    }
}
