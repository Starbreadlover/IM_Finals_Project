-- =================================================================
-- IT Help Desk Ticketing System - FINAL DATABASE SCRIPT
-- Class 4-310 | Group 3 | CS 2137
--
-- Creates the it_helpdesk database from scratch: 12 tables with their
-- keys and constraints, the reference data, and sample data for testing
-- and the demonstration.
--
-- WARNING: this drops and recreates the it_helpdesk database.
--
-- Run with:   mysql -u root -p < sql/database.sql
-- or open it in MySQL Workbench and click Execute (lightning bolt).
--
-- Sample-data dates are relative to NOW(), so "this month" and
-- "days open" in the reports always have results.
-- =================================================================

DROP DATABASE IF EXISTS it_helpdesk;
CREATE DATABASE it_helpdesk;
USE it_helpdesk;

-- ---------------------------------------------------------------
-- TABLES
-- ---------------------------------------------------------------

CREATE TABLE department (
    department_id    INT AUTO_INCREMENT PRIMARY KEY,
    department_name  VARCHAR(100) NOT NULL UNIQUE,
    location         VARCHAR(100)
);

CREATE TABLE support_team (
    team_id      INT AUTO_INCREMENT PRIMARY KEY,
    team_name    VARCHAR(100) NOT NULL UNIQUE,
    description  VARCHAR(255)
);

CREATE TABLE priority (
    priority_id          INT AUTO_INCREMENT PRIMARY KEY,
    priority_name        VARCHAR(30) NOT NULL UNIQUE,
    response_time_hrs    INT NOT NULL,
    resolution_time_hrs  INT NOT NULL
);

CREATE TABLE ticket_status (
    status_id    INT AUTO_INCREMENT PRIMARY KEY,
    status_name  VARCHAR(30) NOT NULL UNIQUE,
    description  VARCHAR(255)
);

CREATE TABLE asset (
    asset_id       INT AUTO_INCREMENT PRIMARY KEY,
    asset_tag      VARCHAR(30) NOT NULL UNIQUE,
    asset_type     VARCHAR(50) NOT NULL,
    brand          VARCHAR(50),
    model          VARCHAR(80),
    serial_number  VARCHAR(60),
    location       VARCHAR(100),
    asset_status   ENUM('In Use', 'In Storage', 'Under Repair', 'Retired') NOT NULL DEFAULT 'In Use'
);

CREATE TABLE category (
    category_id    INT AUTO_INCREMENT PRIMARY KEY,
    category_name  VARCHAR(100) NOT NULL UNIQUE,
    description    VARCHAR(255),
    team_id        INT NOT NULL,
    FOREIGN KEY (team_id) REFERENCES support_team (team_id)
);

CREATE TABLE user_account (
    user_id         INT AUTO_INCREMENT PRIMARY KEY,
    first_name      VARCHAR(50) NOT NULL,
    last_name       VARCHAR(50) NOT NULL,
    email           VARCHAR(100) NOT NULL UNIQUE,
    contact_number  VARCHAR(20),
    password_hash   VARCHAR(255) NOT NULL,
    role            ENUM('Requester', 'Technician', 'Administrator') NOT NULL DEFAULT 'Requester',
    is_active       BOOLEAN NOT NULL DEFAULT 1,
    date_created    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    department_id   INT NOT NULL,
    team_id         INT,
    FOREIGN KEY (department_id) REFERENCES department (department_id),
    FOREIGN KEY (team_id) REFERENCES support_team (team_id)
);

CREATE TABLE ticket (
    ticket_id      INT AUTO_INCREMENT PRIMARY KEY,
    subject        VARCHAR(150) NOT NULL,
    description    TEXT NOT NULL,
    date_created   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_updated   DATETIME,
    date_resolved  DATETIME,
    date_closed    DATETIME,
    requester_id   INT NOT NULL,
    technician_id  INT,
    category_id    INT NOT NULL,
    priority_id    INT NOT NULL,
    status_id      INT NOT NULL,
    asset_id       INT,
    FOREIGN KEY (requester_id)  REFERENCES user_account (user_id),
    FOREIGN KEY (technician_id) REFERENCES user_account (user_id),
    FOREIGN KEY (category_id)   REFERENCES category (category_id),
    FOREIGN KEY (priority_id)   REFERENCES priority (priority_id),
    FOREIGN KEY (status_id)     REFERENCES ticket_status (status_id),
    FOREIGN KEY (asset_id)      REFERENCES asset (asset_id)
);

