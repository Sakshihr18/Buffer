-- =============================================================================
-- SMART SUPPLY CHAIN - REALISTIC SAMPLE DATA
-- =============================================================================

USE smart_supply_chain;

-- =============================================================================
-- CATEGORIES
-- =============================================================================
INSERT INTO categories (id, name, description, min_expiry_hours, max_expiry_hours) VALUES
(1, 'Dairy', 'Milk, Cheese, Yogurt', 24, 168),
(2, 'Bakery', 'Bread, Rolls, Cakes', 24, 120),
(3, 'Fruits', 'Fresh Fruits', 48, 240),
(4, 'Vegetables', 'Fresh Vegetables', 24, 168),
(5, 'Packaged', 'Processed Food', 720, 8760),
(6, 'Beverages', 'Drinks, Water', 168, 4320),
(7, 'Snacks', 'Chips, Biscuits', 720, 4320),
(8, 'Household', 'Cleaning, Essentials', 8760, 26280);

-- =============================================================================
-- PRODUCTS - Real grocery items
-- =============================================================================
INSERT INTO products (id, name, category_id, sku, barcode, unit_price, cost_price, weight_kg, volume_m3, pack_size, min_stock, max_stock) VALUES
-- Dairy (short expiry)
('PRD001', 'Amul Full Cream Milk 1L', 1, 'AML001', '8901234560010', 65.00, 45.00, 1.05, 0.001, 1, 50, 500),
('PRD002', 'Amul Toned Milk 1L', 1, 'AML002', '8901234560020', 55.00, 38.00, 1.05, 0.001, 1, 50, 500),
('PRD003', 'Amul Curd 500g', 1, 'AML003', '8901234560030', 45.00, 30.00, 0.52, 0.0005, 1, 30, 200),
('PRD004', 'Amul Cheese Slices 200g', 1, 'AML004', '8901234560040', 120.00, 80.00, 0.22, 0.0002, 1, 20, 100),
('PRD005', 'Mother Dairy Paneer 500g', 1, 'MDP005', '8901234560050', 130.00, 90.00, 0.52, 0.0005, 1, 25, 150),
-- Bakery (short expiry)
('PRD006', 'Bread White Large', 2, 'BRD006', '8901234560060', 45.00, 25.00, 0.40, 0.002, 1, 40, 200),
('PRD007', 'Bread Brown', 2, 'BRD007', '8901234560070', 50.00, 28.00, 0.40, 0.002, 1, 30, 150),
('PRD008', 'Multigrain Bread', 2, 'BRD008', '8901234560080', 60.00, 35.00, 0.42, 0.002, 1, 25, 120),
('PRD009', 'Bread Toast', 2, 'BRD009', '8901234560090', 55.00, 30.00, 0.35, 0.0015, 1, 30, 150),
('PRD010', 'Croissants 4pcs', 2, 'CRO010', '8901234560100', 150.00, 80.00, 0.25, 0.001, 1, 15, 50),
-- Fruits (medium expiry)
('PRD011', 'Apples 1kg', 3, 'APL011', '8901234560110', 180.00, 100.00, 1.0, 0.003, 1, 20, 100),
('PRD012', 'Banana 1 dozen', 3, 'BAN012', '8901234560120', 60.00, 30.00, 1.2, 0.004, 1, 30, 150),
('PRD013', 'Oranges 1kg', 3, 'ORG013', '8901234560130', 200.00, 120.00, 1.0, 0.003, 1, 20, 80),
('PRD014', 'Mangoes 1kg', 3, 'MNG014', '8901234560140', 250.00, 150.00, 1.0, 0.003, 1, 15, 60),
('PRD015', 'Grapes 500g', 3, 'GRP015', '8901234560150', 120.00, 70.00, 0.5, 0.0015, 1, 20, 80),
-- Vegetables (short expiry)
('PRD016', 'Tomato 1kg', 4, 'TMT016', '8901234560160', 45.00, 20.00, 1.0, 0.003, 1, 40, 200),
('PRD017', 'Potato 5kg', 4, 'POT017', '8901234560170', 150.00, 80.00, 5.0, 0.015, 1, 30, 100),
('PRD018', 'Onion 5kg', 4, 'ONI018', '8901234560180', 180.00, 90.00, 5.0, 0.015, 1, 30, 100),
('PRD019', 'Carrot 500g', 4, 'CRT019', '8901234560190', 35.00, 18.00, 0.5, 0.001, 1, 25, 100),
('PRD020', 'Spinach 250g', 4, 'SPN020', '8901234560200', 30.00, 15.00, 0.25, 0.0005, 1, 30, 120),
-- Packaged (long expiry)
('PRD021', 'Maggi Noodles 12pk', 5, 'MAG021', '8901234560210', 180.00, 100.00, 0.72, 0.004, 12, 50, 200),
('PRD022', 'Parle-G 800g', 5, 'PRL022', '8901234560220', 50.00, 28.00, 0.82, 0.003, 1, 60, 300),
('PRD023', 'Britannia Cake 700g', 5, 'BRI023', '8901234560230', 180.00, 100.00, 0.72, 0.003, 1, 40, 150),
('PRD024', 'Kissan Ketchup 1kg', 5, 'KIS024', '8901234560240', 220.00, 120.00, 1.0, 0.004, 1, 30, 100),
('PRD025', 'Sunflower Oil 1L', 5, 'SUN025', '8901234560250', 200.00, 140.00, 1.0, 0.001, 1, 40, 150),
-- Beverages
('PRD026', 'Coca-Cola 600ml', 6, 'COC026', '8901234560260', 60.00, 30.00, 0.65, 0.001, 1, 50, 200),
('PRD027', 'Pepsi 600ml', 6, 'PEP027', '8901234560270', 60.00, 30.00, 0.65, 0.001, 1, 50, 200),
('PRD028', 'Water 1L', 6, 'WTR028', '8901234560280', 25.00, 10.00, 1.0, 0.001, 1, 100, 500),
('PRD029', 'Tropicana Juice 1L', 6, 'TRP029', '8901234560290', 180.00, 100.00, 1.0, 0.001, 1, 30, 100),
('PRD030', 'Mango Lassi 1L', 6, 'MLA030', '8901234560300', 80.00, 45.00, 1.0, 0.001, 1, 40, 150),
-- Snacks
('PRD031', 'Lays Chips Plain 150g', 7, 'LAY031', '8901234560310', 50.00, 25.00, 0.15, 0.0005, 1, 60, 250),
('PRD032', 'Doritos 150g', 7, 'DOR032', '8901234560320', 80.00, 40.00, 0.15, 0.0005, 1, 40, 150),
('PRD033', 'Kurkure 150g', 7, 'KUR033', '8901234560330', 50.00, 25.00, 0.15, 0.0005, 1, 60, 250),
('PRD034', 'Bingo Chips 150g', 7, 'BIN034', '8901234560340', 50.00, 25.00, 0.15, 0.0005, 1, 60, 250),
('PRD035', 'Sunfeast Oreo 300g', 7, 'SUN035', '8901234560350', 100.00, 50.00, 0.32, 0.001, 1, 40, 150);

