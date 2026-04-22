package com.supplychain.util;

import com.supplychain.model.Node;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class NetworkInitializer {

    private Graph graph;

    public Graph getGraph() {
        return graph;
    }

    @PostConstruct
    public void init() {
        graph = new Graph();

        List<Node> nodes = Arrays.asList(
            new Node("W1", "Main Warehouse", 250, 200, "warehouse"),
            new Node("W2", "Sub Warehouse", 100, 100, "warehouse"),
            new Node("H1", "North Hub", 200, 80, "hub"),
            new Node("H2", "East Hub", 420, 200, "hub"),
            new Node("H3", "South Hub", 250, 360, "hub"),
            new Node("N1", "Zone Alpha", 120, 180, "zone"),
            new Node("N2", "Zone Beta", 340, 120, "zone"),
            new Node("N3", "Zone Gamma", 200, 150, "zone"),
            new Node("N4", "Zone Delta", 400, 180, "zone"),
            new Node("N5", "Zone Epsilon", 350, 280, "zone"),
            new Node("N6", "Zone Zeta", 300, 380, "zone"),
            new Node("N7", "Zone Eta", 150, 320, "zone")
        );

        for (Node node : nodes) {
            graph.addNode(node);
        }

        String[][] edges = {
            {"W1", "H1", "30"}, {"W1", "H2", "25"}, {"W1", "H3", "40"},
            {"W2", "H1", "20"}, {"W2", "N1", "35"},
            {"H1", "N1", "20"}, {"H1", "N2", "25"}, {"H1", "N3", "15"},
            {"H2", "N2", "20"}, {"H2", "N4", "15"}, {"H2", "N5", "30"},
            {"H3", "N5", "20"}, {"H3", "N6", "15"}, {"H3", "N7", "25"},
            {"N3", "N1", "25"}, {"N3", "N4", "40"},
            {"N5", "N6", "20"}, {"N7", "N1", "30"},
            {"W1", "W2", "50"}
        };

        for (String[] edge : edges) {
            graph.addEdge(edge[0], edge[1], Integer.parseInt(edge[2]));
        }
    }
}