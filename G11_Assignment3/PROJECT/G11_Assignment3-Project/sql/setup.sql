-- ================================================================
-- GoNature - Full Database Schema
-- Based on Project Specification (ספר הפרויקט)
-- Group 11
-- ================================================================

CREATE DATABASE IF NOT EXISTS gonature;
USE gonature;

-- ================================================================
-- 1. PARKS - Nature parks managed by the department
-- ================================================================
CREATE TABLE parks (
    park_id INT AUTO_INCREMENT PRIMARY KEY,
    park_name VARCHAR(100) NOT NULL,
    max_visitors INT NOT NULL,                    -- מכסה מרבית - maximum capacity at any time
    gap_for_walkins INT NOT NULL DEFAULT 0,       -- פער - difference between max and reservations allowed
    estimated_visit_duration DOUBLE NOT NULL DEFAULT 4.0, -- שעות שהייה משוערות - default 4 hours
    current_visitors INT NOT NULL DEFAULT 0,      -- מספר מבקרים נוכחי
    full_price DOUBLE NOT NULL DEFAULT 50.0       -- מחיר מלא per visitor (set by tourism ministry)
);

-- ================================================================
-- 2. EMPLOYEES - All department workers (park workers, managers, HQ)
--    Roles: park_worker, park_manager, department_manager, service_rep
-- ================================================================
CREATE TABLE employees (
    employee_id INT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    role ENUM('park_worker', 'park_manager', 'department_manager', 'service_rep') NOT NULL,
    park_id INT,                                  -- NULL for department_manager and service_rep
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    is_logged_in BOOLEAN NOT NULL DEFAULT FALSE,  -- prevent double login
    FOREIGN KEY (park_id) REFERENCES parks(park_id)
);

-- ================================================================
-- 3. SUBSCRIBERS - Family club members (מנויים משפחתיים / חבר מועדון)
--    Registered by service_rep at HQ
-- ================================================================
CREATE TABLE subscribers (
    subscriber_id INT AUTO_INCREMENT PRIMARY KEY,
    id_number VARCHAR(20) NOT NULL UNIQUE,        -- מספר זהות
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    family_members INT NOT NULL,                  -- מספר בני משפחה גרעינית
    credit_card VARCHAR(20)                       -- אופציונלי - can pay cash
);

-- ================================================================
-- 4. GUIDES - Registered group guides (מדריכי קבוצות)
--    Registered by service_rep at HQ
-- ================================================================
CREATE TABLE guides (
    guide_id INT AUTO_INCREMENT PRIMARY KEY,
    id_number VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20)
);

-- ================================================================
-- 5. ORDERS - Visit reservations (הזמנות ביקור)
--    Types: individual, family, organized_group, walk_in, walk_in_group
--    Status flow: pending → confirmed → completed/cancelled/no_show
-- ================================================================
CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    visitor_id VARCHAR(20) NOT NULL,              -- מספר זהות of the person booking
    park_id INT NOT NULL,
    visit_date DATE NOT NULL,                     -- יום הביקור
    visit_time TIME NOT NULL,                     -- שעת כניסה מתוכננת
    num_visitors INT NOT NULL,                    -- מספר מבקרים
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    order_type ENUM(
        'individual',                             -- ביקור אישי/משפחתי מוזמן
        'family',                                 -- ביקור משפחתי מוזמן (subscriber)
        'organized_group',                        -- ביקור קבוצתי בהזמנה (max 15)
        'walk_in',                                -- ביקור מזדמן אישי
        'walk_in_group'                           -- ביקור קבוצתי מזדמן
    ) NOT NULL,
    status ENUM(
        'pending',                                -- ממתין לאישור
        'confirmed',                              -- אושר
        'waitlist',                                -- ברשימת המתנה
        'cancelled',                              -- בוטל
        'completed',                              -- בוצע - נכנסו לפארק
        'no_show',                                -- לא הגיעו
        'expired'                                 -- פג תוקף (לא אישר תזכורת)
    ) NOT NULL DEFAULT 'pending',
    confirmation_code VARCHAR(20),                -- קוד אישור
    guide_id INT,                                 -- מדריך (for organized groups)
    subscriber_id INT,                            -- מנוי (for subscriber discount)
    is_paid_in_advance BOOLEAN DEFAULT FALSE,     -- תשלום מראש (12% extra discount for groups)
    total_price DOUBLE,                           -- סכום לתשלום
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reminder_sent BOOLEAN DEFAULT FALSE,          -- תזכורת נשלחה
    reminder_sent_at DATETIME,                    -- מתי נשלחה התזכורת
    reminder_confirmed BOOLEAN DEFAULT FALSE,     -- המזמין אישר את התזכורת
    FOREIGN KEY (park_id) REFERENCES parks(park_id),
    FOREIGN KEY (guide_id) REFERENCES guides(guide_id),
    FOREIGN KEY (subscriber_id) REFERENCES subscribers(subscriber_id)
);

