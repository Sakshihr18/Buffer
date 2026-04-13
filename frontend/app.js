// ============================================================
// APP.JS — Main Application Controller
// Smart Supply Chain Optimization System
// ============================================================

// ── Global State ──────────────────────────────────────────
const AppState = {
  currentPage: 'dashboard',
  selectedOrder: null,
  liveTrackingInterval: null,
  mapCanvas: null,
  mapCtx: null,
  networkCanvas: null,
  networkCtx: null,
  activeRoute: null,
  trafficMode: false,
  scheduler: null,
  notifications: [],
};

// ── Boot ──────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  initApp();
});

async function initApp() {
  await OrdersModule.loadFromServer();
  await InventoryModule.loadFromServer();

  if (OrdersModule.getAll().length === 0) {
    OrdersModule.seed();
  }
  if (InventoryModule.getAll().length === 0) {
    InventoryModule.seed();
  }

  TrackingModule.seed();
  RoutingModule.buildGraph();

  AppState.scheduler = new DeliveryScheduler();
  OrdersModule.getAll()
    .filter(o => o.status === 'PENDING' || o.status === 'SCHEDULED')
    .forEach(o => AppState.scheduler.addOrder(o));

  setupNavigation();
  renderDashboard();
  setupCanvases();
  startNotificationLoop();

  navigateTo('dashboard');
}

// ── Navigation ─────────────────────────────────────────────
function setupNavigation() {
  document.querySelectorAll('[data-page]').forEach(btn => {
    btn.addEventListener('click', () => navigateTo(btn.dataset.page));
  });
}

function navigateTo(page) {
  AppState.currentPage = page;
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('[data-page]').forEach(b => b.classList.remove('active'));

  const pageEl = document.getElementById('page-' + page);
  const navBtn = document.querySelector(`[data-page="${page}"]`);
  if (pageEl) pageEl.classList.add('active');
  if (navBtn) navBtn.classList.add('active');

  // Render page-specific content
  switch (page) {
    case 'dashboard':    renderDashboard(); break;
    case 'orders':       renderOrders(); break;
    case 'inventory':    renderInventory(); break;
    case 'routing':      renderRouting(); break;
    case 'tracking':     renderTracking(); break;
    case 'vehicles':     renderVehicles(); break;
    case 'place-order':  renderPlaceOrder(); break;
    case 'returns':      renderReturns(); break;
    case 'workforce':    renderWorkforce(); break;
  }
}

// ── Dashboard ─────────────────────────────────────────────
function renderDashboard() {
  const stats = OrdersModule.getStats();
  const invStats = InventoryModule.getStats();
  const alerts = InventoryModule.getAllAlerts();

  setEl('stat-total-orders', stats.total);
  setEl('stat-in-transit', stats.IN_TRANSIT || 0);
  setEl('stat-delivered', stats.DELIVERED || 0);
  setEl('stat-pending', stats.PENDING || 0);
  setEl('stat-inventory-value', '₹' + (invStats.totalValue || 0).toLocaleString());
  setEl('stat-low-stock', invStats.lowStock || 0);
  setEl('stat-alerts', alerts.length);

  // Recent orders table
  const recent = OrdersModule.getAll().slice(-5).reverse();
  renderOrderTable('recent-orders-table', recent);

  // Alert feed
  renderAlerts(alerts);
}

// ── Orders Page ───────────────────────────────────────────
function renderOrders() {
  const orders = OrdersModule.getAll().reverse();
  renderOrderTable('orders-table', orders);
}

function renderOrderTable(tableId, orders) {
  const tbody = document.getElementById(tableId);
  if (!tbody) return;
  tbody.innerHTML = orders.length === 0
    ? '<tr><td colspan="8" class="empty-row">No orders found</td></tr>'
    : orders.map(o => `
      <tr class="order-row status-${o.status.toLowerCase()}">
        <td><span class="order-id">${o.id}</span></td>
        <td>${o.customerName || 'N/A'}</td>
        <td>${o.product}</td>
        <td><span class="badge badge-${o.priority.toLowerCase()}">${o.priority}</span></td>
        <td><span class="status-pill status-${o.status.toLowerCase()}">${o.status}</span></td>
        <td>${o.destinationNode || '-'}</td>
        <td>₹${(o.value || 0).toLocaleString()}</td>
        <td class="actions">
          <button class="btn-sm btn-primary" onclick="viewOrderDetails('${o.id}')">View</button>
          ${o.status === 'PENDING'
            ? `<button class="btn-sm btn-success" onclick="scheduleOrder('${o.id}')">Schedule</button>` : ''}
          ${o.status === 'DELIVERED'
            ? `<button class="btn-sm btn-warning" onclick="initiateReturn('${o.id}')">Return</button>` : ''}
          ${o.status === 'FAILED'
            ? `<button class="btn-sm btn-info" onclick="reassignOrder('${o.id}')">Reassign</button>` : ''}
        </td>
      </tr>`).join('');
}

