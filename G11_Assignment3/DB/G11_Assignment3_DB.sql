-- ================================================================
-- GoNature - Full Database Schema + Rich Demo Data
-- Group 11
-- Designed for clear, meaningful report visualizations
-- ================================================================

DROP DATABASE IF EXISTS gonature;
CREATE DATABASE IF NOT EXISTS gonature;
USE gonature;

-- ================================================================
-- TABLES
-- ================================================================
CREATE TABLE parks (
    park_id INT AUTO_INCREMENT PRIMARY KEY,
    park_name VARCHAR(100) NOT NULL,
    max_visitors INT NOT NULL,
    gap_for_walkins INT NOT NULL DEFAULT 0,
    estimated_visit_duration DOUBLE NOT NULL DEFAULT 4.0,
    current_visitors INT NOT NULL DEFAULT 0,
    full_price DOUBLE NOT NULL DEFAULT 50.0
);
CREATE TABLE employees (
    employee_id INT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    role ENUM('park_worker','park_manager','department_manager','service_rep') NOT NULL,
    park_id INT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    is_logged_in BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (park_id) REFERENCES parks(park_id)
);
CREATE TABLE subscribers (
    subscriber_id INT AUTO_INCREMENT PRIMARY KEY,
    id_number VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    family_members INT NOT NULL,
    credit_card VARCHAR(20)
);
CREATE TABLE guides (
    guide_id INT AUTO_INCREMENT PRIMARY KEY,
    id_number VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20)
);
CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    visitor_id VARCHAR(20) NOT NULL,
    park_id INT NOT NULL,
    visit_date DATE NOT NULL,
    visit_time TIME NOT NULL,
    num_visitors INT NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    order_type ENUM('individual','family','organized_group','walk_in','walk_in_group') NOT NULL,
    status ENUM('pending','confirmed','waitlist','cancelled','in_park','completed','no_show','expired') NOT NULL DEFAULT 'pending',
    confirmation_code VARCHAR(20),
    guide_id INT,
    subscriber_id INT,
    is_paid_in_advance BOOLEAN DEFAULT FALSE,
    total_price DOUBLE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reminder_sent BOOLEAN DEFAULT FALSE,
    reminder_sent_at DATETIME,
    reminder_confirmed BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (park_id) REFERENCES parks(park_id),
    FOREIGN KEY (guide_id) REFERENCES guides(guide_id),
    FOREIGN KEY (subscriber_id) REFERENCES subscribers(subscriber_id)
);
CREATE TABLE waitlist (
    waitlist_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    park_id INT NOT NULL,
    visit_date DATE NOT NULL,
    visit_time TIME NOT NULL,
    position INT NOT NULL,
    status ENUM('waiting','notified','confirmed','expired','cancelled') NOT NULL DEFAULT 'waiting',
    notified_at DATETIME,
    expires_at DATETIME,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (park_id) REFERENCES parks(park_id)
);
CREATE TABLE park_visits (
    visit_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT,
    park_id INT NOT NULL,
    visitor_id VARCHAR(20) NOT NULL,
    num_visitors INT NOT NULL,
    entry_time DATETIME NOT NULL,
    exit_time DATETIME,
    visit_type ENUM('reserved','walk_in') NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (park_id) REFERENCES parks(park_id)
);
CREATE TABLE promotions (
    promo_id INT AUTO_INCREMENT PRIMARY KEY,
    park_id INT NOT NULL,
    discount_percentage DOUBLE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    description VARCHAR(255),
    status ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
    requested_by INT NOT NULL,
    approved_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (park_id) REFERENCES parks(park_id),
    FOREIGN KEY (requested_by) REFERENCES employees(employee_id),
    FOREIGN KEY (approved_by) REFERENCES employees(employee_id)
);
CREATE TABLE parameter_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    park_id INT NOT NULL,
    parameter_name ENUM('max_visitors','gap_for_walkins','estimated_visit_duration') NOT NULL,
    old_value DOUBLE NOT NULL,
    new_value DOUBLE NOT NULL,
    status ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
    requested_by INT NOT NULL,
    approved_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (park_id) REFERENCES parks(park_id),
    FOREIGN KEY (requested_by) REFERENCES employees(employee_id),
    FOREIGN KEY (approved_by) REFERENCES employees(employee_id)
);
CREATE TABLE notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT,
    recipient_email VARCHAR(100),
    recipient_phone VARCHAR(20),
    notification_type ENUM('booking_confirmation','booking_cancellation','reminder','reminder_expired','waitlist_available','waitlist_expired') NOT NULL,
    message_text TEXT,
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);
CREATE TABLE reports (
    report_id INT AUTO_INCREMENT PRIMARY KEY,
    park_id INT,
    report_type ENUM('visits','cancellations','total_visitors','usage') NOT NULL,
    generated_by INT NOT NULL,
    report_month INT,
    report_year INT,
    start_date DATE,
    end_date DATE,
    report_data TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (park_id) REFERENCES parks(park_id),
    FOREIGN KEY (generated_by) REFERENCES employees(employee_id)
);

-- ================================================================
-- PARKS (small capacity so usage report shows FULL days)
-- ================================================================
INSERT INTO parks (park_name,max_visitors,gap_for_walkins,estimated_visit_duration,full_price) VALUES
('Banias Nature Reserve',   20, 3, 4.0, 50.0),
('Ein Gedi Nature Reserve', 18, 2, 4.0, 50.0),
('Masada National Park',    15, 2, 4.0, 50.0);