CREATE TABLE ticket_comment (
    comment_id    INT AUTO_INCREMENT PRIMARY KEY,
    comment_text  TEXT NOT NULL,
    is_internal   BOOLEAN NOT NULL DEFAULT 0,
    date_posted   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ticket_id     INT NOT NULL,
    user_id       INT NOT NULL,
    FOREIGN KEY (ticket_id) REFERENCES ticket (ticket_id),
    FOREIGN KEY (user_id)   REFERENCES user_account (user_id)
);

CREATE TABLE attachment (
    attachment_id  INT AUTO_INCREMENT PRIMARY KEY,
    file_name      VARCHAR(255) NOT NULL,
    file_type      VARCHAR(100) NOT NULL,
    file_size_kb   INT NOT NULL,
    file_path      VARCHAR(500) NOT NULL,
    date_uploaded  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ticket_id      INT NOT NULL,
    FOREIGN KEY (ticket_id) REFERENCES ticket (ticket_id)
);

CREATE TABLE ticket_history (
    history_id    INT AUTO_INCREMENT PRIMARY KEY,
    date_changed  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks       VARCHAR(255),
    ticket_id     INT NOT NULL,
    status_id     INT NOT NULL,
    changed_by    INT NOT NULL,
    FOREIGN KEY (ticket_id)  REFERENCES ticket (ticket_id),
    FOREIGN KEY (status_id)  REFERENCES ticket_status (status_id),
    FOREIGN KEY (changed_by) REFERENCES user_account (user_id)
);

CREATE TABLE feedback (
    feedback_id     INT AUTO_INCREMENT PRIMARY KEY,
    rating          TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments        VARCHAR(500),
    date_submitted  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ticket_id       INT NOT NULL UNIQUE,
    FOREIGN KEY (ticket_id) REFERENCES ticket (ticket_id)
);

-- ---------------------------------------------------------------
-- REFERENCE DATA AND TECHNICIANS
-- ---------------------------------------------------------------

INSERT INTO department (department_name, location) VALUES
    ('Information Technology', '3rd Floor'),
    ('Human Resources', '2nd Floor'),
    ('Finance', '2nd Floor');

INSERT INTO support_team (team_name, description) VALUES
    ('Hardware Support', 'Computers and printers'),
    ('Network Support', 'Network and VPN'),
    ('Software Support', 'Software and accounts');

INSERT INTO category (category_name, team_id) VALUES
    ('Computer Hardware', 1),
    ('Printer Issues', 1),
    ('Network Connectivity', 2),
    ('Software Installation', 3);

INSERT INTO priority (priority_name, response_time_hrs, resolution_time_hrs) VALUES
    ('Critical', 1, 4),
    ('High', 2, 8),
    ('Medium', 4, 24),
    ('Low', 8, 72);

INSERT INTO ticket_status (status_name, description) VALUES
    ('Open', 'Waiting to be assigned'),
    ('Assigned', 'Assigned to a technician'),
    ('In Progress', 'Technician is working on it'),
    ('Resolved', 'Fixed, waiting for requester to confirm'),
    ('Closed', 'Requester confirmed the fix'),
    ('Reopened', 'Problem came back');

INSERT INTO user_account (first_name, last_name, email, password_hash, role, department_id, team_id) VALUES
    ('Jose', 'Ramirez', 'jose@example.com', 'hash2', 'Technician', 1, 1),
    ('Miguel', 'Torres', 'miguel@example.com', 'hash3', 'Technician', 1, 2),
    ('Paolo', 'Navarro', 'paolo@example.com', 'hash4', 'Technician', 1, 3);

-- ---------------------------------------------------------------
-- SAMPLE DATA (users 4-9, assets 1-7, tickets 1-12)
-- ---------------------------------------------------------------
INSERT INTO user_account (first_name, last_name, email, contact_number, password_hash, role, department_id, team_id) VALUES
    ('Ana',   'Santos', 'ana@example.com',   '09171234501', 'hash5', 'Requester',     2, NULL),  -- 4
    ('Ben',   'Cruz',   'ben@example.com',   '09171234502', 'hash6', 'Requester',     3, NULL),  -- 5
    ('Carla', 'Reyes',  'carla@example.com', '09171234503', 'hash7', 'Requester',     2, NULL),  -- 6
    ('Dan',   'Lim',    'dan@example.com',   '09171234504', 'hash8', 'Requester',     3, NULL),  -- 7
    ('Ella',  'Garcia', 'ella@example.com',  '09171234505', 'hash9', 'Administrator', 1, NULL),  -- 8
    ('Felix', 'Uy',     'felix@example.com', '09171234506', 'hash10','Requester',     1, NULL);  -- 9

