package com.supplychain.controller;

import com.supplychain.model.Customer;
import com.supplychain.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable String id) {
        return customerService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        return ResponseEntity.ok(customerService.save(customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable String id,
                                                   @RequestBody Customer customer) {
        return customerService.findById(id).map(existing -> {
            existing.setName(customer.getName());
            existing.setEmail(customer.getEmail());
            existing.setPhone(customer.getPhone());
            existing.setAddress(customer.getAddress());
            existing.setZoneNode(customer.getZoneNode());
            existing.setXCoord(customer.getXCoord());
            existing.setYCoord(customer.getYCoord());
            return ResponseEntity.ok(customerService.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}