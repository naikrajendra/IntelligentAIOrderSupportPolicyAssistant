package com.example.ordersupport.support;

import java.io.IOException;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
public class OrderSupportController {

    private final OrderSupportService orderSupportService;
    private final PolicyIngestionService policyIngestionService;
    private final OrderSeedService orderSeedService;
    private final String defaultPolicyDirectory;

    public OrderSupportController(
            OrderSupportService orderSupportService,
            PolicyIngestionService policyIngestionService,
            OrderSeedService orderSeedService,
            @Value("${app.policies.pdf-directory}") String defaultPolicyDirectory
    ) {
        this.orderSupportService = orderSupportService;
        this.policyIngestionService = policyIngestionService;
        this.orderSeedService = orderSeedService;
        this.defaultPolicyDirectory = defaultPolicyDirectory;
    }

    @PostMapping("/query")
    public ResponseEntity<OrderSupportResponse> query(@RequestBody OrderSupportRequest request) {
        String answer = orderSupportService.generatePolicyAwareAnswer(request);
        return ResponseEntity.ok(new OrderSupportResponse(answer));
    }

    @PostMapping("/policies/ingest")
    public ResponseEntity<PolicyIngestionResponse> ingestPolicies(
            @RequestParam(required = false) String directory
    ) throws IOException {
        String effectiveDirectory = directory == null || directory.isBlank()
                ? defaultPolicyDirectory
                : directory;

        PolicyIngestionResponse response = policyIngestionService.ingestFromDirectory(Path.of(effectiveDirectory));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders/seed")
    public ResponseEntity<OrderSeedResponse> seedOrders() {
        int ordersSeeded = orderSeedService.seedDefaultOrders();
        return ResponseEntity.ok(new OrderSeedResponse(ordersSeeded));
    }
}