-- ================================================================
-- EMPLOYEES
-- ================================================================
INSERT INTO employees VALUES
(1001,'David','Cohen',  'david.cohen@gonature.com','department_manager',NULL,'david_dm', 'password123',FALSE),
(2001,'Sarah','Levi',   'sarah.levi@gonature.com', 'park_manager',      1,  'sarah_pm1','password123',FALSE),
(2002,'Moshe','Ben-Ari','moshe.ba@gonature.com',   'park_manager',      2,  'moshe_pm2','password123',FALSE),
(2003,'Yael', 'Shapira','yael.s@gonature.com',     'park_manager',      3,  'yael_pm3', 'password123',FALSE),
(3001,'Avi',  'Katz',   'avi.k@gonature.com',      'park_worker',       1,  'avi_pw1',  'password123',FALSE),
(3002,'Dana', 'Mizrahi','dana.m@gonature.com',      'park_worker',       2,  'dana_pw2', 'password123',FALSE),
(3003,'Ron',  'Peretz', 'ron.p@gonature.com',       'park_worker',       3,  'ron_pw3',  'password123',FALSE),
(4001,'Noa',  'Goldberg','noa.g@gonature.com',      'service_rep',       NULL,'noa_sr',  'password123',FALSE);

-- ================================================================
-- SUBSCRIBERS
-- ================================================================
INSERT INTO subscribers (id_number,first_name,last_name,phone,email,family_members,credit_card) VALUES
('123456789','Eli',     'Avraham', '050-1234567','eli.a@gmail.com',      4,'4580-1234-5678-9012'),
('234567890','Tamar',   'Friedman','052-2345678','tamar.f@gmail.com',    3,NULL),
('345678901','Oren',    'Dayan',   '054-3456789','oren.d@gmail.com',     5,'4580-9876-5432-1098'),
('325478717','Mhmod',   'Khalaile','050-3254787','mhmod@gonature.com',   3,'4580-1111-2222-3333'),
('214077323','Mohammed','Khalaile','052-2140773','mohammed@gonature.com',4,NULL),
('213470353','Ebrahim', 'Khalaile','054-2134703','ebrahim@gonature.com', 2,'4580-4444-5555-6666'),
('327875969','Kamar',   'Dabbah',  '050-3278759','kamar@gonature.com',   5,NULL),
('214674814','Hadi',    'Dabbah',  '052-2146748','hadi@gonature.com',    3,'4580-7777-8888-9999');

-- ================================================================
-- GUIDES
-- ================================================================
INSERT INTO guides (id_number,first_name,last_name,email,phone) VALUES
('111222333','Amir','Yosef', 'amir.y@guides.com','050-1112223'),
('222333444','Liat','Baruch','liat.b@guides.com','052-2223334');

-- ================================================================
-- HELPER: insert_order procedure for clean inserts
-- We insert orders then park_visits together per day
-- ================================================================

-- ================================================================
-- PARK 1 (Banias, max=40) - MAY 2026
-- Goal: 8 FULL days, 12 not-full days, varied visitor types
-- Visit report: entry times 7:00-16:00, stay 60-300 min
-- Cancellation: 3-4 per week, varying counts per day
-- ================================================================

-- MAY WEEK 1 (1-7)
INSERT INTO orders (visitor_id,park_id,visit_date,visit_time,num_visitors,email,phone,order_type,status,confirmation_code,subscriber_id,total_price) VALUES
('123456789',1,'2026-05-01','07:00:00',4,'eli.a@gmail.com','050-1234567','family','completed','P1M01',1,153.00),
('234567890',1,'2026-05-01','08:30:00',3,'tamar.f@gmail.com','052-2345678','individual','completed','P1M02',2,127.50),
('111222333',1,'2026-05-01','09:00:00',15,'amir.y@guides.com','050-1112223','organized_group','completed','P1M03',NULL,562.50),
('AAA00001', 1,'2026-05-01','10:00:00',15,'grp1@gmail.com','050-0000001','organized_group','completed','P1M04',NULL,562.50),
('AAA00002', 1,'2026-05-02','07:30:00',2,'ind1@gmail.com','050-0000002','walk_in','completed','P1M05',NULL,100.00),
('AAA00003', 1,'2026-05-02','09:00:00',3,'ind2@gmail.com','050-0000003','individual','completed','P1M06',NULL,127.50),
('111222333',1,'2026-05-03','08:00:00',12,'amir.y@guides.com','050-1112223','organized_group','completed','P1M07',NULL,450.00),
('345678901',1,'2026-05-03','10:00:00',5,'oren.d@gmail.com','054-3456789','family','completed','P1M08',3,191.25),
('AAA00004', 1,'2026-05-04','07:00:00',2,'ind3@gmail.com','050-0000004','individual','completed','P1M09',NULL,85.00),
('AAA00005', 1,'2026-05-04','09:30:00',4,'ind4@gmail.com','050-0000005','family','completed','P1M10',NULL,153.00),
('AAA00006', 1,'2026-05-04','11:00:00',2,'wlk1@gmail.com','050-0000006','walk_in','completed','P1M11',NULL,100.00),
('AAA00007', 1,'2026-05-05','08:00:00',3,'ind5@gmail.com','050-0000007','individual','completed','P1M12',NULL,127.50),
('222333444',1,'2026-05-05','09:00:00',10,'liat.b@guides.com','052-2223334','organized_group','completed','P1M13',NULL,375.00),
-- Cancellations week 1
('CX00001',1,'2026-05-02','10:00:00',2,'cx1@gmail.com','050-9000001','individual','cancelled','P1CX01',NULL,NULL),
('CX00002',1,'2026-05-02','14:00:00',3,'cx2@gmail.com','050-9000002','individual','no_show','P1CX02',NULL,NULL),
('CX00003',1,'2026-05-03','10:00:00',2,'cx3@gmail.com','050-9000003','individual','cancelled','P1CX03',NULL,NULL),
('CX00004',1,'2026-05-04','10:00:00',3,'cx4@gmail.com','050-9000004','individual','expired','P1CX04',NULL,NULL),
('CX00005',1,'2026-05-04','14:00:00',2,'cx5@gmail.com','050-9000005','individual','cancelled','P1CX05',NULL,NULL),
('CX00006',1,'2026-05-05','14:00:00',2,'cx6@gmail.com','050-9000006','individual','no_show','P1CX06',NULL,NULL);

