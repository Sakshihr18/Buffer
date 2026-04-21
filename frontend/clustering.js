// ============================================================
// K-MEANS CLUSTERING — Order Batching
// Groups nearby delivery locations to minimize total travel
// ============================================================

function kMeansClustering(points, k, maxIter = 100) {
  if (points.length === 0 || k <= 0) return [];
  k = Math.min(k, points.length);

  // Initialize centroids using K-Means++ (better than random)
  const centroids = initKMeansPlusPlus(points, k);
  let assignments = new Array(points.length).fill(0);
  let changed = true;
  let iter = 0;

  while (changed && iter < maxIter) {
    changed = false;
    iter++;

    // Assignment step: assign each point to nearest centroid
    for (let i = 0; i < points.length; i++) {
      let minDist = Infinity;
      let cluster = 0;
      for (let c = 0; c < centroids.length; c++) {
        const d = euclidean(points[i], centroids[c]);
        if (d < minDist) { minDist = d; cluster = c; }
      }
      if (assignments[i] !== cluster) { assignments[i] = cluster; changed = true; }
    }

    // Update step: recompute centroids
    for (let c = 0; c < centroids.length; c++) {
      const clusterPoints = points.filter((_, i) => assignments[i] === c);
      if (clusterPoints.length === 0) continue;
      centroids[c] = {
        x: clusterPoints.reduce((s, p) => s + p.x, 0) / clusterPoints.length,
        y: clusterPoints.reduce((s, p) => s + p.y, 0) / clusterPoints.length
      };
    }
  }

  // Build cluster result
  const clusters = Array.from({ length: k }, (_, c) => ({
    id: c,
    centroid: centroids[c],
    points: points.filter((_, i) => assignments[i] === c),
    orders: points.filter((_, i) => assignments[i] === c).map(p => p.orderId).filter(Boolean)
  }));

  return clusters.filter(c => c.points.length > 0);
}

// K-Means++ initialization for better convergence
function initKMeansPlusPlus(points, k) {
  const centroids = [];
  // First centroid: random
  centroids.push({ ...points[Math.floor(Math.random() * points.length)] });

  for (let i = 1; i < k; i++) {
    // Compute D² distances to nearest centroid
    const distances = points.map(p => {
      const minD = Math.min(...centroids.map(c => euclidean(p, c)));
      return minD * minD;
    });
    const total = distances.reduce((a, b) => a + b, 0);
    // Weighted random selection
    let rand = Math.random() * total;
    for (let j = 0; j < points.length; j++) {
      rand -= distances[j];
      if (rand <= 0) { centroids.push({ ...points[j] }); break; }
    }
    if (centroids.length <= i) centroids.push({ ...points[Math.floor(Math.random() * points.length)] });
  }
  return centroids;
}

// ============================================================
// GREEDY NEAREST-NEIGHBOR CLUSTERING
// Simpler: group points within a radius threshold
// ============================================================
function greedyClustering(orders, radiusThreshold) {
  const clusters = [];
  const assigned = new Set();

  for (let i = 0; i < orders.length; i++) {
    if (assigned.has(i)) continue;
    const cluster = [orders[i]];
    assigned.add(i);

    for (let j = i + 1; j < orders.length; j++) {
      if (assigned.has(j)) continue;
      if (euclidean(orders[i], orders[j]) <= radiusThreshold) {
        cluster.push(orders[j]);
        assigned.add(j);
      }
    }
    clusters.push({
      id: clusters.length,
      orders: cluster,
      centroid: {
        x: cluster.reduce((s, o) => s + o.x, 0) / cluster.length,
        y: cluster.reduce((s, o) => s + o.y, 0) / cluster.length
      }
    });
  }
  return clusters;
}

// ============================================================
// GREEDY NEAREST NEIGHBOR — Last Mile Delivery Agent Assignment
// ============================================================
function nearestNeighborAssign(failedLocation, agents) {
  let nearest = null;
  let minDist = Infinity;

  for (const agent of agents) {
    if (!agent.available) continue;
    const d = euclidean(failedLocation, agent);
    if (d < minDist) { minDist = d; nearest = agent; }
  }
  return { agent: nearest, distance: minDist };
}

// ============================================================
// GREEDY WORKFORCE MATCHING
// Assigns agents to orders minimizing total distance
// ============================================================
function greedyWorkforceMatch(orders, agents) {
  const available = agents.filter(a => a.available);
  const assignments = [];
  const assignedAgents = new Set();

  for (const order of orders) {
    let bestAgent = null, minDist = Infinity;
    for (const agent of available) {
      if (assignedAgents.has(agent.id)) continue;
      const d = euclidean(order, agent);
      if (d < minDist) { minDist = d; bestAgent = agent; }
    }
    if (bestAgent) {
      assignments.push({ order, agent: bestAgent, distance: minDist });
      assignedAgents.add(bestAgent.id);
    }
  }
  return assignments;
}

function euclidean(a, b) {
  return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
}
