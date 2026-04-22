-- =============================================================================
-- SMART SUPPLY CHAIN - REAL-WORLD GROCERY DATABASE SCHEMA
-- =============================================================================
-- Quick Commerce / Blinkit-style Supply Chain System
-- =============================================================================

CREATE DATABASE IF NOT EXISTS smart_supply_chain;
USE smart_supply_chain;

-- =============================================================================
-- PRODUCT CATEGORIES
-- =============================================================================
CREATE TABLE IF NOT EXISTS categories (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,
    description     VARCHAR(200),
    parent_id       INT,
    min_expiry_hours INT DEFAULT 24,
    max_expiry_hours INT DEFAULT 8760,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES categories(id)
);

-- =============================================================================
-- PRODUCTS - Grocery items with expiry tracking
-- =============================================================================
CREATE TABLE IF NOT EXISTS products (
    id              VARCHAR(20) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    category_id     INT NOT NULL,
    sku            VARCHAR(30),
    barcode        VARCHAR(50),
    unit_price     DECIMAL(10,2) NOT NULL DEFAULT 0,
    cost_price     DECIMAL(10,2) DEFAULT 0,
    weight_kg      DECIMAL(8,3) DEFAULT 0,
    volume_m3      DECIMAL(8,5) DEFAULT 0,
    pack_size     INT DEFAULT 1,
    min_stock     INT DEFAULT 10,
    max_stock     INT DEFAULT 1000,
    is_active     BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- =============================================================================
-- WAREHOUSES / STORES
-- =============================================================================
CREATE TABLE IF NOT EXISTS warehouses (
    id              VARCHAR(20) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    type            ENUM('DARK_STORE','MAIN_WAREHOUSE','HYUB') DEFAULT 'DARK_STORE',
    address         VARCHAR(255),
    city            VARCHAR(50),
    zone            VARCHAR(20),
    x_coord         DECIMAL(10,6),
    y_coord         DECIMAL(10,6),
    radius_km       DECIMAL(5,2) DEFAULT 5,
    is_active      BOOLEAN DEFAULT TRUE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- INVENTORY BATCHES - Track each batch with expiry
-- =============================================================================
CREATE TABLE IF NOT EXISTS inventory_batches (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    product_id      VARCHAR(20) NOT NULL,
    warehouse_id   VARCHAR(20) NOT NULL,
    batch_number  VARCHAR(50) NOT NULL,
    quantity     INT NOT NULL DEFAULT 0,
    reserved_qty INT DEFAULT 0,
    available_qty INT GENERATED ALWAYS AS (quantity - reserved_qty) STORED,
    unit_cost    DECIMAL(10,2) DEFAULT 0,
    mfg_date     DATE NOT NULL,
    expiry_date  DATE NOT NULL,
    received_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status       ENUM('AVAILABLE','RESERVED','SOLD','EXPIRED','DAMAGED') DEFAULT 'AVAILABLE',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- =============================================================================
-- REPLENISHMENT RULES - Auto reorder configuration
-- =============================================================================
CREATE TABLE IF NOT EXISTS reorder_rules (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    product_id      VARCHAR(20) NOT NULL,
    warehouse_id   VARCHAR(20) NOT NULL,
    reorder_point INT NOT NULL DEFAULT 15,
    reorder_qty   INT NOT NULL DEFAULT 50,
    lead_time_hours INT NOT NULL DEFAULT 24,
    safety_days   INT DEFAULT 1,
    is_active    BOOLEAN DEFAULT TRUE,
    last_triggered TIMESTAMP,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- =============================================================================
-- SUBSTITUTION MAPPING - Product alternatives
-- =============================================================================
CREATE TABLE IF NOT EXISTS substitution_mapping (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    product_id      VARCHAR(20) NOT NULL,
    substitute_id  VARCHAR(20) NOT NULL,
    rank         INT DEFAULT 1,
    similarity   DECIMAL(4,3) DEFAULT 1.0,
    price_diff   DECIMAL(10,2) DEFAULT 0,
    is_active    BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (substitute_id) REFERENCES products(id)
);

-- =============================================================================
-- CUSTOMERS
-- =============================================================================
CREATE TABLE IF NOT EXISTS customers (
    id              VARCHAR(20) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(100),
    address         VARCHAR(255),
    city            VARCHAR(50),
    x_coord        DECIMAL(10,6),
    y_coord        DECIMAL(10,6),
    registration_type ENUM('APP','WEB','ADMIN') DEFAULT 'APP',
    is_active     BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- ORDERS
-- =============================================================================
CREATE TABLE IF NOT EXISTS orders (
    id              VARCHAR(20) PRIMARY KEY,
    customer_id     VARCHAR(20) NOT NULL,
    order_type     ENUM('DELIVERY','PICKUP') DEFAULT 'DELIVERY',
    total_amount   DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount      DECIMAL(10,2) DEFAULT 0,
    status        ENUM('PENDING','CONFIRMED','PICKING','PACKING','OUT_FOR_DELIVERY','DELIVERED','CANCELLED','RETURNED') DEFAULT 'PENDING',
    priority      ENUM('HIGH','MEDIUM','LOW') DEFAULT 'MEDIUM',
    slot_time     TIMESTAMP,
    sla_deadline  TIMESTAMP,
    sla_hours     DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE WHEN sla_deadline IS NOT NULL 
        THEN TIMESTAMPDIFF(HOUR, NOW(), sla_deadline) 
        ELSE NULL END
    ) STORED,
    sla_status    ENUM('ON_TRACK','AT_RISK','DELAYED','BREACHED') STORED,
    sla_score     INT GENERATED ALWAYS AS (
        CASE 
            WHEN sla_deadline IS NULL THEN 100
            WHEN TIMESTAMPDIFF(HOUR, NOW(), sla_deadline) < 0 THEN 0
            WHEN TIMESTAMPDIFF(HOUR, NOW(), sla_deadline) < 1 THEN 10
            WHEN TIMESTAMPDIFF(HOUR, NOW(), sla_deadline) < 2 THEN 30
            WHEN TIMESTAMPDIFF(HOUR, NOW(), sla_deadline) < 4 THEN 50
            ELSE 100
        END
    ) STORED,
    assigned_rider VARCHAR(20),
    assigned_vehicle VARCHAR(20),
    delivery_x    DECIMAL(10,6),
    delivery_y   DECIMAL(10,6),
    distance_km  DECIMAL(8,2),
    delivery_notes TEXT,
    cancelled_reason TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (assigned_rider) REFERENCES delivery_agents(id)
);

-- =============================================================================
-- ORDER ITEMS
-- =============================================================================
CREATE TABLE IF NOT EXISTS order_items (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    order_id       VARCHAR(20) NOT NULL,
    product_id     VARCHAR(20) NOT NULL,
    batch_id      INT,
    quantity     INT NOT NULL DEFAULT 1,
    unit_price    DECIMAL(10,2) NOT NULL,
    total_price   DECIMAL(10,2),
    substitute_id VARCHAR(20),
    is_substituted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (batch_id) REFERENCES inventory_batches(id)
);

-- =============================================================================
-- DELIVERY AGENTS
-- =============================================================================
CREATE TABLE IF NOT EXISTS delivery_agents (
    id              VARCHAR(20) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    warehouse_id    VARCHAR(20) NOT NULL,
    current_x      DECIMAL(10,6),
    current_y     DECIMAL(10,6),
    status        ENUM('OFFLINE','AVAILABLE','BUSY','ON_DELIVERY') DEFAULT 'OFFLINE',
    max_deliveries_per_day INT DEFAULT 20,
    vehicle_type  ENUM('BIKE','SCOOTY','CAR') DEFAULT 'SCOOTY',
    rating        DECIMAL(3,2) DEFAULT 4.5,
    total_orders  INT DEFAULT 0,
    today_orders INT DEFAULT 0,
    is_active    BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- =============================================================================
-- VEHICLES
-- =============================================================================
CREATE TABLE IF NOT EXISTS vehicles (
    id              VARCHAR(20) PRIMARY KEY,
    name            VARCHAR(80) NOT NULL,
    type            ENUM('BIKE','SCOOTY','CAR','VAN') DEFAULT 'SCOOTY',
    capacity_kg    DECIMAL(8,2) DEFAULT 50,
    capacity_qty   INT DEFAULT 20,
    fuel_level    DECIMAL(5,2) DEFAULT 100,
    is_active    BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- NETWORK NODES - For routing
-- =============================================================================
CREATE TABLE IF NOT EXISTS network_nodes (
    id              VARCHAR(10) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    type            ENUM('WAREHOUSE','HUB','ZONE','CUSTOMER') DEFAULT 'ZONE',
    x_coord        DECIMAL(10,6) NOT NULL,
    y_coord       DECIMAL(10,6) NOT NULL,
    warehouse_id  VARCHAR(20),
    is_active    BOOLEAN DEFAULT TRUE
);

-- =============================================================================
-- NETWORK EDGES - Connections
-- =============================================================================
CREATE TABLE IF NOT EXISTS network_edges (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    from_node       VARCHAR(10) NOT NULL,
    to_node         VARCHAR(10) NOT NULL,
    distance_km    DECIMAL(8,2) NOT NULL,
    travel_time_mins INT DEFAULT 15,
    traffic_multiplier DECIMAL(4,2) DEFAULT 1.0,
    is_active    BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (from_node) REFERENCES network_nodes(id),
    FOREIGN KEY (to_node) REFERENCES network_nodes(id)
);

-- =============================================================================
-- DEMAND HISTORY - For prediction
-- =============================================================================
CREATE TABLE IF NOT EXISTS demand_history (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    product_id      VARCHAR(20) NOT NULL,
    warehouse_id   VARCHAR(20) NOT NULL,
    date          DATE NOT NULL,
    hour          INT,
    quantity_sold INT DEFAULT 0,
    units_returned INT DEFAULT 0,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- =============================================================================
-- INDEXES
-- =============================================================================
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_inventory_product ON inventory_batches(product_id, warehouse_id);
CREATE INDEX idx_inventory_expiry ON inventory_batches(expiry_date, status);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_sla ON orders(sla_deadline);
CREATE INDEX idx_demand_product_date ON demand_history(product_id, date);
CREATE INDEX idx_substitution_product ON substitution_mapping(product_id, rank);