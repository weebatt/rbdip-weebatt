package com.rbdip.bookstore.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ZeroDowntimeMigrationLoadTest {

    private static final int CLIENT_COUNT = 4;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookstore_load_test")
            .withUsername("bookstore")
            .withPassword("bookstore");

    @Test
    void legacyApiRemainsAvailableDuringExpandMigrations() throws Exception {
        PGSimpleDataSource dataSource = dataSource();
        migrateTo(dataSource, "1");

        List<Throwable> errors = new CopyOnWriteArrayList<>();
        AtomicBoolean keepSending = new AtomicBoolean(true);
        AtomicInteger successfulRequests = new AtomicInteger();
        CountDownLatch firstRequestsCompleted = new CountDownLatch(CLIENT_COUNT);
        ExecutorService apiExecutor = Executors.newFixedThreadPool(CLIENT_COUNT);
        ExecutorService clients = Executors.newFixedThreadPool(CLIENT_COUNT);
        HttpServer server = startLegacyApi(dataSource, apiExecutor);

        try {
            URI endpoint = URI.create("http://localhost:" + server.getAddress().getPort() + "/orders");
            for (int i = 0; i < CLIENT_COUNT; i++) {
                clients.submit(() -> sendRequests(
                        endpoint, keepSending, successfulRequests, firstRequestsCompleted, errors));
            }

            assertThat(firstRequestsCompleted.await(5, TimeUnit.SECONDS)).isTrue();
            migrateTo(dataSource, "3");
        } finally {
            keepSending.set(false);
            clients.shutdown();
            clients.awaitTermination(5, TimeUnit.SECONDS);
            server.stop(0);
            apiExecutor.shutdownNow();
        }

        assertThat(successfulRequests).hasPositiveValue();
        assertThat(errors)
                .as("legacy API must not return 5xx or time out during expand/backfill migrations")
                .isEmpty();
    }

    private PGSimpleDataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        return dataSource;
    }

    private void migrateTo(PGSimpleDataSource dataSource, String target) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(target)
                .load()
                .migrate();
    }

    private HttpServer startLegacyApi(PGSimpleDataSource dataSource, ExecutorService executor)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/orders", exchange -> handleLegacyOrderRequest(exchange, dataSource));
        server.setExecutor(executor);
        server.start();
        return server;
    }

    private void handleLegacyOrderRequest(HttpExchange exchange, PGSimpleDataSource dataSource)
            throws IOException {
        int status = 200;
        byte[] response = "[]".getBytes(StandardCharsets.UTF_8);
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT customer_full_name FROM orders LIMIT 1");
        } catch (SQLException exception) {
            status = 503;
            response = exception.getMessage().getBytes(StandardCharsets.UTF_8);
        }
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private void sendRequests(
            URI endpoint,
            AtomicBoolean keepSending,
            AtomicInteger successfulRequests,
            CountDownLatch firstRequestsCompleted,
            List<Throwable> errors) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        boolean firstRequest = true;
        while (keepSending.get()) {
            try {
                HttpRequest request = HttpRequest.newBuilder(endpoint)
                        .timeout(Duration.ofSeconds(2))
                        .GET()
                        .build();
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 500) {
                    errors.add(new AssertionError("HTTP status " + response.statusCode()));
                } else {
                    successfulRequests.incrementAndGet();
                }
            } catch (IOException exception) {
                errors.add(exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                if (firstRequest) {
                    firstRequestsCompleted.countDown();
                    firstRequest = false;
                }
            }
        }
    }
}