-- MAY WEEK 2 (8-14) - FULL days
INSERT INTO orders (visitor_id,park_id,visit_date,visit_time,num_visitors,email,phone,order_type,status,confirmation_code,subscriber_id,total_price) VALUES
('AAA00008', 1,'2026-05-08','07:00:00',15,'grp2@gmail.com','050-0000008','organized_group','completed','P1M14',NULL,562.50),
('AAA00009', 1,'2026-05-08','09:00:00',15,'grp3@gmail.com','050-0000009','organized_group','completed','P1M15',NULL,562.50),
('AAA00010', 1,'2026-05-08','11:00:00',10,'grp4@gmail.com','050-0000010','organized_group','completed','P1M16',NULL,375.00),
('325478717',1,'2026-05-09','07:30:00',3,'mhmod@gonature.com','050-3254787','family','completed','P1M17',4,114.75),
('AAA00011', 1,'2026-05-09','09:00:00',15,'grp5@gmail.com','050-0000011','organized_group','completed','P1M18',NULL,562.50),
('AAA00012', 1,'2026-05-09','11:00:00',15,'grp6@gmail.com','050-0000012','organized_group','completed','P1M19',NULL,562.50),
('214077323',1,'2026-05-10','08:00:00',4,'mohammed@gonature.com','052-2140773','individual','completed','P1M20',5,153.00),
('AAA00013', 1,'2026-05-10','10:00:00',3,'wlk2@gmail.com','050-0000013','walk_in','completed','P1M21',NULL,150.00),
('AAA00014', 1,'2026-05-11','07:00:00',2,'ind6@gmail.com','050-0000014','individual','completed','P1M22',NULL,85.00),
('AAA00015', 1,'2026-05-11','08:30:00',3,'ind7@gmail.com','050-0000015','family','completed','P1M23',NULL,114.75),
('AAA00016', 1,'2026-05-11','10:00:00',4,'ind8@gmail.com','050-0000016','individual','completed','P1M24',NULL,153.00),
('AAA00017', 1,'2026-05-12','07:00:00',5,'ind9@gmail.com','050-0000017','family','completed','P1M25',NULL,191.25),
('111222333',1,'2026-05-12','09:00:00',12,'amir.y@guides.com','050-1112223','organized_group','completed','P1M26',NULL,450.00),
('AAA00018', 1,'2026-05-12','11:00:00',10,'grp7@gmail.com','050-0000018','organized_group','completed','P1M27',NULL,375.00),
-- Cancellations week 2 (more on some days)
('CX00007',1,'2026-05-08','10:00:00',2,'cx7@gmail.com','050-9000007','individual','cancelled','P1CX07',NULL,NULL),
('CX00008',1,'2026-05-08','14:00:00',3,'cx8@gmail.com','050-9000008','individual','no_show','P1CX08',NULL,NULL),
('CX00009',1,'2026-05-08','16:00:00',2,'cx9@gmail.com','050-9000009','individual','expired','P1CX09',NULL,NULL),
('CX00010',1,'2026-05-10','10:00:00',2,'cx10@gmail.com','050-9000010','individual','cancelled','P1CX10',NULL,NULL),
('CX00011',1,'2026-05-10','14:00:00',4,'cx11@gmail.com','050-9000011','individual','no_show','P1CX11',NULL,NULL),
('CX00012',1,'2026-05-10','16:00:00',2,'cx12@gmail.com','050-9000012','individual','cancelled','P1CX12',NULL,NULL),
('CX00013',1,'2026-05-12','10:00:00',3,'cx13@gmail.com','050-9000013','individual','expired','P1CX13',NULL,NULL),
('CX00014',1,'2026-05-12','14:00:00',2,'cx14@gmail.com','050-9000014','individual','no_show','P1CX14',NULL,NULL);

