# 00. JDBC for Beginners — Talking to a Database in 5 Steps

---

## 🟢 The 5-Step JDBC Recipe

Every JDBC program follows the exact same 5 steps:
1. **Open Connection** (`DriverManager.getConnection(url, user, password)` or `DataSource.getConnection()`)
2. **Prepare SQL** (`conn.prepareStatement("SELECT id, name FROM employees WHERE age > ?")`)
3. **Bind Parameters** (`ps.setInt(1, 25)` — prevents SQL Injection!)
4. **Execute & Read `ResultSet`** (`try (ResultSet rs = ps.executeQuery()) { while (rs.next()) ... }`)
5. **Auto-Close Everything** via `try-with-resources`!

```java
String sql = "SELECT id, name FROM employees WHERE department = ?";
try (Connection conn = DriverManager.getConnection(url, user, pass);
     PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, "Engineering");
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            System.out.println(rs.getInt("id") + ": " + rs.getString("name"));
        }
    }
}
```

### `executeQuery()` vs `executeUpdate()`
- **`executeQuery()`:** For `SELECT` queries — returns a **`ResultSet`** cursor.
- **`executeUpdate()`:** For `INSERT`, `UPDATE`, `DELETE`, DDL — returns an **`int` row count**.

> Next: Read [`01-jdbc-comprehensive-guide.md`](01-jdbc-comprehensive-guide.md) for Connection Pooling (HikariCP), ACID Transactions, Batching, and Senior tradeoffs!
