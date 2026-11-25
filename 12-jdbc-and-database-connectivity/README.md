# 12. JDBC & Database Connectivity

> **Learning Method:** Start at **🟢 Noob Track** (`00-beginner-jdbc-first-steps.md`), practice the **🟡 10 Production Problems** (`problems/` + `io/`), and master **🔴 Senior Track** (`JDBC_COMPREHENSIVE_GUIDE.md` + Connection Pooling, Savepoint Transactions, SQL Injection, and Batch Benchmarking).

---

## ⚡ 2-Minute Rapid Revision Card

- **4 Driver Types:** Modern apps exclusively use **Type 4 Pure Java Thin Driver** (e.g., `mysql-connector-j-9.6.0.jar`), converting JDBC calls directly to the DB's TCP wire protocol. `Class.forName(...)` is optional since JDBC 4.0 (`ServiceLoader`).
- **`Statement` vs `PreparedStatement`:** ALWAYS use `PreparedStatement`! Prevents **SQL Injection** (strictly separates SQL grammar from data bindings) and enables **DB execution-plan caching**.
- **Transactions:** Call `conn.setAutoCommit(false)`, run multiple statements, `conn.commit()` on success, `conn.rollback()` inside `catch`, and restore `autoCommit` in `finally` (or `Savepoint` for partial rollback).
- **High-Throughput Batching:** `ps.addBatch()` + `ps.executeBatch()` in chunks (e.g., 500–1,000 rows) with `rewriteBatchedStatements=true` cuts network round-trips dramatically.
- **Resource Hygiene:** Always close `Connection`, `PreparedStatement`, and `ResultSet` using **try-with-resources** — leaked connections exhaust HikariCP pools instantly!

---

## 📂 Contents

| Resource | Link |
|---|---|
| **🟢 Beginner Guide** | [`notes/00-beginner-jdbc-first-steps.md`](notes/00-beginner-jdbc-first-steps.md) |
| **🔴 Senior Guide** | [`notes/01-jdbc-comprehensive-guide.md`](notes/01-jdbc-comprehensive-guide.md) |
| **🗃️ SQL Schema & Seed Data** | [`schema.sql`](schema.sql) & [`data/`](data/) |
| **💻 Code Labs** | [`codes/jdbc/`](codes/jdbc/) (`config`, `models`, `practice`, `problems` 01–10, `io`, `advanced`) |
| **📦 MySQL Driver** | [`lib/mysql-connector-j-9.6.0.jar`](lib/mysql-connector-j-9.6.0.jar) |
| **🎯 Interview Questions** | [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) |

---

## 🚀 Compile & Run

```bash
# 1. Seed MySQL database (requires local MySQL on port 3306)
mysql -u root -p < 12-jdbc-and-database-connectivity/schema.sql

# 2. Compile all JDBC classes cleanly
javac -cp "12-jdbc-and-database-connectivity/lib/mysql-connector-j-9.6.0.jar" \
      -d out $(find 12-jdbc-and-database-connectivity/codes -name "*.java")

# 3. Run labs (set DB_PASSWORD env var if not 'root')
java -cp "out:12-jdbc-and-database-connectivity/lib/mysql-connector-j-9.6.0.jar" jdbc.config.DatabaseConnection
java -cp "out:12-jdbc-and-database-connectivity/lib/mysql-connector-j-9.6.0.jar" jdbc.problems.Problem07_TransactionHandling
java -cp "out:12-jdbc-and-database-connectivity/lib/mysql-connector-j-9.6.0.jar" jdbc.advanced.SimpleConnectionPool
```