-- MAY WEEK 3-4 (15-31)
INSERT INTO orders (visitor_id,park_id,visit_date,visit_time,num_visitors,email,phone,order_type,status,confirmation_code,subscriber_id,total_price) VALUES
('213470353',1,'2026-05-15','07:00:00',2,'ebrahim@gonature.com','054-2134703','individual','completed','P1M28',6,85.00),
('327875969',1,'2026-05-15','09:00:00',5,'kamar@gonature.com','050-3278759','family','completed','P1M29',7,191.25),
('222333444',1,'2026-05-15','11:00:00',15,'liat.b@guides.com','052-2223334','organized_group','completed','P1M30',NULL,562.50),
('214674814',1,'2026-05-16','07:30:00',3,'hadi@gonature.com','052-2146748','individual','completed','P1M31',8,127.50),
('AAA00019', 1,'2026-05-16','09:00:00',2,'wlk3@gmail.com','050-0000019','walk_in','completed','P1M32',NULL,100.00),
('AAA00020', 1,'2026-05-19','07:00:00',15,'grp8@gmail.com','050-0000020','organized_group','completed','P1M33',NULL,562.50),
('AAA00021', 1,'2026-05-19','09:00:00',15,'grp9@gmail.com','050-0000021','organized_group','completed','P1M34',NULL,562.50),
('123456789',1,'2026-05-20','08:00:00',4,'eli.a@gmail.com','050-1234567','family','completed','P1M35',1,153.00),
('AAA00022', 1,'2026-05-20','10:00:00',3,'ind10@gmail.com','050-0000022','individual','completed','P1M36',NULL,127.50),
('AAA00023', 1,'2026-05-22','07:00:00',15,'grp10@gmail.com','050-0000023','organized_group','completed','P1M37',NULL,562.50),
('AAA00024', 1,'2026-05-22','09:00:00',15,'grp11@gmail.com','050-0000024','organized_group','completed','P1M38',NULL,562.50),
('AAA00025', 1,'2026-05-23','08:00:00',2,'wlk4@gmail.com','050-0000025','walk_in','completed','P1M39',NULL,100.00),
('234567890',1,'2026-05-26','09:00:00',3,'tamar.f@gmail.com','052-2345678','individual','completed','P1M40',2,127.50),
('AAA00026', 1,'2026-05-27','07:00:00',15,'grp12@gmail.com','050-0000026','organized_group','completed','P1M41',NULL,562.50),
('AAA00027', 1,'2026-05-27','09:00:00',15,'grp13@gmail.com','050-0000027','organized_group','completed','P1M42',NULL,562.50),
('AAA00028', 1,'2026-05-29','08:00:00',3,'ind11@gmail.com','050-0000028','individual','completed','P1M43',NULL,127.50),
-- Cancellations week 3-4
('CX00015',1,'2026-05-15','10:00:00',2,'cx15@gmail.com','050-9000015','individual','cancelled','P1CX15',NULL,NULL),
('CX00016',1,'2026-05-16','10:00:00',3,'cx16@gmail.com','050-9000016','individual','no_show','P1CX16',NULL,NULL),
('CX00017',1,'2026-05-16','14:00:00',2,'cx17@gmail.com','050-9000017','individual','cancelled','P1CX17',NULL,NULL),
('CX00018',1,'2026-05-19','10:00:00',2,'cx18@gmail.com','050-9000018','individual','expired','P1CX18',NULL,NULL),
('CX00019',1,'2026-05-20','10:00:00',3,'cx19@gmail.com','050-9000019','individual','no_show','P1CX19',NULL,NULL),
('CX00020',1,'2026-05-22','10:00:00',2,'cx20@gmail.com','050-9000020','individual','cancelled','P1CX20',NULL,NULL),
('CX00021',1,'2026-05-22','14:00:00',3,'cx21@gmail.com','050-9000021','individual','expired','P1CX21',NULL,NULL),
('CX00022',1,'2026-05-22','16:00:00',2,'cx22@gmail.com','050-9000022','individual','no_show','P1CX22',NULL,NULL),
('CX00023',1,'2026-05-26','10:00:00',2,'cx23@gmail.com','050-9000023','individual','cancelled','P1CX23',NULL,NULL),
('CX00024',1,'2026-05-29','10:00:00',3,'cx24@gmail.com','050-9000024','individual','no_show','P1CX24',NULL,NULL),
('CX00025',1,'2026-05-29','14:00:00',2,'cx25@gmail.com','050-9000025','individual','expired','P1CX25',NULL,NULL);