// ── Order Actions ─────────────────────────────────────────
function viewOrderDetails(orderId) {
  const order = OrdersModule.getById(orderId);
  if (!order) return;
  const route = RoutingModule.optimizeRoute('W1', order.destinationNode);
  const cost = RoutingModule.calculateCost(route);

  showModal(`
    <h2>📦 Order Details — ${order.id}</h2>
    <div class="detail-grid">
      <div class="detail-item"><span>Customer</span><strong>${order.customerName}</strong></div>
      <div class="detail-item"><span>Product</span><strong>${order.product}</strong></div>
      <div class="detail-item"><span>Quantity</span><strong>${order.quantity}</strong></div>
      <div class="detail-item"><span>Weight</span><strong>${order.weight} kg</strong></div>
      <div class="detail-item"><span>Volume</span><strong>${order.volume} m³</strong></div>
      <div class="detail-item"><span>Value</span><strong>₹${order.value?.toLocaleString()}</strong></div>
      <div class="detail-item"><span>Priority</span><strong class="badge badge-${order.priority.toLowerCase()}">${order.priority}</strong></div>
      <div class="detail-item"><span>Status</span><strong class="status-pill status-${order.status.toLowerCase()}">${order.status}</strong></div>
      <div class="detail-item"><span>Deadline</span><strong>${new Date(order.deadline).toLocaleDateString()}</strong></div>
      <div class="detail-item"><span>Destination</span><strong>${order.destinationNode}</strong></div>
    </div>
    <div class="route-info">
      <h3>🗺 Optimal Route (Dijkstra)</h3>
      <div class="route-path">${route.path.join(' → ') || 'N/A'}</div>
      <div class="cost-breakdown">
        <span>Distance Cost: ₹${cost.baseCost}</span>
        <span>Total: ₹${cost.total}</span>
        <span>Hops: ${route.hops}</span>
      </div>
    </div>
    <div class="modal-actions">
      <button class="btn btn-primary" onclick="startDeliveryTracking('${order.id}'); closeModal()">🚚 Start Tracking</button>
      <button class="btn btn-secondary" onclick="closeModal()">Close</button>
    </div>
  `);
}

function scheduleOrder(orderId) {
  const order = OrdersModule.getById(orderId);
  if (!order) return;

  // Optimize route
  const route = RoutingModule.optimizeRoute('W1', order.destinationNode);
  const cost = RoutingModule.calculateCost(route);

  // Reserve inventory
  InventoryModule.reserveStock(order.productId, order.quantity);

  // Update order
  OrdersModule.update(orderId, {
    status: 'SCHEDULED',
    route: route.path,
    cost: cost.total
  });

  addNotification('success', `Order ${orderId} scheduled → Route: ${route.path.join('→')}`);
  renderOrders();
}

function initiateReturn(orderId) {
  const order = OrdersModule.getById(orderId);
  if (!order) return;
  const reverseRoute = RoutingModule.calculateReturnRoute(order.destinationNode);
  const cost = RoutingModule.calculateCost(reverseRoute);

  OrdersModule.initiateReturn(orderId);
  addNotification('info', `Return initiated for ${orderId}. Reverse route: ${reverseRoute.path.join('→')} (₹${cost.total})`);
  renderOrders();
}

function reassignOrder(orderId) {
  const order = OrdersModule.getById(orderId);
  const agents = TrackingModule.getAgents();
  const failedLoc = { x: order.x, y: order.y };
  const { agent, distance } = nearestNeighborAssign(failedLoc, agents);

  if (agent) {
    OrdersModule.update(orderId, { status: 'SCHEDULED', assignedAgent: agent.id });
    TrackingModule.setAgentAvailable(agent.id, false);
    addNotification('success', `Reassigned ${orderId} to ${agent.name} (dist: ${distance.toFixed(1)})`);
  } else {
    addNotification('error', 'No available agents for reassignment');
  }
  renderOrders();
}

