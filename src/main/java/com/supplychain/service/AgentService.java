package com.supplychain.service;

import com.supplychain.model.Agent;
import com.supplychain.repository.AgentRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AgentService {

    private final AgentRepository agentRepository;

    public AgentService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    public List<Agent> findAll() {
        return agentRepository.findAll();
    }

    public Optional<Agent> findById(String id) {
        return agentRepository.findById(id);
    }

    public List<Agent> findAvailable() {
        return agentRepository.findByAvailableTrue();
    }

    public Agent save(Agent agent) {
        return agentRepository.save(agent);
    }

    public Agent updateAvailability(String id, boolean available) {
        Agent agent = agentRepository.findById(id).orElseThrow();
        agent.setAvailable(available);
        return agentRepository.save(agent);
    }

    public void delete(String id) {
        agentRepository.deleteById(id);
    }
}