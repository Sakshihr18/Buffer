// ============================================================
// INVENTORY MODULE
// Uses HashMap (JS Map) for O(1) stock lookups
// Tracks: stock levels, alerts, reservations
// ============================================================

const InventoryModule = (() => {
  const STORE_KEY = 'scs_inventory';
  const API_BASE_URL = 'http://localhost:3001/api';

  // Thresholds
  const LOW_STOCK_THRESHOLD = 10;
  const OVERSTOCK_THRESHOLD = 200;
  const REORDER_POINT = 15;

  async function _remoteFetch(path, options = {}) {
    try {
      const res = await fetch(`${API_BASE_URL}${path}`, options);
      if (!res.ok) {
        throw new Error(`API ${path} failed: ${res.status}`);
      }
      return await res.json();
    } catch (err) {
      console.warn('InventoryModule API request failed', err);
      return null;
    }
  }

  async function _syncProduct(productId, data) {
    if (!productId || !data) return;
    await _remoteFetch(`/inventory/${productId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        quantity: data.quantity,
        reserved: data.reserved,
        max_capacity: data.maxCapacity,
        reorder_point: data.reorderPoint
      })
    });
  }

  async function loadFromServer() {
    const rows = await _remoteFetch('/inventory');
    if (!Array.isArray(rows) || rows.length === 0) {
      return getAll();
    }

    const invMap = new Map();
    rows.forEach(row => {
      invMap.set(row.product_id, {
        name: row.product_name || row.name || `Product ${row.product_id}`,
        quantity: row.quantity,
        reserved: row.reserved || 0,
        maxCapacity: row.max_capacity || 0,
        reorderPoint: row.reorder_point || 0,
        unitPrice: row.unit_price || 0,
        category: row.product_category || row.category || 'General',
        warehouse: row.warehouse || 'WH-1',
        lastUpdated: row.last_updated || new Date().toISOString()
      });
    });

    _save(invMap);
    return getAll();
  }

  function _save(inv) {
    localStorage.setItem(STORE_KEY, JSON.stringify(Object.fromEntries(inv)));
  }

  function _load() {
    const raw = localStorage.getItem(STORE_KEY);
    return raw ? new Map(Object.entries(JSON.parse(raw))) : new Map();
  }

  function getAll() {
    return Array.from(_load().entries()).map(([id, data]) => ({ id, ...data }));
  }

  function getById(productId) {
    const inv = _load();
    return inv.has(productId) ? { id: productId, ...inv.get(productId) } : null;
  }

  function setStock(productId, data) {
    const inv = _load();
    const existing = inv.get(productId) || {};
    inv.set(productId, {
      ...existing,
      ...data,
      lastUpdated: new Date().toISOString()
    });
    _save(inv);
    const result = { id: productId, ...inv.get(productId) };
    _syncProduct(productId, result);
    return result;
  }

  function adjustStock(productId, delta) {
    const inv = _load();
    if (!inv.has(productId)) return null;
    const item = inv.get(productId);
    const newQty = Math.max(0, item.quantity + delta);
    item.quantity = newQty;
    item.lastUpdated = new Date().toISOString();
    inv.set(productId, item);
    _save(inv);
    _syncProduct(productId, item);
    return getAlertStatus(productId, newQty, item.maxCapacity || OVERSTOCK_THRESHOLD);
  }

  function reserveStock(productId, qty) {
    const inv = _load();
    if (!inv.has(productId)) return false;
    const item = inv.get(productId);
    const available = item.quantity - (item.reserved || 0);
    if (available < qty) return false;
    item.reserved = (item.reserved || 0) + qty;
    item.lastUpdated = new Date().toISOString();
    inv.set(productId, item);
    _save(inv);
    _syncProduct(productId, item);
    return true;
  }

  function releaseReservation(productId, qty) {
    const inv = _load();
    if (!inv.has(productId)) return;
    const item = inv.get(productId);
    item.reserved = Math.max(0, (item.reserved || 0) - qty);
    item.quantity = Math.max(0, item.quantity - qty);
    item.lastUpdated = new Date().toISOString();
    inv.set(productId, item);
    _save(inv);
    _syncProduct(productId, item);
  }

  function getAlertStatus(productId, qty, maxCap) {
    const item = getById(productId);
    if (!item) return null;
    const q = qty !== undefined ? qty : item.quantity;
    const max = maxCap || item.maxCapacity || OVERSTOCK_THRESHOLD;

    if (q === 0) return { type: 'OUT_OF_STOCK', message: `${item.name} is OUT OF STOCK!`, severity: 'critical' };
    if (q <= LOW_STOCK_THRESHOLD) return { type: 'LOW_STOCK', message: `${item.name}: Only ${q} units left`, severity: 'warning' };
    if (q >= max) return { type: 'OVERSTOCK', message: `${item.name}: Overstocked (${q} units)`, severity: 'info' };
    if (q <= REORDER_POINT) return { type: 'REORDER', message: `${item.name}: Reorder point reached (${q})`, severity: 'warning' };
    return { type: 'OK', message: `${item.name}: Stock OK (${q})`, severity: 'ok' };
  }

  function getAllAlerts() {
    return getAll()
      .map(item => getAlertStatus(item.id, item.quantity, item.maxCapacity))
      .filter(a => a && a.type !== 'OK');
  }

  function getLowStockItems() {
    return getAll().filter(i => i.quantity <= LOW_STOCK_THRESHOLD);
  }

  function getOverstockItems() {
    return getAll().filter(i => i.quantity >= (i.maxCapacity || OVERSTOCK_THRESHOLD));
  }

  function restock(productId, qty) {
    return adjustStock(productId, qty);
  }

  // Summary stats
  function getStats() {
    const all = getAll();
    return {
      total: all.length,
      totalValue: all.reduce((s, i) => s + i.quantity * (i.unitPrice || 0), 0),
      lowStock: all.filter(i => i.quantity <= LOW_STOCK_THRESHOLD).length,
      outOfStock: all.filter(i => i.quantity === 0).length,
      overstock: all.filter(i => i.quantity >= (i.maxCapacity || OVERSTOCK_THRESHOLD)).length
    };
  }

  // Seed sample inventory
  function seed() {
    if (_load().size > 0) return;
    const products = [
      { id: 'P001', name: 'Electronics Package', quantity: 45, maxCapacity: 100, unitPrice: 500, category: 'Electronics', warehouse: 'WH-1' },
      { id: 'P002', name: 'Furniture Set', quantity: 8, maxCapacity: 50, unitPrice: 1200, category: 'Furniture', warehouse: 'WH-1' },
      { id: 'P003', name: 'Grocery Bundle', quantity: 210, maxCapacity: 200, unitPrice: 150, category: 'Grocery', warehouse: 'WH-2' },
      { id: 'P004', name: 'Medical Supplies', quantity: 3, maxCapacity: 80, unitPrice: 800, category: 'Medical', warehouse: 'WH-2' },
      { id: 'P005', name: 'Sports Equipment', quantity: 30, maxCapacity: 60, unitPrice: 300, category: 'Sports', warehouse: 'WH-1' },
      { id: 'P006', name: 'Clothing Pack', quantity: 0, maxCapacity: 150, unitPrice: 200, category: 'Apparel', warehouse: 'WH-3' },
      { id: 'P007', name: 'Home Appliances', quantity: 12, maxCapacity: 40, unitPrice: 600, category: 'Appliances', warehouse: 'WH-1' },
    ];
    products.forEach(p => setStock(p.id, p));
  }

  return {
    getAll, getById, setStock, adjustStock, reserveStock,
    releaseReservation, getAlertStatus, getAllAlerts,
    getLowStockItems, getOverstockItems, restock, getStats, loadFromServer, seed,
    THRESHOLDS: { LOW_STOCK_THRESHOLD, OVERSTOCK_THRESHOLD, REORDER_POINT }
  };
})();