-- ================================================================
-- 6. WAITLIST - Waiting list for full parks (רשימת המתנה)
-- ================================================================
CREATE TABLE waitlist (
    waitlist_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    park_id INT NOT NULL,
    visit_date DATE NOT NULL,
    visit_time TIME NOT NULL,
    position INT NOT NULL,                        -- מיקום בתור
    status ENUM('waiting', 'notified', 'confirmed', 'expired', 'cancelled') NOT NULL DEFAULT 'waiting',
    notified_at DATETIME,                         -- מתי נשלחה הודעה
    expires_at DATETIME,                          -- ההזמנה נשמרת למזמין שעה
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (park_id) REFERENCES parks(park_id)
);

-- ================================================================
-- 7. PARK_VISITS - Entry and exit tracking (בקרת כניסה ויציאה)
-- ================================================================
CREATE TABLE park_visits (
    visit_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT,                                 -- NULL for walk-in visitors
    park_id INT NOT NULL,
    visitor_id VARCHAR(20) NOT NULL,              -- מספר זיהוי
    num_visitors INT NOT NULL,                    -- מספר מבקרים בפועל
    entry_time DATETIME NOT NULL,
    exit_time DATETIME,                           -- NULL until they leave
    visit_type ENUM('reserved', 'walk_in') NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (park_id) REFERENCES parks(park_id)
);

-- ================================================================
-- 8. PROMOTIONS - Special discounts by park manager (מבצעים)
--    Must be approved by department manager
-- ================================================================
CREATE TABLE promotions (
    promo_id INT AUTO_INCREMENT PRIMARY KEY,
    park_id INT NOT NULL,
    discount_percentage DOUBLE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    description VARCHAR(255),
    status ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending',
    requested_by INT NOT NULL,                    -- park_manager employee_id
    approved_by INT,                              -- department_manager employee_id
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (park_id) REFERENCES parks(park_id),
    FOREIGN KEY (requested_by) REFERENCES employees(employee_id),
    FOREIGN KEY (approved_by) REFERENCES employees(employee_id)
);

-- ================================================================
-- 9. PARAMETER_CHANGE_REQUESTS - Park parameter changes
--    Park manager requests, department manager approves
--    Parameters: max_visitors, gap_for_walkins, estimated_visit_duration
-- ================================================================
CREATE TABLE parameter_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    park_id INT NOT NULL,
    parameter_name ENUM('max_visitors', 'gap_for_walkins', 'estimated_visit_duration') NOT NULL,
    old_value DOUBLE NOT NULL,
    new_value DOUBLE NOT NULL,
    status ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending',
    requested_by INT NOT NULL,                    -- park_manager
    approved_by INT,                              -- department_manager
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (park_id) REFERENCES parks(park_id),
    FOREIGN KEY (requested_by) REFERENCES employees(employee_id),
    FOREIGN KEY (approved_by) REFERENCES employees(employee_id)
);

-- ================================================================
-- 10. NOTIFICATIONS - Email/SMS simulation log (הודעות)
-- ================================================================
CREATE TABLE notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT,
    recipient_email VARCHAR(100),
    recipient_phone VARCHAR(20),
    notification_type ENUM(
        'booking_confirmation',                   -- אישור הזמנה
        'booking_cancellation',                   -- ביטול הזמנה
        'reminder',                               -- תזכורת יום לפני
        'reminder_expired',                       -- הזמנה בוטלה - לא אישר תזכורת
        'waitlist_available',                      -- התפנה מקום
        'waitlist_expired'                        -- פג תוקף המתנה
    ) NOT NULL,
    message_text TEXT,
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- ================================================================
-- 11. REPORTS - Generated reports (דוחות)
-- ================================================================
CREATE TABLE reports (
    report_id INT AUTO_INCREMENT PRIMARY KEY,
    park_id INT,                                  -- NULL for department-wide reports
    report_type ENUM(
        'visits',                                 -- דוח ביקורים - entry times and stay duration
        'cancellations',                          -- דוח ביטולים
        'total_visitors',                         -- דוח מספר מבקרים כולל
        'usage'                                   -- דוח שימוש - when park was not full
    ) NOT NULL,
    generated_by INT NOT NULL,
    report_month INT,                             -- חודש הדוח
    report_year INT,                              -- שנת הדוח
    start_date DATE,
    end_date DATE,
    report_data TEXT,                             -- JSON data for the report
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (park_id) REFERENCES parks(park_id),
    FOREIGN KEY (generated_by) REFERENCES employees(employee_id)
);