-- ================================================================
-- PARK_VISITS for Park 1 May - VARIED entry times 7:00-16:00
-- VARIED stay times 60-300 min for good scatter plot
-- ================================================================
INSERT INTO park_visits (order_id,park_id,visitor_id,num_visitors,entry_time,exit_time,visit_type) VALUES
-- Day 01 (FULL - all 4 visit types, varied hours)
(1, 1,'123456789',4, '2026-05-01 07:10:00','2026-05-01 11:10:00','reserved'),
(2, 1,'234567890',3, '2026-05-01 07:35:00','2026-05-01 11:35:00','reserved'),
(3, 1,'111222333',15,'2026-05-01 08:05:00','2026-05-01 12:05:00','reserved'),
(4, 1,'AAA00001', 15,'2026-05-01 08:35:00','2026-05-01 12:35:00','reserved'),
-- Day 02 (small)
(5, 1,'AAA00002', 2, '2026-05-02 09:05:00','2026-05-02 12:35:00','walk_in'),
(6, 1,'AAA00003', 3, '2026-05-02 09:38:00','2026-05-02 13:08:00','reserved'),
-- Day 03
(7, 1,'111222333',12,'2026-05-03 10:05:00','2026-05-03 13:35:00','reserved'),
(8, 1,'345678901',5, '2026-05-03 10:35:00','2026-05-03 14:05:00','reserved'),
-- Day 04
(9, 1,'AAA00004', 2, '2026-05-04 11:08:00','2026-05-04 14:08:00','reserved'),
(10,1,'AAA00005', 4, '2026-05-04 11:35:00','2026-05-04 14:35:00','reserved'),
(11,1,'AAA00006', 2, '2026-05-04 12:05:00','2026-05-04 15:05:00','walk_in'),
-- Day 05
(12,1,'AAA00007', 3, '2026-05-05 12:35:00','2026-05-05 15:35:00','reserved'),
(13,1,'222333444',10,'2026-05-05 13:05:00','2026-05-05 15:05:00','reserved'),
-- Day 08 (FULL=40)
(14,1,'AAA00008', 15,'2026-05-08 13:38:00','2026-05-08 15:38:00','reserved'),
(15,1,'AAA00009', 15,'2026-05-08 14:05:00','2026-05-08 16:05:00','reserved'),
(16,1,'AAA00010', 10,'2026-05-08 14:35:00','2026-05-08 16:35:00','reserved'),
-- Day 09 (FULL=33)
(17,1,'325478717',3, '2026-05-09 15:05:00','2026-05-09 16:20:00','reserved'),
(18,1,'AAA00011', 15,'2026-05-09 15:35:00','2026-05-09 16:50:00','reserved'),
(19,1,'AAA00012', 15,'2026-05-09 07:10:00','2026-05-09 11:10:00','reserved'),
-- Day 10
(20,1,'214077323',4, '2026-05-10 07:35:00','2026-05-10 11:35:00','reserved'),
(21,1,'AAA00013', 3, '2026-05-10 08:05:00','2026-05-10 12:05:00','walk_in'),
-- Day 11
(22,1,'AAA00014', 2, '2026-05-11 08:35:00','2026-05-11 12:35:00','reserved'),
(23,1,'AAA00015', 3, '2026-05-11 09:05:00','2026-05-11 12:35:00','reserved'),
(24,1,'AAA00016', 4, '2026-05-11 09:38:00','2026-05-11 13:08:00','reserved'),
-- Day 12 (FULL=27)
(25,1,'AAA00017', 5, '2026-05-12 10:05:00','2026-05-12 13:35:00','reserved'),
(26,1,'111222333',12,'2026-05-12 10:35:00','2026-05-12 14:05:00','reserved'),
(27,1,'AAA00018', 10,'2026-05-12 11:08:00','2026-05-12 14:08:00','reserved'),
-- Day 15
(28,1,'213470353',2, '2026-05-15 11:35:00','2026-05-15 14:35:00','reserved'),
(29,1,'327875969',5, '2026-05-15 12:05:00','2026-05-15 15:05:00','reserved'),
(30,1,'222333444',15,'2026-05-15 12:35:00','2026-05-15 15:35:00','reserved'),
-- Day 16
(31,1,'214674814',3, '2026-05-16 13:05:00','2026-05-16 15:05:00','reserved'),
(32,1,'AAA00019', 2, '2026-05-16 13:38:00','2026-05-16 15:38:00','walk_in'),
-- Day 19 (FULL=30)
(33,1,'AAA00020', 15,'2026-05-19 14:05:00','2026-05-19 16:05:00','reserved'),
(34,1,'AAA00021', 15,'2026-05-19 14:35:00','2026-05-19 16:35:00','reserved'),
-- Day 20
(35,1,'123456789',4, '2026-05-20 15:05:00','2026-05-20 16:20:00','reserved'),
(36,1,'AAA00022', 3, '2026-05-20 15:35:00','2026-05-20 16:50:00','reserved'),
-- Day 22 (FULL=30)
(37,1,'AAA00023', 15,'2026-05-22 07:10:00','2026-05-22 11:10:00','reserved'),
(38,1,'AAA00024', 15,'2026-05-22 07:35:00','2026-05-22 11:35:00','reserved'),
-- Day 23
(39,1,'AAA00025', 2, '2026-05-23 08:05:00','2026-05-23 12:05:00','walk_in'),
-- Day 26
(40,1,'234567890',3, '2026-05-26 08:35:00','2026-05-26 12:35:00','reserved'),
-- Day 27 (FULL=30)
(41,1,'AAA00026', 15,'2026-05-27 09:05:00','2026-05-27 12:35:00','reserved'),
(42,1,'AAA00027', 15,'2026-05-27 09:38:00','2026-05-27 13:08:00','reserved'),
-- Day 29
(43,1,'AAA00028', 3, '2026-05-29 10:05:00','2026-05-29 13:35:00','reserved');

