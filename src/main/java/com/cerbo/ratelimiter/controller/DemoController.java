package com.cerbo.ratelimiter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/customers")
    public ResponseEntity<?> getCustomers() {

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Customer data retrieved"
                )
        );
    }

    @PostMapping("/customers")
    public ResponseEntity<?> createCustomer(
            @RequestBody Map<String, Object> request) {

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Customer created"
                )
        );
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<?> updateCustomer(
            @PathVariable String id,
            @RequestBody Map<String, Object> request) {

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Customer updated",
                        "id",
                        id
                )
        );
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<?> deleteCustomer(
            @PathVariable String id) {

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Customer deleted",
                        "id",
                        id
                )
        );
    }
}