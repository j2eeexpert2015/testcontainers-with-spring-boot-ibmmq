package com.example.integration;

import com.example.IBMMQDemoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for OrderController using a real IBM MQ container.
 * Verifies that a valid order is successfully posted and processed.
 */
@Testcontainers
@SpringBootTest(classes = IBMMQDemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderControllerWithTestcontainersIT {

    // Start IBM MQ container using Testcontainers
    static final GenericContainer<?> mqContainer = new GenericContainer<>(
            DockerImageName.parse("ibmcom/mq:9.2.4.0-r1")
                    .asCompatibleSubstituteFor("ibmcom/mq"))
            .withExposedPorts(1414)
            .withEnv("LICENSE", "accept")                  // Accept license automatically
            .withEnv("MQ_QMGR_NAME", "QM1")                // Queue manager name
            .withEnv("MQ_APP_PASSWORD", "passw0rd")        // Password for 'app' user
            .waitingFor(Wait.forLogMessage(
                    ".*AMQ5806I: Queued Publish/Subscribe Daemon started.*", 1)) // Wait until MQ is ready
            .withStartupTimeout(Duration.ofMinutes(3));    // Allow time for container startup

    static {
        mqContainer.start(); // Start the container before the test context loads
    }

    // Dynamically register MQ properties for Spring Boot to pick up
    @DynamicPropertySource
    static void mqProps(DynamicPropertyRegistry registry) {
        registry.add("ibm.mq.connName", () ->
                mqContainer.getHost() + "(" + mqContainer.getMappedPort(1414) + ")");
        registry.add("ibm.mq.queueManager", () -> "QM1");
        registry.add("ibm.mq.channel", () -> "DEV.APP.SVRCONN");
        registry.add("ibm.mq.user", () -> "app");
        registry.add("ibm.mq.password", () -> "passw0rd");
        registry.add("ibm.mq.queue", () -> "DEV.QUEUE.1");
    }


    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    /**
     * Sends a valid order to the controller endpoint and verifies the response.
     */
    @Test
    void testSendOrderSuccess() {
        String url = "http://localhost:" + port + "/api/orders";
        String requestJson = """
            {
                "id": "ORD123",
                "product": "Mechanical Keyboard",
                "quantity": 1
            }
        """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestJson, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("ORD123");
    }
}
