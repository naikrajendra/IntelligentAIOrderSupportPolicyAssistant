package com.example.ordersupport.support;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class OrderDataInitializer implements CommandLineRunner {

    private final OrderSeedService orderSeedService;

    public OrderDataInitializer(OrderSeedService orderSeedService) {
        this.orderSeedService = orderSeedService;
    }

    @Override
    public void run(String... args) {
        orderSeedService.seedDefaultOrders();
    }
}
