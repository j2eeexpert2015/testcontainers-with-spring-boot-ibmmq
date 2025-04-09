package com.example.controller;

import com.example.dto.Order;
import com.example.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<String> sendOrder(@RequestBody Order order) {
        try {
            logger.info("Received request to send order via API: {}", order);
            orderService.send(order); // Use the existing service to send [cite: uploaded:testcontainers-with-spring-boot-ibmmq/src/main/java/com/example/service/OrderService.java]
            logger.info("Order successfully sent to MQ queue.");
            return ResponseEntity.ok("Order sent successfully: " + order.getId());
        } catch (Exception e) {
            logger.error("Error sending order via API: {}", order, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send order: " + e.getMessage());
        }
    }
}