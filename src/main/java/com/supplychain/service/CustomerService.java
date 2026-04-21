package com.supplychain.service;

import com.supplychain.model.Customer;
import com.supplychain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Optional<Customer> findById(String id) {
        return customerRepository.findById(id);
    }

    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    public void delete(String id) {
        customerRepository.deleteById(id);
    }
}