-- ================================================================
-- PARK 2 (Ein Gedi, max=35) - MAY 2026
-- ================================================================
INSERT INTO orders (visitor_id,park_id,visit_date,visit_time,num_visitors,email,phone,order_type,status,confirmation_code,subscriber_id,total_price) VALUES
('123456789',2,'2026-05-01','07:00:00',4,'eli.a@gmail.com','050-1234567','family','completed','P2M01',1,153.00),
('AAA00029', 2,'2026-05-01','09:00:00',15,'grp14@gmail.com','050-0000029','organized_group','completed','P2M02',NULL,562.50),
('AAA00030', 2,'2026-05-01','11:00:00',15,'grp15@gmail.com','050-0000030','organized_group','completed','P2M03',NULL,562.50),
('234567890',2,'2026-05-05','07:30:00',3,'tamar.f@gmail.com','052-2345678','individual','completed','P2M04',2,127.50),
('AAA00031', 2,'2026-05-05','09:00:00',12,'grp16@gmail.com','050-0000031','organized_group','completed','P2M05',NULL,450.00),
('345678901',2,'2026-05-08','08:00:00',5,'oren.d@gmail.com','054-3456789','family','completed','P2M06',3,191.25),
('AAA00032', 2,'2026-05-08','10:00:00',15,'grp17@gmail.com','050-0000032','organized_group','completed','P2M07',NULL,562.50),
('AAA00033', 2,'2026-05-08','12:00:00',15,'grp18@gmail.com','050-0000033','organized_group','completed','P2M08',NULL,562.50),
('325478717',2,'2026-05-12','07:00:00',3,'mhmod@gonature.com','050-3254787','individual','completed','P2M09',4,127.50),
('AAA00034', 2,'2026-05-12','09:00:00',2,'wlk5@gmail.com','050-0000034','walk_in','completed','P2M10',NULL,100.00),
('AAA00035', 2,'2026-05-15','07:00:00',15,'grp19@gmail.com','050-0000035','organized_group','completed','P2M11',NULL,562.50),
('AAA00036', 2,'2026-05-15','09:00:00',15,'grp20@gmail.com','050-0000036','organized_group','completed','P2M12',NULL,562.50),
('214077323',2,'2026-05-19','08:00:00',4,'mohammed@gonature.com','052-2140773','individual','completed','P2M13',5,153.00),
('AAA00037', 2,'2026-05-19','10:00:00',3,'ind12@gmail.com','050-0000037','individual','completed','P2M14',NULL,127.50),
('222333444',2,'2026-05-22','07:00:00',10,'liat.b@guides.com','052-2223334','organized_group','completed','P2M15',NULL,375.00),
('AAA00038', 2,'2026-05-22','09:00:00',15,'grp21@gmail.com','050-0000038','organized_group','completed','P2M16',NULL,562.50),
('AAA00039', 2,'2026-05-26','08:00:00',2,'wlk6@gmail.com','050-0000039','walk_in','completed','P2M17',NULL,100.00),
('213470353',2,'2026-05-26','10:00:00',2,'ebrahim@gonature.com','054-2134703','individual','completed','P2M18',6,85.00),
-- Cancellations Park 2 May
('CX00026',2,'2026-05-01','14:00:00',2,'cx26@gmail.com','050-9000026','individual','cancelled','P2CX01',NULL,NULL),
('CX00027',2,'2026-05-05','10:00:00',3,'cx27@gmail.com','050-9000027','individual','no_show','P2CX02',NULL,NULL),
('CX00028',2,'2026-05-05','14:00:00',2,'cx28@gmail.com','050-9000028','individual','expired','P2CX03',NULL,NULL),
('CX00029',2,'2026-05-08','14:00:00',2,'cx29@gmail.com','050-9000029','individual','cancelled','P2CX04',NULL,NULL),
('CX00030',2,'2026-05-08','16:00:00',3,'cx30@gmail.com','050-9000030','individual','no_show','P2CX05',NULL,NULL),
('CX00031',2,'2026-05-12','10:00:00',2,'cx31@gmail.com','050-9000031','individual','cancelled','P2CX06',NULL,NULL),
('CX00032',2,'2026-05-15','10:00:00',3,'cx32@gmail.com','050-9000032','individual','expired','P2CX07',NULL,NULL),
('CX00033',2,'2026-05-15','14:00:00',2,'cx33@gmail.com','050-9000033','individual','no_show','P2CX08',NULL,NULL),
('CX00034',2,'2026-05-19','10:00:00',2,'cx34@gmail.com','050-9000034','individual','cancelled','P2CX09',NULL,NULL),
('CX00035',2,'2026-05-22','14:00:00',3,'cx35@gmail.com','050-9000035','individual','no_show','P2CX10',NULL,NULL),
('CX00036',2,'2026-05-22','16:00:00',2,'cx36@gmail.com','050-9000036','individual','expired','P2CX11',NULL,NULL),
('CX00037',2,'2026-05-26','14:00:00',2,'cx37@gmail.com','050-9000037','individual','cancelled','P2CX12',NULL,NULL);

INSERT INTO park_visits (order_id,park_id,visitor_id,num_visitors,entry_time,exit_time,visit_type) VALUES
(44,2,'123456789',4, '2026-05-01 07:08:00','2026-05-01 11:17:00','reserved'),
(45,2,'AAA00029', 15,'2026-05-01 09:05:00','2026-05-01 13:18:00','reserved'),
(46,2,'AAA00030', 15,'2026-05-01 11:08:00','2026-05-01 14:21:00','reserved'),
(47,2,'234567890',3, '2026-05-05 07:35:00','2026-05-05 11:41:00','reserved'),
(48,2,'AAA00031', 12,'2026-05-05 09:08:00','2026-05-05 13:13:00','reserved'),
(49,2,'345678901',5, '2026-05-08 08:05:00','2026-05-08 12:29:00','reserved'),
(50,2,'AAA00032', 15,'2026-05-08 10:08:00','2026-05-08 12:50:00','reserved'),
(51,2,'AAA00033', 15,'2026-05-08 12:05:00','2026-05-08 15:20:00','reserved'),
(52,2,'325478717',3, '2026-05-12 07:05:00','2026-05-12 11:59:00','reserved'),
(53,2,'AAA00034', 2, '2026-05-12 09:08:00','2026-05-12 10:30:00','walk_in'),
(54,2,'AAA00035', 15,'2026-05-15 07:05:00','2026-05-15 11:43:00','reserved'),
(55,2,'AAA00036', 15,'2026-05-15 09:08:00','2026-05-15 13:24:00','reserved'),
(56,2,'214077323',4, '2026-05-19 08:08:00','2026-05-19 12:59:00','reserved'),
(57,2,'AAA00037', 3, '2026-05-19 10:05:00','2026-05-19 12:40:00','reserved'),
(58,2,'222333444',10,'2026-05-22 07:08:00','2026-05-22 11:54:00','reserved'),
(59,2,'AAA00038', 15,'2026-05-22 09:05:00','2026-05-22 13:34:00','reserved'),
(60,2,'AAA00039', 2, '2026-05-26 08:05:00','2026-05-26 09:39:00','walk_in'),
(61,2,'213470353',2, '2026-05-26 10:08:00','2026-05-26 12:53:00','reserved');