// ── Place Order Form ───────────────────────────────────────
function renderPlaceOrder() {
  const nodes = RoutingModule.NODES.filter(n => n.type === 'zone');
  const inv = InventoryModule.getAll().filter(i => i.quantity > 0);

  setHTML('place-order-form', `
    <form class="order-form" onsubmit="submitOrder(event)">
      <div class="form-row">
        <div class="form-group">
          <label>Your Name</label>
          <input type="text" id="f-name" placeholder="Arjun Sharma" required>
        </div>
        <div class="form-group">
          <label>Customer ID</label>
          <input type="text" id="f-custid" placeholder="CUST-100">
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Product</label>
          <select id="f-product" onchange="updateProductInfo()" required>
            <option value="">Select Product...</option>
            ${inv.map(i => `<option value="${i.id}" data-weight="${i.weight||1}" data-volume="${i.volume||1}" data-price="${i.unitPrice||100}">${i.name} (${i.quantity} in stock)</option>`).join('')}
          </select>
        </div>
        <div class="form-group">
          <label>Quantity</label>
          <input type="number" id="f-qty" min="1" value="1" onchange="updateProductInfo()">
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Delivery Zone</label>
          <select id="f-zone" required>
            ${nodes.map(n => `<option value="${n.id}" data-x="${n.x}" data-y="${n.y}">${n.name} (${n.id})</option>`).join('')}
          </select>
        </div>
        <div class="form-group">
          <label>Priority</label>
          <select id="f-priority">
            <option value="HIGH">🔴 HIGH — Express</option>
            <option value="MEDIUM" selected>🟡 MEDIUM — Standard</option>
            <option value="LOW">🟢 LOW — Economy</option>
          </select>
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Delivery Deadline (days)</label>
          <input type="number" id="f-deadline" min="1" max="14" value="3">
        </div>
        <div class="form-group">
          <label>Notes</label>
          <input type="text" id="f-notes" placeholder="Optional instructions">
        </div>
      </div>
      <div id="order-preview" class="order-preview hidden"></div>
      <div class="form-actions">
        <button type="submit" class="btn btn-primary btn-lg">📦 Place Order</button>
        <button type="button" class="btn btn-secondary" onclick="updateProductInfo()">🔍 Preview Route</button>
      </div>
    </form>
  `);
}

function updateProductInfo() {
  const sel = document.getElementById('f-product');
  const qty = parseInt(document.getElementById('f-qty')?.value || 1);
  const zone = document.getElementById('f-zone');
  if (!sel || !sel.value || !zone) return;

  const opt = sel.options[sel.selectedIndex];
  const weight = parseFloat(opt.dataset.weight || 1) * qty;
  const volume = parseFloat(opt.dataset.volume || 1) * qty;
  const price = parseFloat(opt.dataset.price || 100) * qty;

  const zoneOpt = zone.options[zone.selectedIndex];
  const destNode = zoneOpt?.value || 'N3';
  const route = RoutingModule.optimizeRoute('W1', destNode);
  const cost = RoutingModule.calculateCost(route);

  const preview = document.getElementById('order-preview');
  if (preview) {
    preview.classList.remove('hidden');
    preview.innerHTML = `
      <div class="preview-grid">
        <div>📦 <strong>Weight:</strong> ${weight} kg</div>
        <div>📐 <strong>Volume:</strong> ${volume} m³</div>
        <div>💰 <strong>Value:</strong> ₹${price.toLocaleString()}</div>
        <div>🗺 <strong>Route:</strong> ${route.path.join(' → ')}</div>
        <div>🚀 <strong>Hops:</strong> ${route.hops}</div>
        <div>💸 <strong>Delivery Cost:</strong> ₹${cost.total}</div>
      </div>
    `;
  }
}

function submitOrder(e) {
  e.preventDefault();
  const sel = document.getElementById('f-product');
  const zoneEl = document.getElementById('f-zone');
  const opt = sel.options[sel.selectedIndex];
  const zoneOpt = zoneEl.options[zoneEl.selectedIndex];
  const qty = parseInt(document.getElementById('f-qty').value);

  const order = OrdersModule.create({
    customerName: document.getElementById('f-name').value,
    customerId: document.getElementById('f-custid').value || 'CUST-NEW',
    productId: sel.value,
    product: opt.text.split(' (')[0],
    quantity: qty,
    weight: parseFloat(opt.dataset.weight || 1) * qty,
    volume: parseFloat(opt.dataset.volume || 1) * qty,
    value: parseFloat(opt.dataset.price || 100) * qty,
    priority: document.getElementById('f-priority').value,
    destinationNode: zoneOpt.value,
    destinationName: zoneOpt.text,
    x: parseFloat(zoneOpt.dataset.x || 0),
    y: parseFloat(zoneOpt.dataset.y || 0),
    daysToDeadline: parseInt(document.getElementById('f-deadline').value || 3),
    notes: document.getElementById('f-notes').value
  });

  AppState.scheduler.addOrder(order);
  addNotification('success', `Order ${order.id} placed! Optimizing route...`);

  // Auto-schedule
  setTimeout(() => scheduleOrder(order.id), 500);
  navigateTo('orders');
}

