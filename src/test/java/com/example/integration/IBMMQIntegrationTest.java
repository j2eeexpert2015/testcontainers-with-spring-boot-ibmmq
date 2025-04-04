package com.example.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.example.dto.Order;
import com.example.service.OrderService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IBMMQIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(IBMMQIntegrationTest.class);

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

    // Service that sends messages
    @Autowired
    private OrderService orderService;

    // Injected BlockingQueue used to capture received message from the listener
    @Autowired
    @Qualifier("receivedOrdersQueue")
    private BlockingQueue<Order> receivedOrdersQueue;

    @Test
    void testOrderMessaging() throws Exception {
        // Step 1: Create a test order
        Order testOrder = new Order("A101", "Test Product", 5);
        log.info("Sending test order: {}", testOrder);

        // Step 2: Send the order to IBM MQ via OrderService
        orderService.send(testOrder);

        // Step 3: Poll the BlockingQueue for up to 10 seconds to retrieve the received message
        Order received = receivedOrdersQueue.poll(10, TimeUnit.SECONDS);
        log.info("Received test order: {}", received);

        // Step 4: Validate received message
        assertThat(received).isNotNull();
        assertThat(received.getId()).isEqualTo("A101");
        assertThat(received.getProduct()).isEqualTo("Test Product");
        assertThat(received.getQuantity()).isEqualTo(5);
    }
}