-- ================================================================
-- PARK 3 (Masada, max=30) - MAY 2026
-- ================================================================
INSERT INTO orders (visitor_id,park_id,visit_date,visit_time,num_visitors,email,phone,order_type,status,confirmation_code,subscriber_id,total_price) VALUES
('327875969',3,'2026-05-02','07:00:00',5,'kamar@gonature.com','050-3278759','family','completed','P3M01',7,191.25),
('AAA00040', 3,'2026-05-02','09:00:00',10,'grp22@gmail.com','050-0000040','organized_group','completed','P3M02',NULL,375.00),
('AAA00041', 3,'2026-05-02','11:00:00',15,'grp23@gmail.com','050-0000041','organized_group','completed','P3M03',NULL,562.50),
('214674814',3,'2026-05-06','07:30:00',3,'hadi@gonature.com','052-2146748','individual','completed','P3M04',8,127.50),
('AAA00042', 3,'2026-05-06','09:00:00',15,'grp24@gmail.com','050-0000042','organized_group','completed','P3M05',NULL,562.50),
('111222333',3,'2026-05-09','08:00:00',12,'amir.y@guides.com','050-1112223','organized_group','completed','P3M06',NULL,450.00),
('AAA00043', 3,'2026-05-09','10:00:00',15,'grp25@gmail.com','050-0000043','organized_group','completed','P3M07',NULL,562.50),
('123456789',3,'2026-05-13','07:00:00',4,'eli.a@gmail.com','050-1234567','family','completed','P3M08',1,153.00),
('AAA00044', 3,'2026-05-13','09:00:00',2,'wlk7@gmail.com','050-0000044','walk_in','completed','P3M09',NULL,100.00),
('234567890',3,'2026-05-16','08:00:00',3,'tamar.f@gmail.com','052-2345678','individual','completed','P3M10',2,127.50),
('222333444',3,'2026-05-16','10:00:00',15,'liat.b@guides.com','052-2223334','organized_group','completed','P3M11',NULL,562.50),
('AAA00045', 3,'2026-05-20','07:00:00',15,'grp26@gmail.com','050-0000045','organized_group','completed','P3M12',NULL,562.50),
('AAA00046', 3,'2026-05-20','09:00:00',15,'grp27@gmail.com','050-0000046','organized_group','completed','P3M13',NULL,562.50),
('345678901',3,'2026-05-23','08:00:00',5,'oren.d@gmail.com','054-3456789','family','completed','P3M14',3,191.25),
('AAA00047', 3,'2026-05-23','10:00:00',3,'ind13@gmail.com','050-0000047','individual','completed','P3M15',NULL,127.50),
('AAA00048', 3,'2026-05-27','07:00:00',15,'grp28@gmail.com','050-0000048','organized_group','completed','P3M16',NULL,562.50),
('AAA00049', 3,'2026-05-27','09:00:00',12,'grp29@gmail.com','050-0000049','organized_group','completed','P3M17',NULL,450.00),
-- Cancellations Park 3 May
('CX00038',3,'2026-05-02','14:00:00',2,'cx38@gmail.com','050-9000038','individual','cancelled','P3CX01',NULL,NULL),
('CX00039',3,'2026-05-06','10:00:00',3,'cx39@gmail.com','050-9000039','individual','no_show','P3CX02',NULL,NULL),
('CX00040',3,'2026-05-09','10:00:00',2,'cx40@gmail.com','050-9000040','individual','cancelled','P3CX03',NULL,NULL),
('CX00041',3,'2026-05-09','14:00:00',2,'cx41@gmail.com','050-9000041','individual','expired','P3CX04',NULL,NULL),
('CX00042',3,'2026-05-13','10:00:00',3,'cx42@gmail.com','050-9000042','individual','no_show','P3CX05',NULL,NULL),
('CX00043',3,'2026-05-16','10:00:00',2,'cx43@gmail.com','050-9000043','individual','cancelled','P3CX06',NULL,NULL),
('CX00044',3,'2026-05-20','10:00:00',3,'cx44@gmail.com','050-9000044','individual','no_show','P3CX07',NULL,NULL),
('CX00045',3,'2026-05-20','14:00:00',2,'cx45@gmail.com','050-9000045','individual','expired','P3CX08',NULL,NULL),
('CX00046',3,'2026-05-23','10:00:00',2,'cx46@gmail.com','050-9000046','individual','cancelled','P3CX09',NULL,NULL),
('CX00047',3,'2026-05-27','10:00:00',3,'cx47@gmail.com','050-9000047','individual','no_show','P3CX10',NULL,NULL),
('CX00048',3,'2026-05-27','14:00:00',2,'cx48@gmail.com','050-9000048','individual','expired','P3CX11',NULL,NULL);

