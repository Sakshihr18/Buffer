// ============================================================
// 0/1 KNAPSACK — Vehicle Capacity Optimization
// Each order has weight + volume; vehicle has weight/volume limits
// Goal: maximize value (revenue) without exceeding capacity
// ============================================================

function knapsack01(orders, weightCapacity, volumeCapacity) {
  const n = orders.length;
  // 3D DP: [item][weight][volume]
  // Too memory-heavy for large inputs — use 2D rolling for real use
  // Here we use a 2D table with (weight, volume) combined constraint

  const W = weightCapacity;
  const V = volumeCapacity;

  // dp[w][v] = max value achievable with weight ≤ w and volume ≤ v
  const dp = Array.from({ length: W + 1 }, () => new Array(V + 1).fill(0));

  for (let i = 0; i < n; i++) {
    const { weight, volume, value } = orders[i];
    // Traverse backwards to ensure 0/1 (each item used once)
    for (let w = W; w >= weight; w--) {
      for (let v = V; v >= volume; v--) {
        dp[w][v] = Math.max(dp[w][v], dp[w - weight][v - volume] + value);
      }
    }
  }

  // Backtrack to find which orders were selected
  const selected = [];
  let w = W, v = V;
  for (let i = n - 1; i >= 0; i--) {
    const { weight, volume, value } = orders[i];
    if (w >= weight && v >= volume &&
        dp[w][v] === dp[w - weight][v - volume] + value) {
      selected.push(orders[i]);
      w -= weight;
      v -= volume;
    }
  }

  return {
    maxValue: dp[W][V],
    selectedOrders: selected,
    usedWeight: W - w,
    usedVolume: V - v,
    remainingWeight: w,
    remainingVolume: v,
    efficiency: Math.round((selected.length / n) * 100)
  };
}

// ============================================================
// MULTI-VEHICLE ASSIGNMENT
// Greedy: assign highest-value orders to vehicles using knapsack
// ============================================================
function assignOrdersToVehicles(orders, vehicles) {
  let remaining = [...orders];
  const assignments = [];

  for (const vehicle of vehicles) {
    if (remaining.length === 0) break;
    const result = knapsack01(remaining, vehicle.weightCapacity, vehicle.volumeCapacity);
    const assignedIds = new Set(result.selectedOrders.map(o => o.id));
    assignments.push({
      vehicleId: vehicle.id,
      vehicleName: vehicle.name,
      orders: result.selectedOrders,
      maxValue: result.maxValue,
      usedWeight: result.usedWeight,
      usedVolume: result.usedVolume,
      weightCapacity: vehicle.weightCapacity,
      volumeCapacity: vehicle.volumeCapacity,
      efficiency: result.efficiency
    });
    remaining = remaining.filter(o => !assignedIds.has(o.id));
  }

  return {
    assignments,
    unassignedOrders: remaining
  };
}

// ============================================================
// FRACTIONAL KNAPSACK (Greedy) — for continuous goods / fuel
// ============================================================
function fractionalKnapsack(items, capacity) {
  const sorted = [...items].sort((a, b) => (b.value / b.weight) - (a.value / a.weight));
  let totalValue = 0;
  let remaining = capacity;
  const taken = [];

  for (const item of sorted) {
    if (remaining <= 0) break;
    if (item.weight <= remaining) {
      taken.push({ ...item, fraction: 1 });
      totalValue += item.value;
      remaining -= item.weight;
    } else {
      const frac = remaining / item.weight;
      taken.push({ ...item, fraction: frac });
      totalValue += item.value * frac;
      remaining = 0;
    }
  }

  return { totalValue, taken, remaining };
}