-- ---------------------------------------------------------------
-- ASSETS (ids 1-7). LT-003 and MN-001 never get a ticket (query 4).
-- ---------------------------------------------------------------
INSERT INTO asset (asset_tag, asset_type, brand, model, serial_number, location, asset_status) VALUES
    ('LT-001', 'Laptop',  'Dell',    'Latitude 5440',  'DL5440-001', 'HR Office',      'In Use'),        -- 1
    ('LT-002', 'Laptop',  'Lenovo',  'ThinkPad E14',   'LN-E14-002', 'Finance Office', 'In Use'),        -- 2
    ('PR-001', 'Printer', 'HP',      'LaserJet M404',  'HP-M404-001','2nd Floor',      'In Use'),        -- 3
    ('PC-001', 'Desktop', 'Acer',    'Veriton X',      'AC-VX-001',  'HR Office',      'Under Repair'),  -- 4
    ('RT-001', 'Router',  'Cisco',   'ISR 1100',       'CS-1100-001','Server Room',    'In Use'),        -- 5
    ('LT-003', 'Laptop',  'HP',      'ProBook 440',    'HP-PB-003',  'IT Storage',     'In Storage'),    -- 6
    ('MN-001', 'Monitor', 'Samsung', 'S24R350',        'SM-S24-001', 'IT Storage',     'In Storage');    -- 7

-- ---------------------------------------------------------------
-- TICKETS (ids 1-12)
-- ---------------------------------------------------------------
INSERT INTO ticket (subject, description, date_created, date_updated, date_resolved, date_closed,
                    requester_id, technician_id, category_id, priority_id, status_id, asset_id) VALUES
    -- 1: Closed, High, resolved in 6h (on time)
    ('Laptop will not turn on', 'Power light blinks but the screen stays black.',
     NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 40 DAY + INTERVAL 30 HOUR,
     NOW() - INTERVAL 40 DAY + INTERVAL 6 HOUR, NOW() - INTERVAL 40 DAY + INTERVAL 30 HOUR,
     4, 1, 1, 2, 5, 1),
    -- 2: Closed, Medium, resolved in 30h (missed 24h target)
    ('Printer jams on every print', 'Paper jams in tray 2 every time.',
     NOW() - INTERVAL 35 DAY, NOW() - INTERVAL 35 DAY + INTERVAL 50 HOUR,
     NOW() - INTERVAL 35 DAY + INTERVAL 30 HOUR, NOW() - INTERVAL 35 DAY + INTERVAL 50 HOUR,
     5, 1, 2, 3, 5, 3),
    -- 3: Closed, Critical, resolved in 3h (on time)
    ('No internet on 2nd floor', 'Whole floor lost connection after a power outage.',
     NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY + INTERVAL 24 HOUR,
     NOW() - INTERVAL 20 DAY + INTERVAL 3 HOUR, NOW() - INTERVAL 20 DAY + INTERVAL 24 HOUR,
     6, 2, 3, 1, 5, 5),
    -- 4: Resolved, Low, resolved in 80h (missed 72h target)
    ('Install accounting software', 'Need the new accounting package on my laptop.',
     NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY + INTERVAL 80 HOUR,
     NOW() - INTERVAL 12 DAY + INTERVAL 80 HOUR, NULL,
     5, 3, 4, 4, 4, NULL),
    -- 5: In Progress, High (overdue)
    ('VPN keeps disconnecting', 'VPN drops every 10 minutes when working from home.',
     NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 8 DAY, NULL, NULL,
     7, 2, 3, 2, 3, 2),
    -- 6: Assigned, Medium (overdue)
    ('Desktop is very slow', 'Takes 10 minutes to start and programs freeze.',
     NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY, NULL, NULL,
     4, 1, 1, 3, 2, 4),
    -- 7: Resolved, Low, resolved in 20h (on time)
    ('Need MS Office license', 'Office says my license has expired.',
     NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY + INTERVAL 20 HOUR,
     NOW() - INTERVAL 5 DAY + INTERVAL 20 HOUR, NULL,
     6, 3, 4, 4, 4, NULL),
    -- 8: Open, unassigned, Medium (overdue)
    ('Printer shows offline', 'The 2nd floor printer shows offline on all PCs.',
     NOW() - INTERVAL 3 DAY, NULL, NULL, NULL,
     4, NULL, 2, 3, 1, 3),
    -- 9: Open, unassigned, new
    ('Email not syncing on phone', 'Work email stopped syncing on my phone.',
     NOW() - INTERVAL 2 HOUR, NULL, NULL, NULL,
     7, NULL, 4, 3, 1, NULL),
    -- 10: Reopened, High (overdue)
    ('Wi-Fi drops in meeting room', 'Fixed last week but it is dropping again.',
     NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 2 DAY, NULL, NULL,
     5, 2, 3, 2, 6, NULL),
    -- 11: Open, unassigned, new
    ('Monitor flickering', 'Second monitor flickers when the laptop is docked.',
     NOW() - INTERVAL 1 HOUR, NULL, NULL, NULL,
     4, NULL, 1, 4, 1, NULL),
    -- 12: In Progress, Low
    ('Keyboard keys stuck', 'Some keys on my keyboard do not respond.',
     NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 3 HOUR, NULL, NULL,
     9, 1, 1, 4, 3, NULL);

