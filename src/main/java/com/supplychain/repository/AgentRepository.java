package com.supplychain.repository;

import com.supplychain.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AgentRepository extends JpaRepository<Agent, String> {
    List<Agent> findByAvailableTrue();
}