-- =============================================================================
-- WAREHOUSES
-- =============================================================================
INSERT INTO warehouses (id, name, type, address, city, zone, x_coord, y_coord, radius_km) VALUES
('WH001', 'Whitefield Dark Store', 'DARK_STORE', 'Whitefield Main Road', 'Bangalore', 'East', 12.9698, 77.7499, 5),
('WH002', 'Koramangala Hub', 'DARK_STORE', 'Koramangala 6th Block', 'Bangalore', 'South', 12.9352, 77.6245, 5),
('WH003', 'Indiranagar Store', 'DARK_STORE', '100 Feet Road', 'Bangalore', 'East', 12.9754, 77.6401, 5),
('WH004', 'JP Nagar Dark Store', 'DARK_STORE', 'JP Nagar 6th Phase', 'Bangalore', 'South', 12.9107, 77.5853, 5),
('WH005', 'HSR Layout Hub', 'DARK_STORE', 'HSR Sector 1', 'Bangalore', 'South', 12.9121, 77.6447, 5),
('WH006', 'MG Road Warehouse', 'MAIN_WAREHOUSE', 'MG Road', 'Bangalore', 'Central', 12.9753, 77.6061, 10);

-- =============================================================================
-- NETWORK NODES
-- =============================================================================
INSERT INTO network_nodes (id, name, type, x_coord, y_coord, warehouse_id) VALUES
('N001', 'Whitefield', 'WAREHOUSE', 12.9698, 77.7499, 'WH001'),
('N002', 'Koramangala', 'ZONE', 12.9352, 77.6245, 'WH002'),
('N003', 'Indiranagar', 'ZONE', 12.9754, 77.6401, 'WH003'),
('N004', 'JP Nagar', 'ZONE', 12.9107, 77.5853, 'WH004'),
('N005', 'HSR Layout', 'ZONE', 12.9121, 77.6447, 'WH005'),
('N006', 'MG Road', 'HUB', 12.9753, 77.6061, 'WH006'),
('N007', 'Marathahalli', 'ZONE', 12.9591, 77.7011, 'WH001'),
('N008', 'Bellandur', 'ZONE', 12.9258, 77.6766, 'WH002'),
('N009', 'Electronic City', 'ZONE', 12.8456, 77.6603, 'WH004'),
('N010', 'Hebbal', 'ZONE', 13.0358, 77.5970, 'WH003');

