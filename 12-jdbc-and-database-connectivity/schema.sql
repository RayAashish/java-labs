-- ============================================================================
-- JDBC LAB: UNIFIED SCHEMA INITIALIZATION SCRIPT
-- ============================================================================
-- This script provisions the 'jdbclab' database and all relational schemas
-- required across the general practice, problems, file I/O, and advanced modules.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS jdbclab;
USE jdbclab;

-- ----------------------------------------------------------------------------
-- 1. Departments Table
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 2. Employees Table (Used in Problems 1-6, 8-10, Practice, and DAO)
-- ----------------------------------------------------------------------------
CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    age INT NOT NULL,
    department_id INT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_department FOREIGN KEY (department_id) 
        REFERENCES departments(id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 3. Accounts Table (Used in Problem 07 & Advanced Transactions)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS accounts;

CREATE TABLE accounts (
    account_number INT PRIMARY KEY,
    holder_name VARCHAR(100) NOT NULL,
    balance DECIMAL(15, 2) NOT NULL,
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 4. Students Table (Preserved & Enhanced from jdbc-with-IO)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS students;

CREATE TABLE students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 5. Laptop Table (Preserved & Enhanced from jdbc-with-IO)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS laptop;

CREATE TABLE laptop (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    ram INT NOT NULL COMMENT 'RAM in GB',
    ssd INT NOT NULL COMMENT 'SSD storage in GB',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
-- SEED DATA
-- ============================================================================

INSERT INTO departments (id, name, location) VALUES
(1, 'Engineering', 'San Francisco'),
(2, 'Human Resources', 'New York'),
(3, 'Finance', 'Chicago');

INSERT INTO employees (name, email, age, department_id) VALUES
('Aashish', 'aashish@example.com', 24, 1),
('Rukiya', 'rukiya@example.com', 22, 1),
('Archana', 'archana@example.com', 23, 2),
('David', 'david@example.com', 52, 2),
('Elena', 'elena@example.com', 58, 3),
('Siddharth', 'siddharth@example.com', 31, 1),
('Priya', 'priya@example.com', 45, 3);

INSERT INTO accounts (account_number, holder_name, balance) VALUES
(123, 'Alice Smith', 50000.00),
(567, 'Bob Jones', 20000.00),
(890, 'Charlie Brown', 15000.00);

-- Initial students and laptops
INSERT INTO students (name, age) VALUES
('Aashish', 23),
('Rukiya', 20),
('Archana', 22);

INSERT INTO laptop (name, ram, ssd) VALUES
('Macbook-Air', 8, 256),
('Asus-Vivobook', 16, 1024),
('Lenovo_IdeaPad', 16, 1024);