-- ---------------------------------------------------------------
-- TICKET HISTORY (every status change is logged, BR11)
-- changed_by 8 = Ella Garcia (Administrator)
-- ---------------------------------------------------------------
INSERT INTO ticket_history (date_changed, remarks, ticket_id, status_id, changed_by) VALUES
    (NOW() - INTERVAL 40 DAY,                         'Ticket submitted',         1, 1, 4),
    (NOW() - INTERVAL 40 DAY + INTERVAL 1 HOUR,       'Assigned to technician',   1, 2, 8),
    (NOW() - INTERVAL 40 DAY + INTERVAL 6 HOUR,       'Replaced power adapter',   1, 4, 1),
    (NOW() - INTERVAL 40 DAY + INTERVAL 30 HOUR,      'Requester confirmed',      1, 5, 4),

    (NOW() - INTERVAL 35 DAY,                         'Ticket submitted',         2, 1, 5),
    (NOW() - INTERVAL 35 DAY + INTERVAL 2 HOUR,       'Assigned to technician',   2, 2, 8),
    (NOW() - INTERVAL 35 DAY + INTERVAL 30 HOUR,      'Replaced pickup roller',   2, 4, 1),
    (NOW() - INTERVAL 35 DAY + INTERVAL 50 HOUR,      'Requester confirmed',      2, 5, 5),

    (NOW() - INTERVAL 20 DAY,                         'Ticket submitted',         3, 1, 6),
    (NOW() - INTERVAL 20 DAY + INTERVAL 15 MINUTE,    'Assigned to technician',   3, 2, 8),
    (NOW() - INTERVAL 20 DAY + INTERVAL 3 HOUR,       'Router restarted and reconfigured', 3, 4, 2),
    (NOW() - INTERVAL 20 DAY + INTERVAL 24 HOUR,      'Requester confirmed',      3, 5, 6),

    (NOW() - INTERVAL 12 DAY,                         'Ticket submitted',         4, 1, 5),
    (NOW() - INTERVAL 12 DAY + INTERVAL 5 HOUR,       'Assigned to technician',   4, 2, 8),
    (NOW() - INTERVAL 12 DAY + INTERVAL 80 HOUR,      'Software installed',       4, 4, 3),

    (NOW() - INTERVAL 9 DAY,                          'Ticket submitted',         5, 1, 7),
    (NOW() - INTERVAL 9 DAY + INTERVAL 1 HOUR,        'Assigned to technician',   5, 2, 8),
    (NOW() - INTERVAL 8 DAY,                          'Checking VPN logs',        5, 3, 2),

    (NOW() - INTERVAL 6 DAY,                          'Ticket submitted',         6, 1, 4),
    (NOW() - INTERVAL 5 DAY,                          'Assigned to technician',   6, 2, 8),

    (NOW() - INTERVAL 5 DAY,                          'Ticket submitted',         7, 1, 6),
    (NOW() - INTERVAL 5 DAY + INTERVAL 2 HOUR,        'Assigned to technician',   7, 2, 8),
    (NOW() - INTERVAL 5 DAY + INTERVAL 20 HOUR,       'License renewed',          7, 4, 3),

    (NOW() - INTERVAL 3 DAY,                          'Ticket submitted',         8, 1, 4),
    (NOW() - INTERVAL 2 HOUR,                         'Ticket submitted',         9, 1, 7),

    (NOW() - INTERVAL 15 DAY,                         'Ticket submitted',        10, 1, 5),
    (NOW() - INTERVAL 15 DAY + INTERVAL 1 HOUR,       'Assigned to technician',  10, 2, 8),
    (NOW() - INTERVAL 14 DAY,                         'Access point restarted',  10, 4, 2),
    (NOW() - INTERVAL 2 DAY,                          'Problem came back',       10, 6, 5),

    (NOW() - INTERVAL 1 HOUR,                         'Ticket submitted',        11, 1, 4),

    (NOW() - INTERVAL 4 HOUR,                         'Ticket submitted',        12, 1, 9),
    (NOW() - INTERVAL 3 HOUR + INTERVAL -30 MINUTE,   'Assigned to technician',  12, 2, 8),
    (NOW() - INTERVAL 3 HOUR,                         'Ordering replacement keyboard', 12, 3, 1);

