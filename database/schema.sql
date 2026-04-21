-- ============================================================
-- SMART SUPPLY CHAIN OPTIMIZATION SYSTEM
-- MySQL Database Schema
-- ============================================================


CREATE DATABASE IF NOT EXISTS supply_chain_db;
USE supply_chain_db;

-- ============================================================
-- TABLE: products
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
  id           VARCHAR(20)  PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  category     VARCHAR(50),
  unit_price   DECIMAL(10,2) DEFAULT 0,
  weight_kg    DECIMAL(8,2)  DEFAULT 0,
  volume_m3    DECIMAL(8,3)  DEFAULT 0,
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: inventory
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  product_id      VARCHAR(20)  NOT NULL,
  warehouse_id    VARCHAR(20)  NOT NULL,
  quantity        INT          DEFAULT 0,
  reserved        INT          DEFAULT 0,
  max_capacity    INT          DEFAULT 200,
  reorder_point   INT          DEFAULT 15,
  last_updated    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: customers
-- ============================================================
CREATE TABLE IF NOT EXISTS customers (
  id           VARCHAR(20)  PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  email        VARCHAR(100),
  phone        VARCHAR(20),
  address      TEXT,
  zone_node    VARCHAR(10),
  x_coord      FLOAT DEFAULT 0,
  y_coord      FLOAT DEFAULT 0,
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: orders
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
  id                VARCHAR(20)  PRIMARY KEY,
  customer_id       VARCHAR(20)  NOT NULL,
  product_id        VARCHAR(20)  NOT NULL,
  quantity          INT          DEFAULT 1,
  total_weight      DECIMAL(8,2),
  total_volume      DECIMAL(8,3),
  total_value       DECIMAL(10,2),
  priority          ENUM('HIGH','MEDIUM','LOW') DEFAULT 'MEDIUM',
  status            ENUM('PENDING','SCHEDULED','IN_TRANSIT','DELIVERED','FAILED','RETURNED') DEFAULT 'PENDING',
  destination_node  VARCHAR(10),
  deadline          TIMESTAMP,
  assigned_vehicle  VARCHAR(20),
  assigned_agent    VARCHAR(20),
  delivery_cost     DECIMAL(10,2),
  fail_reason       TEXT,
  notes             TEXT,
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (customer_id) REFERENCES customers(id),
  FOREIGN KEY (product_id)  REFERENCES products(id)
);

-- ============================================================
-- TABLE: vehicles
-- ============================================================
CREATE TABLE IF NOT EXISTS vehicles (
  id               VARCHAR(20) PRIMARY KEY,
  name             VARCHAR(80) NOT NULL,
  type             ENUM('truck','van','bike','drone') DEFAULT 'van',
  weight_capacity  DECIMAL(8,2) DEFAULT 500,
  volume_capacity  DECIMAL(8,3) DEFAULT 250,
  fuel_level       DECIMAL(5,2) DEFAULT 100,
  available        BOOLEAN DEFAULT TRUE,
  current_node     VARCHAR(10),
  assigned_orders  JSON,
  total_deliveries INT DEFAULT 0,
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: delivery_agents
-- ============================================================
CREATE TABLE IF NOT EXISTS delivery_agents (
  id               VARCHAR(20) PRIMARY KEY,
  name             VARCHAR(100) NOT NULL,
  phone            VARCHAR(20),
  x_coord          FLOAT DEFAULT 0,
  y_coord          FLOAT DEFAULT 0,
  available        BOOLEAN DEFAULT TRUE,
  rating           DECIMAL(3,2) DEFAULT 4.5,
  total_deliveries INT DEFAULT 0,
  current_order    VARCHAR(20),
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: routes
-- ============================================================
CREATE TABLE IF NOT EXISTS routes (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  order_id       VARCHAR(20)  NOT NULL,
  source_node    VARCHAR(10),
  dest_node      VARCHAR(10),
  path           JSON,          -- e.g. ["W1","H1","N3"]
  algorithm      VARCHAR(20) DEFAULT 'dijkstra',
  base_cost      DECIMAL(8,2),
  delay_penalty  DECIMAL(8,2) DEFAULT 0,
  total_cost     DECIMAL(8,2),
  distance       DECIMAL(8,2),
  delay_applied  BOOLEAN DEFAULT FALSE,
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: returns
-- ============================================================
CREATE TABLE IF NOT EXISTS returns (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  order_id       VARCHAR(20)  NOT NULL,
  customer_id    VARCHAR(20),
  reason         TEXT,
  return_route   JSON,
  return_cost    DECIMAL(8,2),
  status         ENUM('REQUESTED','IN_TRANSIT','RECEIVED','REFUNDED') DEFAULT 'REQUESTED',
  requested_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  completed_at   TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- ============================================================
-- TABLE: traffic_events (for simulation logging)
-- ============================================================
CREATE TABLE IF NOT EXISTS traffic_events (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  from_node      VARCHAR(10),
  to_node        VARCHAR(10),
  multiplier     DECIMAL(4,2) DEFAULT 1.0,
  active         BOOLEAN DEFAULT TRUE,
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- INDEXES for performance
-- ============================================================
CREATE INDEX idx_orders_status    ON orders(status);
CREATE INDEX idx_orders_priority  ON orders(priority);
CREATE INDEX idx_orders_deadline  ON orders(deadline);
CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_routes_order     ON routes(order_id);