INSERT INTO park_visits (order_id,park_id,visitor_id,num_visitors,entry_time,exit_time,visit_type) VALUES
(62,3,'327875969',5, '2026-05-02 07:08:00','2026-05-02 12:07:00','reserved'),
(63,3,'AAA00040', 10,'2026-05-02 09:05:00','2026-05-02 13:29:00','reserved'),
(64,3,'AAA00041', 15,'2026-05-02 11:08:00','2026-05-02 13:48:00','reserved'),
(65,3,'214674814',3, '2026-05-06 07:35:00','2026-05-06 12:10:00','reserved'),
(66,3,'AAA00042', 15,'2026-05-06 09:05:00','2026-05-06 13:23:00','reserved'),
(67,3,'111222333',12,'2026-05-09 08:05:00','2026-05-09 12:58:00','reserved'),
(68,3,'AAA00043', 15,'2026-05-09 10:08:00','2026-05-09 13:58:00','reserved'),
(69,3,'123456789',4, '2026-05-13 07:05:00','2026-05-13 11:44:00','reserved'),
(70,3,'AAA00044', 2, '2026-05-13 09:08:00','2026-05-13 11:04:00','walk_in'),
(71,3,'234567890',3, '2026-05-16 08:05:00','2026-05-16 13:00:00','reserved'),
(72,3,'222333444',15,'2026-05-16 10:08:00','2026-05-16 13:24:00','reserved'),
(73,3,'AAA00045', 15,'2026-05-20 07:05:00','2026-05-20 11:41:00','reserved'),
(74,3,'AAA00046', 15,'2026-05-20 09:08:00','2026-05-20 13:20:00','reserved'),
(75,3,'345678901',5, '2026-05-23 08:05:00','2026-05-23 12:50:00','reserved'),
(76,3,'AAA00047', 3, '2026-05-23 10:08:00','2026-05-23 12:46:00','reserved'),
(77,3,'AAA00048', 15,'2026-05-27 07:08:00','2026-05-27 11:10:00','reserved'),
(78,3,'AAA00049', 12,'2026-05-27 09:05:00','2026-05-27 13:47:00','reserved');

-- ================================================================
-- ACTIVE DEMO ORDERS (today + tomorrow)
-- ================================================================
INSERT INTO orders (visitor_id,park_id,visit_date,visit_time,num_visitors,email,phone,order_type,status,confirmation_code,subscriber_id,total_price) VALUES
('123456789',1,CURDATE(),'10:00:00',2,'eli.a@gmail.com','050-1234567','individual','confirmed','CONF-DEMO1',1,85.00),
('234567890',2,CURDATE(),'11:00:00',3,'tamar.f@gmail.com','052-2345678','family','confirmed','CONF-DEMO2',2,114.75),
('111222333',1,CURDATE(),'09:00:00',10,'amir.y@guides.com','050-1112223','organized_group','confirmed','CONF-DEMO3',NULL,375.00),
('325478717',3,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'10:00:00',3,'mhmod@gonature.com','050-3254787','individual','confirmed','CONF-DEMO4',4,127.50),
('345678901',2,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'14:00:00',5,'oren.d@gmail.com','054-3456789','family','confirmed','CONF-DEMO5',3,191.25);

-- ================================================================
-- PROMOTIONS
-- ================================================================
INSERT INTO promotions (park_id,discount_percentage,start_date,end_date,description,status,requested_by,approved_by) VALUES
(1,20.0,'2026-05-01','2026-05-31','Summer promotion','approved',2001,1001),
(2,15.0,DATE_SUB(CURDATE(),INTERVAL 5 DAY),DATE_ADD(CURDATE(),INTERVAL 10 DAY),'Weekend special','approved',2002,1001),
(3,10.0,CURDATE(),DATE_ADD(CURDATE(),INTERVAL 14 DAY),'Independence Day','pending',2003,NULL);

-- ================================================================
-- PARAMETER REQUESTS
-- ================================================================
INSERT INTO parameter_requests (park_id,parameter_name,old_value,new_value,status,requested_by,approved_by) VALUES
(1,'max_visitors',20,25,'approved',2001,1001),
(2,'gap_for_walkins',2,4,'approved',2002,1001),
(3,'estimated_visit_duration',4.0,3.5,'pending',2003,NULL);

-- ================================================================
-- NOTIFICATIONS
-- ================================================================
INSERT INTO notifications (order_id,recipient_email,notification_type,message_text,is_read) VALUES
(1,'eli.a@gmail.com','booking_confirmation','Your booking CONF-P1M01 confirmed.',TRUE),
(79,'mhmod@gonature.com','reminder','Reminder: visit tomorrow CONF-DEMO4. Confirm within 2 hours.',FALSE),
(80,'oren.d@gmail.com','reminder','Reminder: visit tomorrow CONF-DEMO5. Confirm within 2 hours.',FALSE);

-- ================================================================
-- VERIFY
-- ================================================================
SELECT 'GoNature database ready!' AS status;
SELECT CONCAT(COUNT(*),' parks')       FROM parks;
SELECT CONCAT(COUNT(*),' employees')   FROM employees;
SELECT CONCAT(COUNT(*),' subscribers') FROM subscribers;
SELECT CONCAT(COUNT(*),' guides')      FROM guides;
SELECT CONCAT(COUNT(*),' orders')      FROM orders;
SELECT CONCAT(COUNT(*),' park_visits') FROM park_visits;
SELECT CONCAT(SUM(status='completed'),' completed') FROM orders;
SELECT CONCAT(SUM(status IN ('cancelled','no_show','expired')),' cancelled/noshow/expired') FROM orders;