// ── Inventory ─────────────────────────────────────────────
function renderInventory() {
  const items = InventoryModule.getAll();
  const alerts = InventoryModule.getAllAlerts();

  setHTML('inventory-alerts', alerts.map(a => `
    <div class="alert alert-${a.severity}">
      <span class="alert-icon">${a.severity === 'critical' ? '🚨' : a.severity === 'warning' ? '⚠️' : 'ℹ️'}</span>
      ${a.message}
    </div>`).join('') || '<p class="ok-msg">✅ All stock levels normal</p>');

  setHTML('inventory-table', items.map(item => {
    const pct = Math.min(100, Math.round((item.quantity / (item.maxCapacity || 200)) * 100));
    const alertStatus = InventoryModule.getAlertStatus(item.id);
    return `
      <tr>
        <td>${item.id}</td>
        <td>${item.name}</td>
        <td>${item.category || '-'}</td>
        <td>
          <div class="stock-bar-wrap">
            <div class="stock-bar stock-${alertStatus?.type?.toLowerCase() || 'ok'}" style="width:${pct}%"></div>
          </div>
          <span class="qty-label">${item.quantity} / ${item.maxCapacity}</span>
        </td>
        <td>${item.reserved || 0}</td>
        <td>₹${(item.unitPrice || 0).toLocaleString()}</td>
        <td><span class="status-pill status-${alertStatus?.type?.toLowerCase() || 'ok'}">${alertStatus?.type || 'OK'}</span></td>
        <td>
          <button class="btn-sm btn-success" onclick="restockItem('${item.id}', 20)">+20</button>
          <button class="btn-sm btn-warning" onclick="adjustStockItem('${item.id}', -5)">-5</button>
        </td>
      </tr>`;
  }).join(''));
}

function restockItem(id, qty) {
  InventoryModule.restock(id, qty);
  addNotification('success', `Restocked ${id} by +${qty} units`);
  renderInventory();
}

function adjustStockItem(id, delta) {
  InventoryModule.adjustStock(id, delta);
  renderInventory();
}

// ── Routing Page ───────────────────────────────────────────
function renderRouting() {
  const nodes = RoutingModule.NODES;
  const sourceOpts = nodes.map(n => `<option value="${n.id}">${n.name} (${n.id})</option>`).join('');
  const destOpts = nodes.filter(n => n.type !== 'warehouse')
    .map(n => `<option value="${n.id}">${n.name} (${n.id})</option>`).join('');

  setHTML('route-controls', `
    <div class="route-form">
      <div class="form-row">
        <div class="form-group">
          <label>Source Node</label>
          <select id="route-source">${sourceOpts}</select>
        </div>
        <div class="form-group">
          <label>Destination Node</label>
          <select id="route-dest">${destOpts}</select>
        </div>
        <div class="form-group">
          <label>Algorithm</label>
          <select id="route-algo">
            <option value="dijkstra">Dijkstra</option>
            <option value="astar">A* Search</option>
          </select>
        </div>
      </div>
      <div class="btn-group">
        <button class="btn btn-primary" onclick="runRouteOptimization()">⚡ Optimize Route</button>
        <button class="btn btn-danger" onclick="simulateDelay()">🚦 Simulate Traffic</button>
        <button class="btn btn-secondary" onclick="clearDelays()">🔄 Clear Delays</button>
        <button class="btn btn-warning" onclick="runKnapsack()">🎒 Assign Vehicle (Knapsack)</button>
        <button class="btn btn-info" onclick="runOrderBatching()">📦 Batch Orders (K-Means)</button>
      </div>
    </div>
  `);

  drawNetwork();
}

function runRouteOptimization() {
  const source = document.getElementById('route-source')?.value || 'W1';
  const dest = document.getElementById('route-dest')?.value || 'N3';
  const algo = document.getElementById('route-algo')?.value || 'dijkstra';

  const route = RoutingModule.optimizeRoute(source, dest, algo);
  const costNow = RoutingModule.calculateCost(route);

  // Also compute without traffic (rebuild clean graph)
  const graph = RoutingModule.getGraph();
  const { dist: cleanDist, prev: cleanPrev } = dijkstra(graph, source);
  const cleanPath = reconstructPath(cleanPrev, source, dest);
  const cleanCost = RoutingModule.calculateCost({ cost: cleanDist.get(dest), path: cleanPath });

  AppState.activeRoute = route;
  drawNetwork(route.path);

  setHTML('route-result', `
    <div class="result-card">
      <h3>🗺 Route: ${source} → ${dest} <span class="badge badge-algo">${algo.toUpperCase()}</span></h3>
      <div class="path-visual">${route.path.map((n, i) => `
        <span class="path-node ${i === 0 ? 'src' : i === route.path.length - 1 ? 'dest' : ''}">${n}</span>
        ${i < route.path.length - 1 ? '<span class="arrow">→</span>' : ''}`).join('')}</div>
      <div class="cost-compare">
        <div class="cost-box before"><span>Before</span><strong>₹${cleanCost.total}</strong></div>
        <div class="cost-box after"><span>After Optimization</span><strong>₹${costNow.total}</strong></div>
        <div class="cost-box savings"><span>Savings</span><strong>₹${Math.max(0, cleanCost.total - costNow.total)}</strong></div>
      </div>
      <div class="route-stats">
        <span>Hops: ${route.hops}</span>
        <span>Base Cost: ₹${costNow.baseCost}</span>
        <span>Delay Penalty: ₹${costNow.delayPenalty}</span>
        <span>Valid: ${route.isValid ? '✅' : '❌'}</span>
      </div>
    </div>
  `);
}

