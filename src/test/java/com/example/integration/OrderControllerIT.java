package com.example.integration;

import com.example.dto.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for OrderController.
 * Sends a valid order using HTTP POST and verifies the response.
 * Requires IBM MQ to be running (e.g., via Docker Compose).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderControllerIT {

    private static final Logger logger = LoggerFactory.getLogger(OrderControllerIT.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testSendValidOrder() {
        // Define target URL for the controller endpoint
        String url = "http://localhost:" + port + "/api/orders";

        // Create a valid order payload
        Order order = new Order();
        order.setId("ORD123");
        order.setProduct("Mechanical Keyboard");
        order.setQuantity(1);

        // Create HTTP request with JSON headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Order> request = new HttpEntity<>(order, headers);

        // Send the HTTP POST request
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        // Log and assert
        logger.info("Response from /api/orders: {}", response);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Order sent successfully");
    }
}