-- =============================================================================
-- NETWORK EDGES
-- =============================================================================
INSERT INTO network_edges (from_node, to_node, distance_km, travel_time_mins, traffic_multiplier) VALUES
('N001', 'N002', 5.2, 18, 1.0),
('N001', 'N007', 3.1, 12, 1.2),
('N002', 'N003', 3.5, 14, 1.0),
('N002', 'N008', 4.2, 16, 1.1),
('N002', 'N005', 2.8, 10, 1.0),
('N003', 'N006', 4.0, 15, 1.3),
('N004', 'N005', 4.5, 18, 1.0),
('N004', 'N009', 8.2, 30, 1.0),
('N006', 'N003', 4.0, 15, 1.3),
('N006', 'N010', 6.5, 25, 1.0);

-- =============================================================================
-- CUSTOMERS
-- =============================================================================
INSERT INTO customers (id, name, phone, email, address, city, x_coord, y_coord) VALUES
('CUS001', 'Rahul Sharma', '9876543210', 'rahul@email.com', 'Flat 201, Whitefield', 'Bangalore', 12.9700, 77.7500),
('CUS002', 'Priya Patel', '9876543211', 'priya@email.com', '202 Koramangala', 'Bangalore', 12.9360, 77.6250),
('CUS003', 'Amit Kumar', '9876543212', 'amit@email.com', '303 Indiranagar', 'Bangalore', 12.9760, 77.6410),
('CUS004', 'Sneha Gupta', '9876543213', 'sneha@email.com', '401 JP Nagar', 'Bangalore', 12.9110, 77.5860),
('CUS005', 'Raj Malhotra', '9876543214', 'raj@email.com', '501 HSR Layout', 'Bangalore', 12.9130, 77.6450),
('CUS006', 'Anita Singh', '9876543215', 'anita@email.com', '602 Marathahalli', 'Bangalore', 12.9600, 77.7020),
('CUS007', 'Vikram Reddy', '9876543216', 'vikram@email.com', '703 Bellandur', 'Bangalore', 12.9260, 77.6770),
('CUS008', 'Meera Nair', '9876543217', 'meera@email.com', '804 Electronic City', 'Bangalore', 12.8460, 77.6610),
('CUS009', 'Deepak Chowdhury', '9876543218', 'deepak@email.com', '905 Hebbal', 'Bangalore', 13.0360, 77.5980),
('CUS010', 'Kavita Iyer', '9876543219', 'kavita@email.com', '1006 MG Road', 'Bangalore', 12.9760, 77.6070);