function simulateDelay() {
  // Apply random delay on a random edge
  const edges = [['W1','H1'], ['W1','H2'], ['H2','N5'], ['H3','N6'], ['W1','H3']];
  const edge = edges[Math.floor(Math.random() * edges.length)];
  const mult = (1.5 + Math.random() * 2).toFixed(1);
  const result = RoutingModule.simulateTrafficDelay(edge[0], edge[1], parseFloat(mult));
  addNotification('warning', `🚦 Traffic delay: ${result.message}`);
  setHTML('delay-status', `<div class="alert alert-warning">Active Delay: ${result.affectedEdge} × ${mult}</div>`);
  drawNetwork();

  // Show recalculation
  if (AppState.activeRoute) {
    setTimeout(() => {
      addNotification('info', '🔄 Recalculating optimal route...');
      runRouteOptimization();
    }, 800);
  }
}

function clearDelays() {
  RoutingModule.clearAllDelays();
  addNotification('success', 'All traffic delays cleared');
  setHTML('delay-status', '');
  drawNetwork();
}

function runKnapsack() {
  const orders = OrdersModule.getAll().filter(o => o.status === 'PENDING' || o.status === 'SCHEDULED');
  const vehicles = TrackingModule.getVehicles().filter(v => v.available);

  const { assignments, unassignedOrders } = assignOrdersToVehicles(orders, vehicles);

  let html = '<h3>🎒 Knapsack Vehicle Assignment</h3>';
  assignments.forEach(a => {
    const wPct = Math.round((a.usedWeight / a.weightCapacity) * 100);
    html += `
      <div class="vehicle-card">
        <div class="vehicle-header"><strong>${a.vehicleName}</strong>
          <span class="badge">₹${a.maxValue}</span>
        </div>
        <div class="capacity-bars">
          <div class="cap-row"><span>Weight</span>
            <div class="bar"><div class="fill" style="width:${wPct}%"></div></div>
            <span>${a.usedWeight}/${a.weightCapacity}kg</span>
          </div>
        </div>
        <div class="assigned-orders">Orders: ${a.orders.map(o => `<span class="tag">${o.id}</span>`).join('')}</div>
      </div>`;
  });
  if (unassignedOrders.length > 0) {
    html += `<div class="alert alert-warning">⚠️ Unassigned: ${unassignedOrders.map(o => o.id).join(', ')}</div>`;
  }
  setHTML('knapsack-result', html);
}

function runOrderBatching() {
  const orders = OrdersModule.getAll()
    .filter(o => o.status !== 'DELIVERED' && o.x && o.y)
    .map(o => ({ ...o, orderId: o.id }));

  const k = Math.min(3, orders.length);
  const clusters = kMeansClustering(orders, k);

  const COLORS = ['#F97316', '#3B82F6', '#10B981'];
  let html = `<h3>📦 Order Batching (K-Means, k=${k})</h3>`;
  clusters.forEach((c, i) => {
    html += `
      <div class="cluster-card" style="border-left: 4px solid ${COLORS[i % COLORS.length]}">
        <strong>Cluster ${i + 1}</strong> — ${c.orders.length} orders
        <div>Centroid: (${c.centroid.x.toFixed(0)}, ${c.centroid.y.toFixed(0)})</div>
        <div>${c.orders.map(id => `<span class="tag">${id}</span>`).join('')}</div>
      </div>`;
  });
  setHTML('batching-result', html);
}

// ── Live Tracking ─────────────────────────────────────────
function renderTracking() {
  const orders = OrdersModule.getAll()
    .filter(o => ['SCHEDULED', 'IN_TRANSIT'].includes(o.status));

  setHTML('tracking-list', orders.length === 0
    ? '<p class="empty-msg">No active deliveries</p>'
    : orders.map(o => `
      <div class="track-card" id="track-${o.id}">
        <div class="track-header">
          <span>${o.id}</span>
          <span class="status-pill status-${o.status.toLowerCase()}">${o.status}</span>
        </div>
        <div class="track-info">${o.customerName} → ${o.destinationNode}</div>
        <div class="track-progress">
          <div class="progress-bar"><div class="progress-fill" style="width:${o.deliveryProgress || 0}%"></div></div>
          <span>${o.deliveryProgress || 0}%</span>
        </div>
        <button class="btn-sm btn-primary" onclick="startDeliveryTracking('${o.id}')">▶ Track Live</button>
      </div>`).join(''));
}

