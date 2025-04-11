package com.example.listener;

import com.example.dto.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;

@Component
public class OrderListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderListener.class);

    private final BlockingQueue<Order> receivedOrdersQueue;

    public OrderListener(@Qualifier("receivedOrdersQueue") BlockingQueue<Order> receivedOrdersQueue) {
        this.receivedOrdersQueue = receivedOrdersQueue;
    }

    @JmsListener(destination = "${ibm.mq.queue}")
    public void receiveOrder(Order order) {
        logger.info("📥 Received Order: {}", order);
        receivedOrdersQueue.offer(order);  //  Place it in the shared queue
    }
}
