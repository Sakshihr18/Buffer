// ============================================================
// ROUTING MODULE
// Builds delivery network, runs Dijkstra/A*, computes costs
// ============================================================

const RoutingModule = (() => {
  const STORE_KEY = 'scs_traffic_delays';

  // ── Network Definition ──────────────────────────────────
  // Nodes: Warehouses (W), Hubs (H), Customer zones (N)
  const NETWORK_NODES = [
    { id: 'W1', name: 'Main Warehouse', x: 250, y: 200, type: 'warehouse' },
    { id: 'W2', name: 'Sub Warehouse',  x: 100, y: 100, type: 'warehouse' },
    { id: 'H1', name: 'North Hub',      x: 200, y: 80,  type: 'hub' },
    { id: 'H2', name: 'East Hub',       x: 420, y: 200, type: 'hub' },
    { id: 'H3', name: 'South Hub',      x: 250, y: 360, type: 'hub' },
    { id: 'N1', name: 'Zone Alpha',     x: 120, y: 180, type: 'zone' },
    { id: 'N2', name: 'Zone Beta',      x: 340, y: 120, type: 'zone' },
    { id: 'N3', name: 'Zone Gamma',     x: 200, y: 150, type: 'zone' },
    { id: 'N4', name: 'Zone Delta',     x: 400, y: 180, type: 'zone' },
    { id: 'N5', name: 'Zone Epsilon',   x: 350, y: 280, type: 'zone' },
    { id: 'N6', name: 'Zone Zeta',      x: 300, y: 380, type: 'zone' },
    { id: 'N7', name: 'Zone Eta',       x: 150, y: 320, type: 'zone' },
  ];

  const NETWORK_EDGES = [
    // Warehouse connections
    ['W1', 'H1', 30], ['W1', 'H2', 25], ['W1', 'H3', 40],
    ['W2', 'H1', 20], ['W2', 'N1', 35],
    // Hub connections
    ['H1', 'N1', 20], ['H1', 'N2', 25], ['H1', 'N3', 15],
    ['H2', 'N2', 20], ['H2', 'N4', 15], ['H2', 'N5', 30],
    ['H3', 'N5', 20], ['H3', 'N6', 15], ['H3', 'N7', 25],
    // Cross connections
    ['N3', 'N1', 25], ['N3', 'N4', 40],
    ['N5', 'N6', 20], ['N7', 'N1', 30],
    ['W1', 'W2', 50],
  ];

  let _graph = null;
  let _trafficDelays = {};

  function buildGraph() {
    _graph = new Graph();
    NETWORK_NODES.forEach(n => _graph.addNode(n.id, n));
    NETWORK_EDGES.forEach(([from, to, w]) => _graph.addEdge(from, to, w));

    // Apply stored traffic delays
    const delays = JSON.parse(localStorage.getItem(STORE_KEY) || '{}');
    Object.entries(delays).forEach(([key, mult]) => {
      const [from, to] = key.split('-');
      _graph.applyTrafficDelay(from, to, mult);
    });
    _trafficDelays = delays;
    return _graph;
  }

  function getGraph() {
    if (!_graph) buildGraph();
    return _graph;
  }

  // ── Route Calculation ────────────────────────────────────

  function optimizeRoute(sourceNode, destNode, algorithm = 'dijkstra') {
    const graph = getGraph();

    let path, cost;
    if (algorithm === 'astar') {
      const result = aStar(graph, sourceNode, destNode);
      path = result.path;
      cost = result.cost;
    } else {
      const { dist, prev } = dijkstra(graph, sourceNode);
      path = reconstructPath(prev, sourceNode, destNode);
      cost = dist.get(destNode);
    }

    const pathDetails = path.map(id => graph.getNode(id));
    return {
      path, pathDetails, cost,
      algorithm,
      hops: path.length - 1,
      isValid: path.length > 0 && cost !== Infinity
    };
  }

  function getAllRoutesFrom(sourceNode) {
    const graph = getGraph();
    const { dist, prev } = dijkstra(graph, sourceNode);
    const routes = {};
    graph.nodes.forEach((node, id) => {
      if (id === sourceNode) return;
      routes[id] = {
        dest: node,
        cost: dist.get(id),
        path: reconstructPath(prev, sourceNode, id)
      };
    });
    return routes;
  }

  // ── Cost Function ────────────────────────────────────────
  // cost = distance + delay_penalty + congestion_factor

  const DELAY_PENALTY_PER_HOUR = 5;
  const BASE_COST_PER_KM = 2;

  function calculateCost(route, delayHours = 0) {
    const baseCost = route.cost * BASE_COST_PER_KM;
    const delayPenalty = delayHours * DELAY_PENALTY_PER_HOUR;
    const total = baseCost + delayPenalty;
    return {
      baseCost: Math.round(baseCost),
      delayPenalty: Math.round(delayPenalty),
      total: Math.round(total),
      perKm: BASE_COST_PER_KM
    };
  }

  function compareCosts(routeBefore, routeAfter) {
    const before = calculateCost(routeBefore);
    const after = calculateCost(routeAfter);
    return {
      before, after,
      savings: before.total - after.total,
      improvement: Math.round(((before.total - after.total) / before.total) * 100)
    };
  }

  // ── Traffic Simulation ───────────────────────────────────

  function simulateTrafficDelay(fromNode, toNode, multiplier = 2.5) {
    const key = `${fromNode}-${toNode}`;
    _trafficDelays[key] = multiplier;
    localStorage.setItem(STORE_KEY, JSON.stringify(_trafficDelays));
    _graph = null; // Force rebuild
    buildGraph();
    return { affectedEdge: key, multiplier, message: `Traffic delay applied on ${fromNode}→${toNode} (×${multiplier})` };
  }

  function clearAllDelays() {
    _trafficDelays = {};
    localStorage.removeItem(STORE_KEY);
    _graph = null;
    buildGraph();
  }

  function getActiveDelays() {
    return Object.entries(_trafficDelays).map(([edge, mult]) => ({ edge, multiplier: mult }));
  }

  // ── Return Route ─────────────────────────────────────────

  function calculateReturnRoute(customerNode, warehouseNode = 'W1') {
    return optimizeRoute(customerNode, warehouseNode);
  }

  return {
    buildGraph, getGraph,
    optimizeRoute, getAllRoutesFrom,
    calculateCost, compareCosts,
    simulateTrafficDelay, clearAllDelays, getActiveDelays,
    calculateReturnRoute,
    NODES: NETWORK_NODES
  };
})();