function startDeliveryTracking(orderId) {
  const order = OrdersModule.getById(orderId);
  if (!order) return;

  let route = order.route;
  if (!route || route.length === 0) {
    const optimized = RoutingModule.optimizeRoute('W1', order.destinationNode);
    route = optimized.path;
    OrdersModule.update(orderId, { route, status: 'IN_TRANSIT' });
  } else {
    OrdersModule.updateStatus(orderId, 'IN_TRANSIT');
  }

  navigateTo('tracking');

  const logEl = document.getElementById('tracking-log');
  if (logEl) logEl.innerHTML = '';

  TrackingModule.startTracking(
    orderId, route,
    (state) => {
      // Update progress card
      const card = document.getElementById('track-' + orderId);
      if (card) {
        const fill = card.querySelector('.progress-fill');
        const pct = card.querySelector('.progress-bar + span');
        if (fill) fill.style.width = state.progress + '%';
        if (pct) pct.textContent = state.progress + '%';
      }
      OrdersModule.update(orderId, { deliveryProgress: state.progress, currentNode: state.currentPosition });

      // Draw on canvas
      drawTrackingPath(route, state.currentStep);

      // Log
      appendLog(`[${new Date().toLocaleTimeString()}] Vehicle at ${state.currentPosition} → Next: ${state.nextPosition} (${state.progress}%)`);
    },
    (state) => {
      OrdersModule.updateStatus(orderId, 'DELIVERED', { deliveryProgress: 100 });
      InventoryModule.releaseReservation(order.productId, order.quantity);
      appendLog(`✅ [${new Date().toLocaleTimeString()}] Delivered! Order ${orderId} complete.`);
      addNotification('success', `📬 Order ${orderId} delivered to ${order.customerName}!`);
      renderTracking();
    }
  );

  addNotification('info', `🚚 Live tracking started for ${orderId}`);
}

function appendLog(msg) {
  const log = document.getElementById('tracking-log');
  if (!log) return;
  const line = document.createElement('div');
  line.className = 'log-line';
  line.textContent = msg;
  log.prepend(line);
  if (log.children.length > 20) log.lastChild.remove();
}

// ── Vehicles ──────────────────────────────────────────────
function renderVehicles() {
  const vehicles = TrackingModule.getVehicles();
  setHTML('vehicles-grid', vehicles.map(v => `
    <div class="vehicle-card-full">
      <div class="vehicle-icon">${v.type === 'truck' ? '🚛' : v.type === 'van' ? '🚐' : '🏍️'}</div>
      <div class="vehicle-info">
        <strong>${v.name}</strong>
        <span class="badge badge-${v.available ? 'success' : 'danger'}">${v.available ? 'Available' : 'On Route'}</span>
      </div>
      <div class="vehicle-specs">
        <span>⚖️ ${v.weightCapacity}kg</span>
        <span>📦 ${v.volumeCapacity}m³</span>
        <span>⛽ ${v.fuel}%</span>
        <span>📍 ${v.currentNode}</span>
      </div>
    </div>`).join(''));
}

// ── Returns ───────────────────────────────────────────────
function renderReturns() {
  const returned = OrdersModule.getByStatus('DELIVERED');
  const failed = OrdersModule.getByStatus('FAILED');
  const all = [...returned, ...failed];

  setHTML('returns-list', all.length === 0
    ? '<p class="empty-msg">No eligible orders for return</p>'
    : all.map(o => `
      <div class="return-card">
        <div class="return-info">
          <strong>${o.id}</strong> — ${o.customerName}
          <span class="status-pill status-${o.status.toLowerCase()}">${o.status}</span>
        </div>
        <div>${o.product} × ${o.quantity}</div>
        <button class="btn-sm btn-warning" onclick="processReturn('${o.id}')">🔄 Process Return</button>
      </div>`).join(''));
}

function processReturn(orderId) {
  const order = OrdersModule.getById(orderId);
  const reverseRoute = RoutingModule.calculateReturnRoute(order.destinationNode);
  const cost = RoutingModule.calculateCost(reverseRoute);

  OrdersModule.initiateReturn(orderId);
  InventoryModule.adjustStock(order.productId, order.quantity); // restock

  showModal(`
    <h2>🔄 Return Processed — ${orderId}</h2>
    <div class="route-info">
      <div>Return Route: <strong>${reverseRoute.path.join(' → ')}</strong></div>
      <div>Return Cost: <strong>₹${cost.total}</strong></div>
      <div>Status: <strong>RETURNED</strong></div>
    </div>
    <div class="modal-actions">
      <button class="btn btn-primary" onclick="closeModal()">Confirm</button>
    </div>
  `);
  addNotification('info', `Return initiated for ${orderId}`);
  renderReturns();
}

