# Unified JDBC Master Laboratory & Problem Suite

A production-grade, consolidated Java Database Connectivity (JDBC) laboratory combining core concepts, the 10 canonical interview/production problems, file I/O integration, and senior-level architectural engineering patterns.

---

## 📂 Directory Layout

```text
jdbc/
├── JDBC_COMPREHENSIVE_GUIDE.md    <-- Detailed architectural notes (Junior vs Senior POV)
├── README.md                      <-- Project orientation and run guide
├── schema.sql                     <-- Database DDL & seed dataset script
├── lib/
│   └── mysql-connector-j-9.6.0.jar<-- MySQL JDBC Driver
├── data/
│   ├── students.txt               <-- Text dataset for File I/O
│   ├── laptop.txt                 <-- Text dataset for Hardware I/O
│   ├── employees.csv              <-- CSV dataset for Batch Ingestion
│   └── exported_employees.csv     <-- Generated output from DatabaseToFileExport
├── config/                        <-- Centralized Database Configuration & Connection Factory
│   ├── DatabaseConfig.java        (package jdbc.config;)
│   └── DatabaseConnection.java    (package jdbc.config;)
├── models/                        <-- Domain Entities
│   ├── Employee.java              (package jdbc.models;)
│   ├── Department.java            (package jdbc.models;)
│   ├── Account.java               (package jdbc.models;)
│   ├── Student.java               (package jdbc.models;)
│   └── Laptop.java                (package jdbc.models;)
├── practice/                      <-- General Practice & Fundamentals
│   ├── JDBCBasics.java            (package jdbc.practice;)
│   ├── CRUDOperations.java        (package jdbc.practice;)
│   └── MetadataExploration.java   (package jdbc.practice;)
├── problems/                      <-- The 10 Focused Industry Problems
│   ├── Problem01_GetAllEmployees.java        (package jdbc.problems;)
│   ├── Problem02_AddEmployee.java            (package jdbc.problems;)
│   ├── Problem03_CreateEmployeeGeneratedKeys.java (package jdbc.problems;)
│   ├── Problem04_UpdateEmployee.java         (package jdbc.problems;)
│   ├── Problem05_DeleteEmployee.java         (package jdbc.problems;)
│   ├── Problem06_FindEmployeesOlderThan.java (package jdbc.problems;)
│   ├── Problem07_TransactionHandling.java    (package jdbc.problems;)
│   ├── Problem08_BatchProcessing.java        (package jdbc.problems;)
│   ├── Problem09_PaginationQuery.java        (package jdbc.problems;)
│   └── Problem10_DAOPattern.java             (package jdbc.problems;)
├── io/                            <-- File I/O + Database Integration
│   ├── FileReaderUtility.java     (package jdbc.io;)
│   ├── FileToDatabaseIngestion.java (package jdbc.io;)
│   └── DatabaseToFileExport.java  (package jdbc.io;)
└── advanced/                      <-- Senior Architectural Patterns
    ├── SimpleConnectionPool.java  (package jdbc.advanced;)
    ├── RobustTransactionManager.java (package jdbc.advanced;)
    └── StatementVsPreparedStatementBenchmark.java (package jdbc.advanced;)
```

---

## 🚀 Quick Start & Execution

### 1. Database Setup
Ensure MySQL is running locally on port `3306`. Initialize the schema and seed data:

```bash
mysql -u root -p < jdbc/schema.sql
```
*(Default password configured is `Raysi@2002` or can be overridden via `DB_PASSWORD` env variable)*.

### 2. Compiling the Entire Project
From the repository root (`java-labs`):

```bash
javac -cp "jdbc/lib/mysql-connector-j-9.6.0.jar:." -d out $(find jdbc -name "*.java")
```

### 3. Running Classes
Execute any class directly using the compiled classpath:

```bash
# Verify Database Connectivity & Metadata
java -cp "out:jdbc/lib/mysql-connector-j-9.6.0.jar" jdbc.config.DatabaseConnection

# General Practice: CRUD Operations
java -cp "out:jdbc/lib/mysql-connector-j-9.6.0.jar" jdbc.practice.CRUDOperations

# Problem 07: ACID Transaction Handling (Commit, Rollback, Savepoints)
java -cp "out:jdbc/lib/mysql-connector-j-9.6.0.jar" jdbc.problems.Problem07_TransactionHandling

# Problem 08: High-Performance Batch Processing (1,000 rows benchmark)
java -cp "out:jdbc/lib/mysql-connector-j-9.6.0.jar" jdbc.problems.Problem08_BatchProcessing

# Problem 10: Complete DAO Architecture
java -cp "out:jdbc/lib/mysql-connector-j-9.6.0.jar" jdbc.problems.Problem10_DAOPattern

# File I/O Ingestion: Parse Text/CSV files into DB
java -cp "out:jdbc/lib/mysql-connector-j-9.6.0.jar" jdbc.io.FileToDatabaseIngestion

# File I/O Export: Stream DB rows to CSV
java -cp "out:jdbc/lib/mysql-connector-j-9.6.0.jar" jdbc.io.DatabaseToFileExport

# Advanced: Connection Pooling Internals
java -cp "out:jdbc/lib/mysql-connector-j-9.6.0.jar" jdbc.advanced.SimpleConnectionPool

# Advanced: Statement vs PreparedStatement (SQL Injection Demo)
java -cp "out:jdbc/lib/mysql-connector-j-9.6.0.jar" jdbc.advanced.StatementVsPreparedStatementBenchmark
```

---

## 📖 In-Depth Architectural Documentation
For exhaustive, production-grade notes covering JDBC internals from both Junior and Senior engineering perspectives, see:
👉 **[`JDBC_COMPREHENSIVE_GUIDE.md`](./JDBC_COMPREHENSIVE_GUIDE.md)**