-- ---------------------------------------------------------------
-- COMMENTS
-- ---------------------------------------------------------------
INSERT INTO ticket_comment (comment_text, is_internal, date_posted, ticket_id, user_id) VALUES
    ('Laptop works now, thank you!',                     0, NOW() - INTERVAL 40 DAY + INTERVAL 29 HOUR, 1, 4),
    ('Pickup roller was worn out. Ordered spares.',      1, NOW() - INTERVAL 35 DAY + INTERVAL 30 HOUR, 2, 1),
    ('VPN client updated, still monitoring.',            0, NOW() - INTERVAL 8 DAY,                     5, 2),
    ('Same issue on Tuesday afternoon.',                 0, NOW() - INTERVAL 2 DAY,                    10, 5),
    ('Possibly interference from the new projector.',    1, NOW() - INTERVAL 1 DAY,                    10, 2);

-- ---------------------------------------------------------------
-- FEEDBACK (BR12: at most one per ticket, rating 1-5, after resolution)
-- ---------------------------------------------------------------
INSERT INTO feedback (rating, comments, date_submitted, ticket_id) VALUES
    (5, 'Fixed quickly, thank you!',                   NOW() - INTERVAL 40 DAY + INTERVAL 30 HOUR, 1),
    (3, 'Took longer than expected but it works now.', NOW() - INTERVAL 35 DAY + INTERVAL 50 HOUR, 2),
    (5, 'Very fast response for the whole floor.',     NOW() - INTERVAL 20 DAY + INTERVAL 24 HOUR, 3);

-- ---------------------------------------------------------------
-- ATTACHMENTS (BR10: files stay on the server; only their details are stored)
-- ---------------------------------------------------------------
INSERT INTO attachment (file_name, file_type, file_size_kb, file_path, date_uploaded, ticket_id) VALUES
    ('printer_error.jpg', 'image/jpeg',       245, '/uploads/tickets/2/printer_error.jpg', NOW() - INTERVAL 35 DAY, 2),
    ('vpn_log.txt',       'text/plain',        12, '/uploads/tickets/5/vpn_log.txt',       NOW() - INTERVAL 9 DAY,  5),
    ('offline_screen.png','image/png',        310, '/uploads/tickets/8/offline_screen.png',NOW() - INTERVAL 3 DAY,  8);

-- ---------------------------------------------------------------
-- CHECK
-- ---------------------------------------------------------------
SELECT 'department' AS table_name, COUNT(*) AS row_count FROM department
UNION ALL SELECT 'support_team',   COUNT(*) FROM support_team
UNION ALL SELECT 'category',       COUNT(*) FROM category
UNION ALL SELECT 'priority',       COUNT(*) FROM priority
UNION ALL SELECT 'ticket_status',  COUNT(*) FROM ticket_status
UNION ALL SELECT 'user_account',   COUNT(*) FROM user_account
UNION ALL SELECT 'asset',          COUNT(*) FROM asset
UNION ALL SELECT 'ticket',         COUNT(*) FROM ticket
UNION ALL SELECT 'ticket_history', COUNT(*) FROM ticket_history
UNION ALL SELECT 'ticket_comment', COUNT(*) FROM ticket_comment
UNION ALL SELECT 'attachment',     COUNT(*) FROM attachment
UNION ALL SELECT 'feedback',       COUNT(*) FROM feedback;