-- =============================================================================
-- DELIVERY AGENTS
-- =============================================================================
INSERT INTO delivery_agents (id, name, phone, warehouse_id, current_x, current_y, status, max_deliveries_per_day, vehicle_type, rating) VALUES
('RID001', 'Kumar Singh', '9876500001', 'WH001', 12.9700, 77.7500, 'AVAILABLE', 20, 'SCOOTY', 4.8),
('RID002', 'Suresh Patel', '9876500002', 'WH002', 12.9360, 77.6250, 'AVAILABLE', 20, 'SCOOTY', 4.7),
('RID003', 'Mohan Rao', '9876500003', 'WH003', 12.9760, 77.6410, 'AVAILABLE', 20, 'SCOOTY', 4.9),
('RID004', 'Ravi Kumar', '9876500004', 'WH004', 12.9110, 77.5860, 'AVAILABLE', 15, 'BIKE', 4.6),
('RID005', 'Prakash G', '9876500005', 'WH005', 12.9130, 77.6450, 'AVAILABLE', 20, 'SCOOTY', 4.5),
('RID006', 'Gopal Das', '9876500006', 'WH001', 12.9600, 77.7020, 'AVAILABLE', 20, 'SCOOTY', 4.8),
('RID007', 'Naveen K', '9876500007', 'WH002', 12.9260, 77.6770, 'AVAILABLE', 20, 'SCOOTY', 4.7),
('RID008', 'Arun Moses', '9876500008', 'WH003', 13.0360, 77.5980, 'OFFLINE', 15, 'BIKE', 4.4),
('RID009', 'John Prasad', '9876500009', 'WH004', 12.8460, 77.6610, 'AVAILABLE', 20, 'SCOOTY', 4.6),
('RID010', 'Venky R', '9876500010', 'WH005', 12.9760, 77.6070, 'OFFLINE', 20, 'SCOOTY', 4.5);

-- =============================================================================
-- VEHICLES
-- =============================================================================
INSERT INTO vehicles (id, name, type, capacity_kg, capacity_qty) VALUES
('VEH001', 'Scooty XL-1', 'SCOOTY', 50, 20),
('VEH002', 'Bike Premium', 'BIKE', 25, 10),
('VEH003', 'Car City', 'CAR', 200, 50),
('VEH004', 'Van Express', 'VAN', 500, 100);

