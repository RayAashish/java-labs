# 12. JDBC & Database Connectivity — Interview Questions & Answers

---

## 🟢 Beginner Questions

### Q1. What are the 5 core interfaces/classes of JDBC?
`DriverManager` / `DataSource`, `Connection`, `Statement` / `PreparedStatement` / `CallableStatement`, `ResultSet`, and `SQLException`.

### Q2. `executeQuery()` vs `executeUpdate()` vs `execute()`?
- `executeQuery()`: `SELECT` statements -> returns `ResultSet`.
- `executeUpdate()`: `INSERT`, `UPDATE`, `DELETE`, DDL -> returns `int` affected row count.
- `execute()`: Unknown dynamic SQL -> returns `boolean` (`true` if first result is a `ResultSet`, `false` if update count).

### Q3. Why is `Class.forName("com.mysql.cj.jdbc.Driver")` no longer required?
Since **JDBC 4.0 (Java 6+)**, `DriverManager` uses Java's `ServiceLoader` to auto-discover drivers declared inside `META-INF/services/java.sql.Driver` on the classpath.

---

## 🟡 Intermediate Questions

### Q4. `Statement` vs `PreparedStatement` vs `CallableStatement`?
- **`Statement`:** Concatenates raw SQL strings -> **vulnerable to SQL Injection** and parses/plans SQL every time.
- **`PreparedStatement`:** Pre-compiled parameterized SQL (`?`) -> **prevents SQL Injection** by strictly separating SQL syntax from data bindings, enables **database execution-plan caching**, and supports batching cleanly.
- **`CallableStatement`:** Executes database **Stored Procedures** (`{call proc(?, ?)}`) supporting `IN`, `OUT`, and `INOUT` parameters.

### Q5. How do you retrieve an auto-generated primary key (`AUTO_INCREMENT`) after an `INSERT`?
Pass `Statement.RETURN_GENERATED_KEYS` when preparing the statement (`conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)`), call `executeUpdate()`, and read `try (ResultSet keys = ps.getGeneratedKeys())`.

### Q6. How do you handle JDBC Transactions (`commit`, `rollback`, `Savepoint`) cleanly?
1. Call `conn.setAutoCommit(false)` (disables single-statement auto-commit).
2. Execute multiple SQL operations.
3. Call `conn.commit()` on full success.
4. Inside `catch (SQLException e)`, call `conn.rollback()` (or `conn.rollback(savepoint)` for partial rollback!).
5. Always restore `conn.setAutoCommit(true)` in `finally` before returning a pooled connection!

### Q7. How does Batch Processing (`addBatch` / `executeBatch`) improve bulk insert speed?
Instead of sending 10,000 separate network round-trips (each incurring latency RTT + commit disk fsync), `ps.addBatch()` buffers parameter sets and `ps.executeBatch()` sends them in bulk chunks (e.g. 500–1,000 rows per batch; with MySQL `rewriteBatchedStatements=true`, multiple `INSERT`s rewrite into one multi-row insert!).

---

## 🔴 Senior / Production Database Engineering Questions

### Q8. `DriverManager` vs `DataSource` & Connection Pooling (HikariCP) — why does pooling matter?
Opening a fresh TCP + TLS + MySQL Auth handshake takes **50–250 ms** and burns DB server memory/threads. `javax.sql.DataSource` backed by a **Connection Pool** (like **HikariCP**) maintains pre-warmed reusable connections; calling `conn.close()` returns the logical proxy connection to the pool in microseconds rather than tearing down the TCP socket!

### Q9. How do you stream 5,000,000 database rows in JDBC without crashing with `OutOfMemoryError`?
By default, MySQL Connector/J buffers the **entire** `ResultSet` in client JVM Heap before returning from `executeQuery()`! To stream row-by-row:
- Use `Statement.TYPE_FORWARD_ONLY`, `ResultSet.CONCUR_READ_ONLY`, and call `ps.setFetchSize(Integer.MIN_VALUE)` (for MySQL streaming) or `ps.setFetchSize(1000)` + `useCursorFetch=true` (for PostgreSQL/MySQL cursor batching, ensuring `autoCommit=false` on PostgreSQL!).

### Q10. Explain the 4 ANSI SQL Transaction Isolation Levels & Anomalies.
| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read |
|---|---|---|---|
| `READ_UNCOMMITTED` | Possible | Possible | Possible |
| `READ_COMMITTED` *(PostgreSQL/Oracle default)* | Prevented | Possible | Possible |
| `REPEATABLE_READ` *(MySQL InnoDB default)* | Prevented | Prevented | Prevented in InnoDB via Gap Locks (MVCC) |
| `SERIALIZABLE` | Prevented | Prevented | Prevented (max lock contention) |

### Q11. What happens if you forget to close a `ResultSet` or `Statement` inside a pooled `Connection`?
Even though `HikariCP` proxies `conn.close()`, unclosed `PreparedStatement`s and `ResultSet`s created on long-lived pooled connections leak server-side database cursors and memory until `connection.close()` cleans them up (or the pool kills the connection for leak detection). Always wrap `Connection`, `PreparedStatement`, and `ResultSet` in `try-with-resources`!

### Q12. Compare JDBC vs JPA/Hibernate — why do senior engineers still need raw JDBC knowledge?
JPA/Hibernate automates CRUD and dirty-checking, **but** struggles with bulk ETL ingestion (1M rows), complex analytical reporting queries, and hides **N+1 SELECT queries** and unexpected batch-disabling when using `GenerationType.IDENTITY`. Spring `JdbcTemplate` / `JdbcClient` (Java 21/Spring 6.1) shines for predictable, high-throughput SQL pipelines!
