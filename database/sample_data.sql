-- ============================================================
-- SAMPLE DATA — Supply Chain System
-- Run this AFTER schema.sql
-- ============================================================

USE supply_chain_db;

-- ── Products ──────────────────────────────────────────────
INSERT INTO products VALUES
('P001','Electronics Package','Electronics',500.00,2.00,0.003,NOW()),
('P002','Furniture Set','Furniture',1200.00,15.00,0.020,NOW()),
('P003','Grocery Bundle','Grocery',150.00,5.00,0.008,NOW()),
('P004','Medical Supplies','Medical',800.00,1.00,0.002,NOW()),
('P005','Sports Equipment','Sports',300.00,8.00,0.010,NOW()),
('P006','Clothing Pack','Apparel',200.00,3.00,0.005,NOW()),
('P007','Home Appliances','Appliances',600.00,12.00,0.015,NOW());

-- ── Inventory ─────────────────────────────────────────────
INSERT INTO inventory (product_id,warehouse_id,quantity,reserved,max_capacity,reorder_point) VALUES
('P001','WH-1',45,5,100,15),
('P002','WH-1',8,2,50,5),
('P003','WH-2',210,10,200,20),
('P004','WH-2',3,1,80,10),
('P005','WH-1',30,3,60,8),
('P006','WH-3',0,0,150,15),
('P007','WH-1',12,2,40,5);

-- ── Customers ─────────────────────────────────────────────
INSERT INTO customers VALUES
('CUST-100','Arjun Sharma','arjun@email.com','9876543210','12 MG Road, Pune','N3',200,150,NOW()),
('CUST-101','Priya Patel','priya@email.com','9876543211','45 FC Road, Pune','N5',350,280,NOW()),
('CUST-102','Rahul Verma','rahul@email.com','9876543212','7 Baner Road, Pune','N7',150,320,NOW()),
('CUST-103','Sneha Iyer','sneha@email.com','9876543213','88 Koregaon Park','N4',400,180,NOW()),
('CUST-104','Kiran Mehta','kiran@email.com','9876543214','3 Viman Nagar','N6',300,380,NOW());

-- ── Vehicles ──────────────────────────────────────────────
INSERT INTO vehicles VALUES
('V001','Truck Alpha','truck',1000,500,100,TRUE,'W1','[]',0,NOW()),
('V002','Van Beta','van',500,250,85,TRUE,'W1','[]',0,NOW()),
('V003','Bike Gamma','bike',50,30,60,FALSE,'H1','[]',0,NOW()),
('V004','Truck Delta','truck',800,400,95,TRUE,'W2','[]',0,NOW());

-- ── Delivery Agents ───────────────────────────────────────
INSERT INTO delivery_agents VALUES
('A001','Ravi Kumar','9111222333',120,180,TRUE,4.8,142,NULL,NOW()),
('A002','Sita Devi','9111222334',350,280,TRUE,4.6,98,NULL,NOW()),
('A003','Mohan Singh','9111222335',200,150,FALSE,4.9,201,NULL,NOW()),
('A004','Lakshmi Nair','9111222336',400,180,TRUE,4.7,77,NULL,NOW()),
('A005','Vijay Reddy','9111222337',300,380,TRUE,4.5,55,NULL,NOW());

-- ── Orders ────────────────────────────────────────────────
INSERT INTO orders VALUES
('ORD-1001','CUST-100','P001',2,4.00,0.006,1000,'HIGH','PENDING','N3',DATE_ADD(NOW(),INTERVAL 1 DAY),NULL,NULL,NULL,NULL,'Fragile - handle with care',NOW(),NOW()),
('ORD-1002','CUST-101','P002',1,15.00,0.020,1200,'MEDIUM','SCHEDULED','N5',DATE_ADD(NOW(),INTERVAL 2 DAY),'V001','A002',NULL,NULL,'',NOW(),NOW()),
('ORD-1003','CUST-102','P003',3,15.00,0.024,450,'LOW','IN_TRANSIT','N7',DATE_ADD(NOW(),INTERVAL 3 DAY),'V002','A001',NULL,NULL,'',NOW(),NOW()),
('ORD-1004','CUST-103','P004',1,1.00,0.002,800,'HIGH','DELIVERED','N4',DATE_ADD(NOW(),INTERVAL 1 DAY),'V001','A004',1650,NULL,'Urgent delivery',NOW(),NOW()),
('ORD-1005','CUST-104','P005',2,16.00,0.020,600,'MEDIUM','FAILED','N6',DATE_ADD(NOW(),INTERVAL 2 DAY),'V002',NULL,NULL,'Customer not available','',NOW(),NOW());

-- ── Routes ────────────────────────────────────────────────
INSERT INTO routes (order_id,source_node,dest_node,path,algorithm,base_cost,delay_penalty,total_cost,distance) VALUES
('ORD-1001','W1','N3','["W1","H1","N3"]','dijkstra',90,0,90,45),
('ORD-1002','W1','N5','["W1","H2","N5"]','astar',110,0,110,55),
('ORD-1003','W1','N7','["W1","H3","N7"]','dijkstra',160,0,160,80),
('ORD-1004','W1','N4','["W1","H2","N4"]','dijkstra',80,0,80,40),
('ORD-1005','W1','N6','["W1","H3","N6"]','dijkstra',110,25,135,55);

-- ── Returns ───────────────────────────────────────────────
INSERT INTO returns (order_id,customer_id,reason,return_route,return_cost,status) VALUES
('ORD-1005','CUST-104','Customer not available','["N6","H3","W1"]',110,'REQUESTED');

-- ── CRUD Stored Procedures ────────────────────────────────

DELIMITER //

-- Get all orders with customer + product info
CREATE PROCEDURE GetAllOrders()
BEGIN
  SELECT o.*, c.name AS customer_name, p.name AS product_name
  FROM orders o
  JOIN customers c ON o.customer_id = c.id
  JOIN products p ON o.product_id = p.id
  ORDER BY o.created_at DESC;
END//

-- Get orders by status
CREATE PROCEDURE GetOrdersByStatus(IN p_status VARCHAR(20))
BEGIN
  SELECT o.*, c.name AS customer_name
  FROM orders o
  JOIN customers c ON o.customer_id = c.id
  WHERE o.status = p_status;
END//

-- Update order status
CREATE PROCEDURE UpdateOrderStatus(IN p_id VARCHAR(20), IN p_status VARCHAR(20))
BEGIN
  UPDATE orders SET status = p_status, updated_at = NOW() WHERE id = p_id;
END//

-- Get inventory with alerts
CREATE PROCEDURE GetInventoryAlerts()
BEGIN
  SELECT i.*, p.name AS product_name,
    CASE
      WHEN i.quantity = 0 THEN 'OUT_OF_STOCK'
      WHEN i.quantity <= i.reorder_point THEN 'LOW_STOCK'
      WHEN i.quantity >= i.max_capacity THEN 'OVERSTOCK'
      ELSE 'OK'
    END AS alert_status
  FROM inventory i
  JOIN products p ON i.product_id = p.id;
END//

-- Assign vehicle to order
CREATE PROCEDURE AssignVehicle(IN p_order_id VARCHAR(20), IN p_vehicle_id VARCHAR(20))
BEGIN
  UPDATE orders SET assigned_vehicle = p_vehicle_id, status='SCHEDULED', updated_at=NOW()
  WHERE id = p_order_id;
  UPDATE vehicles SET available = FALSE WHERE id = p_vehicle_id;
END//

DELIMITER ;