-- =============================================================================
-- INVENTORY BATCHES - With realistic expiry
-- =============================================================================
-- Milk batches (expire in 2-3 days)
INSERT INTO inventory_batches (product_id, warehouse_id, batch_number, quantity, unit_cost, mfg_date, expiry_date) VALUES
('PRD001', 'WH001', 'AML2024042001', 200, 45.00, CURRENT_DATE - 1, CURRENT_DATE + 2),
('PRD001', 'WH001', 'AML2024042002', 150, 45.00, CURRENT_DATE, CURRENT_DATE + 3),
('PRD001', 'WH002', 'AML2024042003', 180, 45.00, CURRENT_DATE - 1, CURRENT_DATE + 2),
('PRD002', 'WH001', 'TND2024042001', 250, 38.00, CURRENT_DATE - 1, CURRENT_DATE + 2),
('PRD002', 'WH002', 'TND2024042002', 200, 38.00, CURRENT_DATE, CURRENT_DATE + 3),
('PRD003', 'WH001', 'CUR2024042001', 100, 30.00, CURRENT_DATE - 1, CURRENT_DATE + 1),
('PRD003', 'WH003', 'CUR2024042002', 80, 30.00, CURRENT_DATE, CURRENT_DATE + 2),
('PRD004', 'WH001', 'CHS2024042001', 50, 80.00, CURRENT_DATE - 5, CURRENT_DATE + 10),
('PRD004', 'WH002', 'CHS2024042002', 40, 80.00, CURRENT_DATE - 10, CURRENT_DATE + 5),
-- Bread batches (expire in 3-5 days)
('PRD006', 'WH001', 'BRD2024042201', 120, 25.00, CURRENT_DATE - 1, CURRENT_DATE + 4),
('PRD006', 'WH002', 'BRD2024042202', 100, 25.00, CURRENT_DATE, CURRENT_DATE + 5),
('PRD006', 'WH003', 'BRD2024042203', 80, 25.00, CURRENT_DATE - 2, CURRENT_DATE + 3),
('PRD007', 'WH001', 'BRB2024042001', 60, 28.00, CURRENT_DATE - 1, CURRENT_DATE + 4),
('PRD007', 'WH002', 'BRB2024042002', 50, 28.00, CURRENT_DATE, CURRENT_DATE + 5),
('PRD008', 'WH001', 'BRM2024042001', 40, 35.00, CURRENT_DATE, CURRENT_DATE + 5),
-- Fruits/Veg (expire in 3-7 days)
('PRD011', 'WH001', 'APL2024042201', 40, 100.00, CURRENT_DATE - 3, CURRENT_DATE + 7),
('PRD012', 'WH001', 'BAN2024042201', 60, 30.00, CURRENT_DATE - 2, CURRENT_DATE + 5),
('PRD012', 'WH002', 'BAN2024042202', 50, 30.00, CURRENT_DATE - 3, CURRENT_DATE + 4),
('PRD016', 'WH001', 'TOM2024042201', 80, 20.00, CURRENT_DATE - 1, CURRENT_DATE + 3),
('PRD016', 'WH002', 'TOM2024042202', 100, 20.00, CURRENT_DATE - 2, CURRENT_DATE + 2),
('PRD017', 'WH001', 'POT2024042001', 200, 80.00, CURRENT_DATE - 30, CURRENT_DATE + 35),
-- Packaged (long expiry)
('PRD021', 'WH001', 'MAG2024042001', 300, 100.00, CURRENT_DATE - 60, CURRENT_DATE + 305),
('PRD022', 'WH001', 'PRL2024042001', 400, 28.00, CURRENT_DATE - 30, CURRENT_DATE + 270),
('PRD023', 'WH001', 'BRI2024042001', 200, 100.00, CURRENT_DATE - 45, CURRENT_DATE + 250),
-- Beverages
('PRD026', 'WH001', 'COC2024042001', 250, 30.00, CURRENT_DATE - 90, CURRENT_DATE + 270),
('PRD028', 'WH001', 'WTR2024042001', 500, 10.00, CURRENT_DATE - 30, CURRENT_DATE + 330),
-- Snacks
('PRD031', 'WH001', 'LAY2024042001', 400, 25.00, CURRENT_DATE - 45, CURRENT_DATE + 320),
('PRD032', 'WH001', 'DOR2024042001', 200, 40.00, CURRENT_DATE - 60, CURRENT_DATE + 305);

-- =============================================================================
-- REORDER RULES
-- =============================================================================
INSERT INTO reorder_rules (product_id, warehouse_id, reorder_point, reorder_qty, lead_time_hours, safety_days) VALUES
('PRD001', 'WH001', 50, 200, 24, 1),
('PRD001', 'WH002', 40, 150, 24, 1),
('PRD002', 'WH001', 50, 200, 24, 1),
('PRD003', 'WH001', 30, 100, 24, 1),
('PRD006', 'WH001', 40, 150, 12, 1),
('PRD007', 'WH001', 30, 100, 12, 1),
('PRD011', 'WH001', 20, 50, 48, 2),
('PRD012', 'WH001', 25, 60, 48, 2),
('PRD016', 'WH001', 40, 150, 12, 1),
('PRD021', 'WH001', 50, 200, 72, 3);

