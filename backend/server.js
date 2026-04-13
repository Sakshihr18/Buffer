const express = require('express');
const mysql = require('mysql2/promise');
const cors = require('cors');
const path = require('path');

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, '..', 'frontend')));

const DB_HOST = process.env.DB_HOST || 'localhost';
const DB_USER = process.env.DB_USER || 'root';
const DB_PASSWORD = process.env.DB_PASSWORD || '';
const DB_NAME = process.env.DB_NAME || 'supply_chain_db';
const PORT = parseInt(process.env.PORT, 10) || 3001;

const pool = mysql.createPool({
  host: DB_HOST,
  user: DB_USER,
  password: DB_PASSWORD,
  database: DB_NAME,
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0
});

function normalizeOrderRow(row) {
  return {
    id: row.id,
    customerId: row.customer_id,
    productId: row.product_id,
    quantity: row.quantity,
    weight: row.total_weight,
    volume: row.total_volume,
    value: row.total_value,
    priority: row.priority,
    status: row.status,
    destinationNode: row.destination_node,
    deadline: row.deadline,
    assignedVehicle: row.assigned_vehicle,
    assignedAgent: row.assigned_agent,
    deliveryCost: row.delivery_cost,
    failReason: row.fail_reason,
    notes: row.notes,
    createdAt: row.created_at,
    updatedAt: row.updated_at
  };
}

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.get('/api/orders', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT * FROM orders ORDER BY created_at DESC');
    res.json(rows.map(normalizeOrderRow));
  } catch (error) {
    console.error('Orders fetch failed:', error);
    res.status(500).json({ error: 'Unable to fetch orders' });
  }
});

app.post('/api/orders', async (req, res) => {
  const order = req.body;
  try {
    await pool.execute(
      `INSERT INTO orders
        (id, customer_id, product_id, quantity, total_weight, total_volume, total_value,
         priority, status, destination_node, deadline, assigned_vehicle, assigned_agent,
         delivery_cost, fail_reason, notes, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
      ON DUPLICATE KEY UPDATE
        customer_id = VALUES(customer_id),
        product_id = VALUES(product_id),
        quantity = VALUES(quantity),
        total_weight = VALUES(total_weight),
        total_volume = VALUES(total_volume),
        total_value = VALUES(total_value),
        priority = VALUES(priority),
        status = VALUES(status),
        destination_node = VALUES(destination_node),
        deadline = VALUES(deadline),
        assigned_vehicle = VALUES(assigned_vehicle),
        assigned_agent = VALUES(assigned_agent),
        delivery_cost = VALUES(delivery_cost),
        fail_reason = VALUES(fail_reason),
        notes = VALUES(notes),
        updated_at = NOW()`,
      [
        order.id,
        order.customerId,
        order.productId,
        order.quantity,
        order.weight,
        order.volume,
        order.value,
        order.priority,
        order.status,
        order.destinationNode,
        order.deadline,
        order.assignedVehicle,
        order.assignedAgent,
        order.deliveryCost || order.cost || 0,
        order.failReason || null,
        order.notes || null
      ]
    );
    res.status(201).json({ ok: true });
  } catch (error) {
    console.error('Orders insert failed:', error);
    res.status(500).json({ error: 'Unable to save order' });
  }
});

app.put('/api/orders/:id', async (req, res) => {
  const order = req.body;
  const id = req.params.id;
  const fields = [];
  const values = [];

  const allowed = [
    'customer_id', 'product_id', 'quantity', 'total_weight', 'total_volume', 'total_value',
    'priority', 'status', 'destination_node', 'deadline', 'assigned_vehicle', 'assigned_agent',
    'delivery_cost', 'fail_reason', 'notes'
  ];

  Object.entries(order).forEach(([key, value]) => {
    if (allowed.includes(key)) {
      fields.push(`${key} = ?`);
      values.push(value);
    }
  });

  if (fields.length === 0) {
    return res.status(400).json({ error: 'No valid fields to update' });
  }

  values.push(id);

  try {
    await pool.execute(
      `UPDATE orders SET ${fields.join(', ')}, updated_at = NOW() WHERE id = ?`,
      values
    );
    res.json({ ok: true });
  } catch (error) {
    console.error('Orders update failed:', error);
    res.status(500).json({ error: 'Unable to update order' });
  }
});

app.delete('/api/orders/:id', async (req, res) => {
  try {
    await pool.execute('DELETE FROM orders WHERE id = ?', [req.params.id]);
    res.json({ ok: true });
  } catch (error) {
    console.error('Orders delete failed:', error);
    res.status(500).json({ error: 'Unable to delete order' });
  }
});

app.get('/api/inventory', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT i.*, p.name AS product_name, p.category AS product_category, p.unit_price
       FROM inventory i
       LEFT JOIN products p ON p.id = i.product_id`
    );
    res.json(rows);
  } catch (error) {
    console.error('Inventory fetch failed:', error);
    res.status(500).json({ error: 'Unable to fetch inventory' });
  }
});

app.put('/api/inventory/:productId', async (req, res) => {
  const data = req.body;
  const productId = req.params.productId;
  const fields = [];
  const values = [];

  ['quantity', 'reserved', 'max_capacity', 'reorder_point'].forEach(key => {
    if (data[key] !== undefined) {
      fields.push(`${key} = ?`);
      values.push(data[key]);
    }
  });

  if (fields.length === 0) {
    return res.status(400).json({ error: 'No valid fields to update' });
  }

  fields.push('last_updated = NOW()');
  values.push(productId);

  try {
    await pool.execute(
      `UPDATE inventory SET ${fields.join(', ')} WHERE product_id = ?`,
      values
    );
    res.json({ ok: true });
  } catch (error) {
    console.error('Inventory update failed:', error);
    res.status(500).json({ error: 'Unable to update inventory' });
  }
});

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, '..', 'frontend', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Backend API listening on http://localhost:${PORT}`);
});