// ── Workforce ─────────────────────────────────────────────
function renderWorkforce() {
  const agents = TrackingModule.getAgents();
  const pendingOrders = OrdersModule.getAll().filter(o => o.status === 'SCHEDULED' && !o.assignedAgent);
  const assignments = greedyWorkforceMatch(pendingOrders, agents);

  setHTML('agents-grid', agents.map(a => `
    <div class="agent-card">
      <div class="agent-avatar">${a.name.split(' ').map(n => n[0]).join('')}</div>
      <div class="agent-info">
        <strong>${a.name}</strong>
        <div class="stars">${'★'.repeat(Math.floor(a.rating))}${'☆'.repeat(5 - Math.floor(a.rating))}</div>
        <span class="badge badge-${a.available ? 'success' : 'danger'}">${a.available ? 'Available' : 'Busy'}</span>
      </div>
      <div class="agent-stats">
        <span>📦 ${a.deliveries} deliveries</span>
        <span>📍 (${a.x}, ${a.y})</span>
      </div>
    </div>`).join(''));

  setHTML('workforce-assignments', assignments.length === 0
    ? '<p class="empty-msg">No pending assignments</p>'
    : assignments.map(a => `
      <div class="assignment-row">
        <span class="tag">${a.order.id}</span>
        <span>→</span>
        <strong>${a.agent.name}</strong>
        <span class="dist-badge">dist: ${a.distance.toFixed(1)}</span>
        <button class="btn-sm btn-success" onclick="confirmAssignment('${a.order.id}','${a.agent.id}')">Confirm</button>
      </div>`).join(''));
}

function confirmAssignment(orderId, agentId) {
  const agent = TrackingModule.getAgents().find(a => a.id === agentId);
  OrdersModule.update(orderId, { assignedAgent: agentId });
  TrackingModule.setAgentAvailable(agentId, false);
  addNotification('success', `${agent?.name} assigned to ${orderId}`);
  renderWorkforce();
}

// ── Canvas: Network Map ────────────────────────────────────
function setupCanvases() {
  const nc = document.getElementById('network-canvas');
  const tc = document.getElementById('tracking-canvas');
  if (nc) { AppState.networkCanvas = nc; AppState.networkCtx = nc.getContext('2d'); }
  if (tc) { AppState.mapCanvas = tc; AppState.mapCtx = tc.getContext('2d'); }
}