-- =============================================================================
-- SUBSTITUTION MAPPINGS
-- =============================================================================
INSERT INTO substitution_mapping (product_id, substitute_id, rank, similarity, price_diff) VALUES
-- Milk substitutes
('PRD001', 'PRD002', 1, 0.95, -10),
('PRD002', 'PRD001', 1, 0.95, 10),
-- Bread substitutes
('PRD006', 'PRD007', 1, 0.90, 5),
('PRD006', 'PRD008', 2, 0.85, 15),
('PRD007', 'PRD006', 1, 0.90, -5),
('PRD007', 'PRD008', 2, 0.80, 10),
('PRD008', 'PRD006', 1, 0.85, -15),
('PRD008', 'PRD007', 2, 0.80, -10),
-- Fruit substitutes
('PRD011', 'PRD013', 1, 0.70, 20),
('PRD013', 'PRD011', 1, 0.70, -20),
-- Vegetable substitutes
('PRD016', 'PRD017', 1, 0.50, 105),
('PRD017', 'PRD018', 1, 0.60, 30),
-- Beverages
('PRD026', 'PRD027', 1, 0.98, 0),
('PRD027', 'PRD026', 1, 0.98, 0),
('PRD028', 'PRD030', 1, 0.60, 55);

-- =============================================================================
-- SAMPLE ORDERS WITH SLA
-- =============================================================================
INSERT INTO orders (id, customer_id, order_type, total_amount, status, priority, slot_time, sla_deadline, assigned_rider, delivery_x, delivery_y, distance_km) VALUES
('ORD001', 'CUS001', 'DELIVERY', 450, 'PENDING', 'HIGH', NOW() + INTERVAL '4' HOUR, NOW() + INTERVAL '2' HOUR, 'RID001', 12.9700, 77.7500, 1.5),
('ORD002', 'CUS002', 'DELIVERY', 280, 'CONFIRMED', 'MEDIUM', NOW() + INTERVAL '6' HOUR, NOW() + INTERVAL '4' HOUR, 'RID002', 12.9360, 77.6250, 2.0),
('ORD003', 'CUS003', 'DELIVERY', 1200, 'PICKING', 'HIGH', NOW() + INTERVAL '3' HOUR, NOW() + INTERVAL '1' HOUR, 'RID003', 12.9760, 77.6410, 1.8),
('ORD004', 'CUS004', 'DELIVERY', 850, 'PACKING', 'MEDIUM', NOW() + INTERVAL '5' HOUR, NOW() + INTERVAL '3' HOUR, 'RID004', 12.9110, 77.5860, 2.5),
('ORD005', 'CUS005', 'DELIVERY', 560, 'PENDING', 'LOW', NOW() + INTERVAL '8' HOUR, NOW() + INTERVAL '6' HOUR, NULL, 12.9130, 77.6450, 3.0);

-- =============================================================================
-- DEMAND HISTORY (Last 7 days)
-- =============================================================================
INSERT INTO demand_history (product_id, warehouse_id, date, hour, quantity_sold) VALUES
('PRD001', 'WH001', CURRENT_DATE - 1, 10, 45), ('PRD001', 'WH001', CURRENT_DATE - 1, 11, 52), ('PRD001', 'WH001', CURRENT_DATE - 1, 12, 78),
('PRD001', 'WH001', CURRENT_DATE - 1, 18, 95), ('PRD001', 'WH001', CURRENT_DATE, 10, 48), ('PRD001', 'WH001', CURRENT_DATE, 11, 55),
('PRD006', 'WH001', CURRENT_DATE - 1, 9, 35), ('PRD006', 'WH001', CURRENT_DATE - 1, 12, 65), ('PRD006', 'WH001', CURRENT_DATE, 9, 38),
('PRD011', 'WH001', CURRENT_DATE - 1, 15, 22), ('PRD011', 'WH001', CURRENT_DATE - 2, 15, 18), ('PRD011', 'WH001', CURRENT_DATE, 15, 25),
('PRD016', 'WH001', CURRENT_DATE - 1, 17, 42), ('PRD016', 'WH001', CURRENT_DATE - 2, 17, 38), ('PRD016', 'WH001', CURRENT_DATE, 17, 45);