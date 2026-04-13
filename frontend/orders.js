// ============================================================
// ORDERS MODULE
// Manages order lifecycle: create, track, return
// Backed by localStorage (simulates MySQL)
// ============================================================

const OrdersModule = (() => {
  const STORE_KEY = 'scs_orders';
  const API_BASE_URL = 'http://localhost:3001/api';
  let _id = parseInt(localStorage.getItem('scs_order_id') || '1000');

  const STATUS = {
    PENDING: 'PENDING',
    SCHEDULED: 'SCHEDULED',
    IN_TRANSIT: 'IN_TRANSIT',
    DELIVERED: 'DELIVERED',
    FAILED: 'FAILED',
    RETURNED: 'RETURNED'
  };

  const PRIORITY = { HIGH: 'HIGH', MEDIUM: 'MEDIUM', LOW: 'LOW' };

  async function _remoteFetch(path, options = {}) {
    try {
      const res = await fetch(`${API_BASE_URL}${path}`, options);
      if (!res.ok) {
        throw new Error(`API ${path} failed: ${res.status}`);
      }
      return await res.json();
    } catch (err) {
      console.warn('OrdersModule API request failed', err);
      return null;
    }
  }

  async function _syncOrder(order) {
    await _remoteFetch('/orders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(order)
    });
  }

  async function _syncOrderUpdate(order) {
    await _remoteFetch(`/orders/${order.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        customer_id: order.customerId,
        product_id: order.productId,
        quantity: order.quantity,
        total_weight: order.weight,
        total_volume: order.volume,
        total_value: order.value,
        priority: order.priority,
        status: order.status,
        destination_node: order.destinationNode,
        deadline: order.deadline,
        assigned_vehicle: order.assignedVehicle,
        assigned_agent: order.assignedAgent,
        delivery_cost: order.cost || order.deliveryCost || 0,
        fail_reason: order.failReason || null,
        notes: order.notes || null
      })
    });
  }

  async function _syncDeleteOrder(id) {
    await _remoteFetch(`/orders/${id}`, { method: 'DELETE' });
  }

  async function loadFromServer() {
    const rows = await _remoteFetch('/orders');
    if (!Array.isArray(rows) || rows.length === 0) {
      return getAll();
    }

    const normalized = rows.map(o => ({
      id: o.id,
      customerId: o.customer_id || o.customerId || 'CUST-000',
      customerName: o.customer_name || o.customerName || `Customer ${o.customer_id || ''}`,
      product: o.product || `Product ${o.product_id || ''}`,
      productId: o.product_id || o.productId,
      quantity: o.quantity || 1,
      weight: o.total_weight || o.weight || 0,
      volume: o.total_volume || o.volume || 0,
      value: o.total_value || o.value || 0,
      priority: o.priority || PRIORITY.MEDIUM,
      status: o.status || STATUS.PENDING,
      destinationNode: o.destination_node || o.destinationNode || 'N/A',
      destinationName: o.destination_name || o.destinationName || '',
      x: o.x || 0,
      y: o.y || 0,
      deadline: o.deadline ? new Date(o.deadline).toISOString() : new Date().toISOString(),
      createdAt: o.created_at || o.createdAt || new Date().toISOString(),
      updatedAt: o.updated_at || o.updatedAt || new Date().toISOString(),
      assignedVehicle: o.assigned_vehicle || o.assignedVehicle || null,
      assignedAgent: o.assigned_agent || o.assignedAgent || null,
      route: o.route || [],
      currentNode: o.currentNode || null,
      deliveryProgress: o.delivery_progress || o.deliveryProgress || 0,
      cost: o.delivery_cost || o.cost || 0,
      notes: o.notes || '',
      failReason: o.fail_reason || o.failReason || null
    }));

    _save(normalized);
    return normalized;
  }

  function _save(orders) {
    localStorage.setItem(STORE_KEY, JSON.stringify(orders));
  }

  function getAll() {
    return JSON.parse(localStorage.getItem(STORE_KEY) || '[]');
  }

  function getById(id) {
    return getAll().find(o => o.id === id) || null;
  }

  function getByStatus(status) {
    return getAll().filter(o => o.status === status);
  }

  function create(orderData) {
    const orders = getAll();
    const id = 'ORD-' + (++_id);
    localStorage.setItem('scs_order_id', _id);

    const now = new Date();
    const deadline = new Date(now.getTime() + (orderData.daysToDeadline || 3) * 86400000);

    const order = {
      id,
      customerId: orderData.customerId || 'CUST-001',
      customerName: orderData.customerName || 'Unknown',
      product: orderData.product,
      productId: orderData.productId,
      quantity: orderData.quantity || 1,
      weight: orderData.weight || 1,
      volume: orderData.volume || 1,
      value: orderData.value || 100,
      priority: orderData.priority || PRIORITY.MEDIUM,
      status: STATUS.PENDING,
      destinationNode: orderData.destinationNode,
      destinationName: orderData.destinationName,
      x: orderData.x || 0,
      y: orderData.y || 0,
      deadline: deadline.toISOString(),
      createdAt: now.toISOString(),
      updatedAt: now.toISOString(),
      assignedVehicle: null,
      assignedAgent: null,
      route: [],
      currentNode: null,
      deliveryProgress: 0,
      cost: null,
      notes: orderData.notes || ''
    };

    orders.push(order);
    _save(orders);
    _syncOrder(order);
    return order;
  }

  function update(id, changes) {
    const orders = getAll();
    const idx = orders.findIndex(o => o.id === id);
    if (idx === -1) return null;
    orders[idx] = { ...orders[idx], ...changes, updatedAt: new Date().toISOString() };
    _save(orders);
    _syncOrderUpdate(orders[idx]);
    return orders[idx];
  }

  function updateStatus(id, status, extras = {}) {
    return update(id, { status, ...extras });
  }

  function markFailed(id, reason = 'Delivery failed') {
    return update(id, { status: STATUS.FAILED, failReason: reason });
  }

  function initiateReturn(id) {
    const order = getById(id);
    if (!order) return null;
    return update(id, { status: STATUS.RETURNED, returnInitiated: new Date().toISOString() });
  }

  function deleteOrder(id) {
    const orders = getAll().filter(o => o.id !== id);
    _save(orders);
    _syncDeleteOrder(id);
  }

  function getStats() {
    const all = getAll();
    const stats = {};
    Object.values(STATUS).forEach(s => stats[s] = 0);
    all.forEach(o => stats[o.status] = (stats[o.status] || 0) + 1);
    stats.total = all.length;
    stats.totalRevenue = all.filter(o => o.status === STATUS.DELIVERED)
                            .reduce((s, o) => s + o.value, 0);
    return stats;
  }

  // Seed sample orders if empty
  function seed() {
    if (getAll().length > 0) return;
    const products = [
      { product: 'Electronics Package', productId: 'P001', weight: 2, volume: 3, value: 500 },
      { product: 'Furniture Set', productId: 'P002', weight: 15, volume: 20, value: 1200 },
      { product: 'Grocery Bundle', productId: 'P003', weight: 5, volume: 8, value: 150 },
      { product: 'Medical Supplies', productId: 'P004', weight: 1, volume: 2, value: 800 },
      { product: 'Sports Equipment', productId: 'P005', weight: 8, volume: 10, value: 300 },
    ];

    const customers = [
      { name: 'Arjun Sharma', dest: 'N3', x: 200, y: 150, days: 1 },
      { name: 'Priya Patel', dest: 'N5', x: 350, y: 280, days: 2 },
      { name: 'Rahul Verma', dest: 'N7', x: 150, y: 320, days: 3 },
      { name: 'Sneha Iyer', dest: 'N4', x: 400, y: 180, days: 1 },
      { name: 'Kiran Mehta', dest: 'N6', x: 300, y: 380, days: 2 },
    ];

    customers.forEach((c, i) => {
      const p = products[i % products.length];
      create({
        customerName: c.name, customerId: `CUST-${100 + i}`,
        product: p.product, productId: p.productId,
        quantity: Math.ceil(Math.random() * 3),
        weight: p.weight, volume: p.volume, value: p.value,
        priority: ['HIGH', 'MEDIUM', 'LOW'][i % 3],
        destinationNode: c.dest, destinationName: c.name + "'s Address",
        x: c.x, y: c.y, daysToDeadline: c.days
      });
    });
  }

  return { getAll, getById, getByStatus, create, update, updateStatus,
           markFailed, initiateReturn, deleteOrder, getStats, loadFromServer, seed, STATUS, PRIORITY };
})();