-- ================================================================
-- INSERT SAMPLE DATA
-- ================================================================

-- Parks
INSERT INTO parks (park_name, max_visitors, gap_for_walkins, estimated_visit_duration, full_price) VALUES
('Banias Nature Reserve', 500, 50, 4.0, 50.0),
('Ein Gedi Nature Reserve', 400, 40, 4.0, 50.0),
('Masada National Park', 600, 60, 4.0, 50.0);

-- Department Manager (no park)
INSERT INTO employees VALUES
(1001, 'David', 'Cohen', 'david.cohen@gonature.com', 'department_manager', NULL, 'david_dm', 'password123', FALSE);

-- Park Managers
INSERT INTO employees VALUES
(2001, 'Sarah', 'Levi', 'sarah.levi@gonature.com', 'park_manager', 1, 'sarah_pm1', 'password123', FALSE),
(2002, 'Moshe', 'Ben-Ari', 'moshe.ba@gonature.com', 'park_manager', 2, 'moshe_pm2', 'password123', FALSE),
(2003, 'Yael', 'Shapira', 'yael.s@gonature.com', 'park_manager', 3, 'yael_pm3', 'password123', FALSE);

-- Park Workers
INSERT INTO employees VALUES
(3001, 'Avi', 'Katz', 'avi.k@gonature.com', 'park_worker', 1, 'avi_pw1', 'password123', FALSE),
(3002, 'Dana', 'Mizrahi', 'dana.m@gonature.com', 'park_worker', 2, 'dana_pw2', 'password123', FALSE),
(3003, 'Ron', 'Peretz', 'ron.p@gonature.com', 'park_worker', 3, 'ron_pw3', 'password123', FALSE);

-- Service Representatives (HQ)
INSERT INTO employees VALUES
(4001, 'Noa', 'Goldberg', 'noa.g@gonature.com', 'service_rep', NULL, 'noa_sr', 'password123', FALSE);

-- Subscribers (Family club members)
INSERT INTO subscribers (id_number, first_name, last_name, phone, email, family_members, credit_card) VALUES
('123456789', 'Eli', 'Avraham', '050-1234567', 'eli.a@gmail.com', 4, '4580-1234-5678-9012'),
('234567890', 'Tamar', 'Friedman', '052-2345678', 'tamar.f@gmail.com', 3, NULL),
('345678901', 'Oren', 'Dayan', '054-3456789', 'oren.d@gmail.com', 5, '4580-9876-5432-1098');

-- Guides
INSERT INTO guides (id_number, first_name, last_name, email, phone) VALUES
('111222333', 'Amir', 'Yosef', 'amir.y@guides.com', '050-1112223'),
('222333444', 'Liat', 'Baruch', 'liat.b@guides.com', '052-2223334');

-- Sample Orders
INSERT INTO orders (visitor_id, park_id, visit_date, visit_time, num_visitors, email, phone, order_type, status, confirmation_code) VALUES
('123456789', 1, '2026-06-01', '09:00:00', 4, 'eli.a@gmail.com', '050-1234567', 'family', 'confirmed', 'CONF-10001'),
('234567890', 2, '2026-06-01', '10:00:00', 3, 'tamar.f@gmail.com', '052-2345678', 'individual', 'confirmed', 'CONF-10002'),
('111222333', 1, '2026-06-02', '08:30:00', 12, 'amir.y@guides.com', '050-1112223', 'organized_group', 'confirmed', 'CONF-10003'),
('999888777', 3, '2026-06-03', '11:00:00', 2, 'tourist@gmail.com', '053-9998887', 'individual', 'pending', NULL);


-- ================================================================
-- PRICING REFERENCE (implemented in server logic, not a table)
-- ================================================================
-- 1. Individual/Family - reserved:     15% discount from full price
-- 2. Individual/Family - walk-in:      Full price (no discount)
-- 3. Organized group - reserved:       25% discount. Extra 12% if paid in advance. Guide free.
-- 4. Organized group - walk-in:        10% discount. Guide pays.
-- 5. Subscriber rate:                  Extra 10% discount on top of other discounts.
-- Promotions:                          Additional discount set by park manager (needs dept manager approval).


SELECT 'GoNature database created successfully!' AS status;
SELECT CONCAT(COUNT(*), ' parks') AS parks FROM parks;
SELECT CONCAT(COUNT(*), ' employees') AS employees FROM employees;
SELECT CONCAT(COUNT(*), ' subscribers') AS subscribers FROM subscribers;
SELECT CONCAT(COUNT(*), ' guides') AS guides FROM guides;
SELECT CONCAT(COUNT(*), ' orders') AS orders FROM orders;
