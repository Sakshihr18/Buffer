// ============================================================
// GRAPH DATA STRUCTURE (Adjacency List)
// Used for: Route optimization, delivery networks
// ============================================================

class Graph {
  constructor() {
    this.nodes = new Map();   // nodeId -> { id, name, x, y, type }
    this.edges = new Map();   // nodeId -> [{ to, weight, distance, road }]
  }

  addNode(id, data) {
    this.nodes.set(id, { id, ...data });
    if (!this.edges.has(id)) this.edges.set(id, []);
  }

  addEdge(from, to, weight, bidirectional = true) {
    if (!this.edges.has(from)) this.edges.set(from, []);
    if (!this.edges.has(to)) this.edges.set(to, []);
    this.edges.get(from).push({ to, weight });
    if (bidirectional) this.edges.get(to).push({ to: from, weight });
  }

  getNeighbors(nodeId) {
    return this.edges.get(nodeId) || [];
  }

  getNode(nodeId) {
    return this.nodes.get(nodeId);
  }

  getAllNodes() {
    return Array.from(this.nodes.values());
  }

  // Apply traffic delay multiplier on edges
  applyTrafficDelay(from, to, multiplier) {
    const edges = this.edges.get(from) || [];
    edges.forEach(e => {
      if (e.to === to) e.weight = Math.round(e.weight * multiplier);
    });
    const backEdges = this.edges.get(to) || [];
    backEdges.forEach(e => {
      if (e.to === from) e.weight = Math.round(e.weight * multiplier);
    });
  }

  // Euclidean distance heuristic for A*
  heuristic(nodeA, nodeB) {
    const a = this.nodes.get(nodeA);
    const b = this.nodes.get(nodeB);
    if (!a || !b) return 0;
    return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
  }

  // Serialize for export/display
  toJSON() {
    return {
      nodes: Array.from(this.nodes.entries()),
      edges: Array.from(this.edges.entries())
    };
  }
}

// ============================================================
// DIJKSTRA'S ALGORITHM
// Returns: { dist, prev } maps for shortest paths from source
// ============================================================
function dijkstra(graph, source) {
  const dist = new Map();
  const prev = new Map();
  const visited = new Set();
  const pq = new MinHeap();

  graph.nodes.forEach((_, id) => {
    dist.set(id, Infinity);
    prev.set(id, null);
  });
  dist.set(source, 0);
  pq.insert(0, source);

  while (!pq.isEmpty()) {
    const { key: d, value: u } = pq.extractMin();
    if (visited.has(u)) continue;
    visited.add(u);

    for (const { to: v, weight } of graph.getNeighbors(u)) {
      if (visited.has(v)) continue;
      const alt = dist.get(u) + weight;
      if (alt < dist.get(v)) {
        dist.set(v, alt);
        prev.set(v, u);
        pq.insert(alt, v);
      }
    }
  }
  return { dist, prev };
}

// ============================================================
// A* ALGORITHM
// Uses Euclidean heuristic for faster convergence
// ============================================================
function aStar(graph, source, target) {
  const gScore = new Map();
  const fScore = new Map();
  const prev = new Map();
  const visited = new Set();
  const pq = new MinHeap();

  graph.nodes.forEach((_, id) => {
    gScore.set(id, Infinity);
    fScore.set(id, Infinity);
    prev.set(id, null);
  });

  gScore.set(source, 0);
  fScore.set(source, graph.heuristic(source, target));
  pq.insert(fScore.get(source), source);

  while (!pq.isEmpty()) {
    const { value: current } = pq.extractMin();
    if (current === target) break;
    if (visited.has(current)) continue;
    visited.add(current);

    for (const { to: neighbor, weight } of graph.getNeighbors(current)) {
      if (visited.has(neighbor)) continue;
      const tentativeG = gScore.get(current) + weight;
      if (tentativeG < gScore.get(neighbor)) {
        gScore.set(neighbor, tentativeG);
        fScore.set(neighbor, tentativeG + graph.heuristic(neighbor, target));
        prev.set(neighbor, current);
        pq.insert(fScore.get(neighbor), neighbor);
      }
    }
  }

  // Reconstruct path
  const path = [];
  let cur = target;
  while (cur !== null) {
    path.unshift(cur);
    cur = prev.get(cur);
  }
  const valid = path[0] === source;
  return { path: valid ? path : [], cost: gScore.get(target) || Infinity };
}

// Reconstruct path from dijkstra prev map
function reconstructPath(prev, source, target) {
  const path = [];
  let cur = target;
  while (cur !== null) {
    path.unshift(cur);
    cur = prev.get(cur);
  }
  return path[0] === source ? path : [];
}

// ============================================================
// MIN-HEAP (Priority Queue) for Dijkstra / A*
// ============================================================
class MinHeap {
  constructor() { this.heap = []; }
  insert(key, value) {
    this.heap.push({ key, value });
    this._bubbleUp(this.heap.length - 1);
  }
  extractMin() {
    if (this.heap.length === 1) return this.heap.pop();
    const min = this.heap[0];
    this.heap[0] = this.heap.pop();
    this._sinkDown(0);
    return min;
  }
  isEmpty() { return this.heap.length === 0; }
  _bubbleUp(i) {
    while (i > 0) {
      const parent = Math.floor((i - 1) / 2);
      if (this.heap[parent].key <= this.heap[i].key) break;
      [this.heap[parent], this.heap[i]] = [this.heap[i], this.heap[parent]];
      i = parent;
    }
  }
  _sinkDown(i) {
    const n = this.heap.length;
    while (true) {
      let smallest = i;
      const l = 2 * i + 1, r = 2 * i + 2;
      if (l < n && this.heap[l].key < this.heap[smallest].key) smallest = l;
      if (r < n && this.heap[r].key < this.heap[smallest].key) smallest = r;
      if (smallest === i) break;
      [this.heap[smallest], this.heap[i]] = [this.heap[i], this.heap[smallest]];
      i = smallest;
    }
  }
}
