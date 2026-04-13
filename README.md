# ⚡ SmartChain — Smart Supply Chain Optimization System

A fully interactive, DSA-heavy supply chain management system built with **vanilla JavaScript**, featuring 8 core algorithms, a live delivery simulation, and a polished dark-mode dashboard.

---

## 🗂 Project Structure

```
supply-chain/
├── frontend/
│   ├── index.html              ← Main UI (single-page app)
│   ├── app.js                  ← Main controller + UI rendering + canvas drawing
│   ├── graph.js                ← Graph (adjacency list) + Dijkstra + A* + MinHeap
│   ├── priorityQueue.js        ← Priority Queue (min-heap) + DeliveryScheduler
│   ├── knapsack.js             ← 0/1 Knapsack DP + Multi-vehicle assignment
│   ├── clustering.js           ← K-Means++ + Greedy clustering + Nearest-neighbor
│   ├── orders.js               ← Order lifecycle management (localStorage)
│   ├── inventory.js            ← HashMap-based stock tracking + alerts
│   ├── routing.js              ← Network builder + route optimization + cost function
│   └── tracking.js             ← Live vehicle simulation + fleet + agents
├── backend/
│   ├── server.js               ← Express API server + static frontend host
│   ├── package.json            ← Backend dependencies and start script
│   └── package-lock.json       ← Installed Node modules lockfile
└── database/
    ├── schema.sql              ← MySQL table definitions + stored procedures
    └── sample_data.sql         ← Sample products, orders, agents, vehicles
```

---

## 🚀 How to Run

### Option A — Direct Browser (No Server)
```bash
# Just open index.html in any browser
open supply-chain/frontend/index.html
# Or double-click it in your file manager
```
> All data persists in **localStorage** — no backend needed.

### Option B — Local Dev Server (Recommended)
```bash
cd supply-chain/frontend

# Using Python
python3 -m http.server 3000

# Using Node.js (npx)
npx serve .

# Using VS Code → Right-click index.html → "Open with Live Server"
```
Open: http://localhost:3000

### Option C — Backend API + Database (Recommended for DB integration)
```bash
cd supply-chain/backend
npm install
npm start
```
Open: http://localhost:3001

Then visit the frontend from the browser at http://localhost:3001

### Option D — MySQL Database Setup
```bash
# 1. Start MySQL
mysql -u root -p

# 2. Run schema
source supply-chain/database/schema.sql;

# 3. Insert sample data
source supply-chain/database/sample_data.sql;

# 4. Verify
USE supply_chain_db;
CALL GetAllOrders();
CALL GetInventoryAlerts();
```

---

## 🎯 Feature Walkthrough (Simulation)

### 1. Customer places an order
1. Click **🛒 Place Order** in sidebar
2. Select a product, destination zone, priority (HIGH/MEDIUM/LOW)
3. Click **Preview Route** → sees Dijkstra-optimized path + cost
4. Click **Place Order** → order auto-scheduled via Priority Queue

### 2. Admin optimizes routes
1. Go to **🗺 Route Optimizer**
2. Select source (W1) + destination (N5) + algorithm (Dijkstra or A*)
3. Click **⚡ Optimize Route** → see path visualization on network canvas
4. Click **🚦 Simulate Traffic** → delay multiplier applied to a random edge
5. Route **auto-recalculates** dynamically → cost comparison shown

### 3. Vehicle assignment (Knapsack)
1. On Route Optimizer page, click **🎒 Assign Vehicle**
2. 0/1 Knapsack DP assigns orders to vehicles maximizing value within weight/volume limits
3. Unassigned orders shown with reason

### 4. Live delivery tracking
1. Go to **📦 My Orders**, click **View** on any scheduled order
2. Click **🚚 Start Tracking** in the modal
3. Switch to **📡 Live Tracking** — see vehicle moving along route on canvas
4. Event log shows real-time position updates

### 5. Order batching
1. Route Optimizer → **📦 Batch Orders**
2. K-Means++ clustering groups nearby delivery zones
3. Each cluster shows centroid coordinates + assigned orders

### 6. Workforce management
1. Go to **👷 Workforce**
2. Greedy nearest-neighbor algorithm matches agents to orders by distance
3. Click **Confirm** to commit assignment

### 7. Failed delivery + reassignment
1. On **📦 Orders**, any FAILED order shows **Reassign** button
2. Nearest available agent assigned via greedy algorithm

### 8. Returns
1. DELIVERED orders show **Return** button
2. Reverse Dijkstra computes customer → warehouse path
3. Stock auto-restocked on return confirmation

---

## 🧠 DSA Implementations

| Algorithm | File | Use Case |
|-----------|------|----------|
| Graph (Adjacency List) | `graph.js` | Delivery network representation |
| Dijkstra's Algorithm | `graph.js` | Shortest path routing |
| A* Search | `graph.js` | Heuristic-guided routing |
| Min-Heap | `graph.js` | Priority queue for Dijkstra/A* |
| Priority Queue | `priorityQueue.js` | Order scheduling by urgency |
| 0/1 Knapsack (DP) | `knapsack.js` | Vehicle load optimization |
| K-Means++ | `clustering.js` | Geographic order batching |
| Greedy Nearest Neighbor | `clustering.js` | Last-mile agent assignment |
| Greedy Workforce Match | `clustering.js` | Agent-order pairing |
| HashMap (JS Map) | `inventory.js` | O(1) stock lookups |

---

## 💡 Tech Stack
- **Frontend**: Vanilla HTML5 + CSS3 + JavaScript (ES6+)
- **Persistence**: localStorage (simulates MySQL CRUD)
- **Fonts**: Space Mono (monospace) + Outfit (sans-serif)
- **Canvas API**: Network map + live tracking visualization
- **Database**: MySQL 8.0+ (optional, schema provided)

---

## 📊 Cost Function
```
delivery_cost = (route_distance × ₹2/unit) + (delay_hours × ₹5/hr)
```

---

## 🔧 Customization
- Add new network nodes in `routing.js` → `NETWORK_NODES` array
- Adjust knapsack capacity limits in vehicle data (`tracking.js` → `seed()`)
- Change clustering `k` value or radius threshold in Route Optimizer
- Modify priority scoring formula in `priorityQueue.js` → `_calcScore()`
