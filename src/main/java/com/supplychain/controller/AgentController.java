package com.supplychain.controller;

import com.supplychain.model.Agent;
import com.supplychain.service.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public ResponseEntity<List<Agent>> getAllAgents() {
        return ResponseEntity.ok(agentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Agent> getAgent(@PathVariable String id) {
        return agentService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Agent>> getAvailableAgents() {
        return ResponseEntity.ok(agentService.findAvailable());
    }

    @PostMapping
    public ResponseEntity<Agent> createAgent(@RequestBody Agent agent) {
        return ResponseEntity.ok(agentService.save(agent));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Agent> updateAgent(@PathVariable String id,
                                                 @RequestBody Agent agent) {
        return agentService.findById(id).map(existing -> {
            existing.setName(agent.getName());
            existing.setPhone(agent.getPhone());
            existing.setXCoord(agent.getXCoord());
            existing.setYCoord(agent.getYCoord());
            existing.setAvailable(agent.getAvailable());
            existing.setRating(agent.getRating());
            return ResponseEntity.ok(agentService.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<Agent> updateAvailability(@PathVariable String id,
                                                         @RequestBody Map<String, Boolean> payload) {
        boolean available = payload.get("available");
        return ResponseEntity.ok(agentService.updateAvailability(id, available));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgent(@PathVariable String id) {
        agentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}