function drawNetwork(highlightPath = []) {
  const canvas = document.getElementById('network-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const graph = RoutingModule.getGraph();
  const W = canvas.width, H = canvas.height;
  ctx.clearRect(0, 0, W, H);

  const scaleX = W / 520, scaleY = H / 480;

  // Draw edges
  graph.edges.forEach((edges, fromId) => {
    const from = graph.getNode(fromId);
    if (!from) return;
    edges.forEach(({ to: toId, weight }) => {
      const to = graph.getNode(toId);
      if (!to) return;
      const isHighlighted = highlightPath.includes(fromId) && highlightPath.includes(toId);
      ctx.beginPath();
      ctx.moveTo(from.x * scaleX, from.y * scaleY);
      ctx.lineTo(to.x * scaleX, to.y * scaleY);
      ctx.strokeStyle = isHighlighted ? '#F97316' : '#334155';
      ctx.lineWidth = isHighlighted ? 3 : 1;
      ctx.stroke();
      // Weight label
      const mx = (from.x + to.x) / 2 * scaleX, my = (from.y + to.y) / 2 * scaleY;
      ctx.fillStyle = '#64748B';
      ctx.font = '10px monospace';
      ctx.fillText(weight, mx, my);
    });
  });

  // Draw nodes
  const NODE_COLORS = { warehouse: '#EF4444', hub: '#F97316', zone: '#3B82F6' };
  graph.nodes.forEach((node) => {
    const x = node.x * scaleX, y = node.y * scaleY;
    const isInPath = highlightPath.includes(node.id);
    ctx.beginPath();
    ctx.arc(x, y, isInPath ? 12 : 8, 0, Math.PI * 2);
    ctx.fillStyle = isInPath ? '#FBBF24' : (NODE_COLORS[node.type] || '#64748B');
    ctx.fill();
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.fillStyle = '#F8FAFC';
    ctx.font = 'bold 10px monospace';
    ctx.textAlign = 'center';
    ctx.fillText(node.id, x, y - 14);
  });
}

function drawTrackingPath(route, currentStep) {
  const canvas = document.getElementById('tracking-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const graph = RoutingModule.getGraph();
  const W = canvas.width, H = canvas.height;
  ctx.clearRect(0, 0, W, H);
  const scaleX = W / 520, scaleY = H / 480;

  // Draw full route (dashed)
  ctx.setLineDash([5, 5]);
  ctx.strokeStyle = '#334155';
  ctx.lineWidth = 1;
  for (let i = 0; i < route.length - 1; i++) {
    const a = graph.getNode(route[i]), b = graph.getNode(route[i + 1]);
    if (!a || !b) continue;
    ctx.beginPath();
    ctx.moveTo(a.x * scaleX, a.y * scaleY);
    ctx.lineTo(b.x * scaleX, b.y * scaleY);
    ctx.stroke();
  }
  ctx.setLineDash([]);

  // Draw traveled path (solid)
  ctx.strokeStyle = '#10B981';
  ctx.lineWidth = 3;
  for (let i = 0; i < Math.min(currentStep, route.length - 1); i++) {
    const a = graph.getNode(route[i]), b = graph.getNode(route[i + 1]);
    if (!a || !b) continue;
    ctx.beginPath();
    ctx.moveTo(a.x * scaleX, a.y * scaleY);
    ctx.lineTo(b.x * scaleX, b.y * scaleY);
    ctx.stroke();
  }

  // Draw nodes
  route.forEach((id, idx) => {
    const node = graph.getNode(id);
    if (!node) return;
    const x = node.x * scaleX, y = node.y * scaleY;
    ctx.beginPath();
    ctx.arc(x, y, 8, 0, Math.PI * 2);
    ctx.fillStyle = idx === 0 ? '#EF4444' : idx === route.length - 1 ? '#10B981' : '#3B82F6';
    ctx.fill();
    ctx.fillStyle = '#F8FAFC';
    ctx.font = 'bold 9px monospace';
    ctx.textAlign = 'center';
    ctx.fillText(id, x, y - 12);
  });

  // Draw vehicle position
  if (currentStep < route.length) {
    const cur = graph.getNode(route[currentStep]);
    if (cur) {
      ctx.beginPath();
      ctx.arc(cur.x * scaleX, cur.y * scaleY, 12, 0, Math.PI * 2);
      ctx.fillStyle = '#FBBF24';
      ctx.fill();
      ctx.fillStyle = '#000';
      ctx.font = '12px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('🚚', cur.x * scaleX, cur.y * scaleY + 4);
    }
  }
}

// ── Notifications ─────────────────────────────────────────
function addNotification(type, msg) {
  AppState.notifications.push({ type, msg, ts: Date.now() });
  const dot = document.getElementById('notif-dot');
  if (dot) dot.style.display = 'block';

  const feed = document.getElementById('notification-feed');
  if (feed) {
    const el = document.createElement('div');
    el.className = `notif notif-${type}`;
    el.innerHTML = `<span>${type === 'success' ? '✅' : type === 'error' ? '❌' : type === 'warning' ? '⚠️' : 'ℹ️'}</span> ${msg}`;
    feed.prepend(el);
    setTimeout(() => el.classList.add('fade-out'), 4000);
    setTimeout(() => el.remove(), 5000);
  }
}

function startNotificationLoop() {
  setInterval(() => {
    const alerts = InventoryModule.getAllAlerts();
    if (alerts.length > 0 && Math.random() > 0.9) {
      const alert = alerts[Math.floor(Math.random() * alerts.length)];
      addNotification(alert.severity === 'critical' ? 'error' : 'warning', alert.message);
    }
  }, 15000);
}

function renderAlerts(alerts) {
  const el = document.getElementById('dashboard-alerts');
  if (!el) return;
  el.innerHTML = alerts.slice(0, 5).map(a => `
    <div class="alert-item alert-${a.severity}">
      <span>${a.severity === 'critical' ? '🚨' : '⚠️'}</span> ${a.message}
    </div>`).join('') || '<div class="ok-msg">✅ No alerts</div>';
}

// ── Modal ─────────────────────────────────────────────────
function showModal(html) {
  const modal = document.getElementById('modal-overlay');
  const content = document.getElementById('modal-content');
  if (modal && content) {
    content.innerHTML = html;
    modal.classList.add('active');
  }
}
function closeModal() {
  const modal = document.getElementById('modal-overlay');
  if (modal) modal.classList.remove('active');
}

// ── Helpers ───────────────────────────────────────────────
function setEl(id, val) {
  const el = document.getElementById(id);
  if (el) el.textContent = val;
}
function setHTML(id, html) {
  const el = document.getElementById(id);
  if (el) el.innerHTML = html;
}
