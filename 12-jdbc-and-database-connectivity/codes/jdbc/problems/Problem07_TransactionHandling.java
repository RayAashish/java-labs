package jdbc.problems;

import jdbc.config.DatabaseConnection;
import jdbc.models.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * Problem 07: Transaction Handling (HIGH PRIORITY)
 * <p>
 * <b>Goal:</b>
 * Master ACID transaction boundaries in JDBC using manual commit and rollback.
 * <p>
 * <b>Requirements:</b>
 * 1. Atomically transfer funds between two bank accounts.
 * 2. Atomically transfer an employee from one department to another.
 * 3. If any step fails (insufficient balance, network timeout, constraint violation),
 *    execute an explicit {@link Connection#rollback()}.
 * 4. Demonstrate {@link Savepoint} for partial rollbacks.
 * <p>
 * <b>CRITICAL SENIOR PERSPECTIVES:</b>
 * <ul>
 *   <li><b>Default State:</b> JDBC connections default to {@code autoCommit = true}.
 *       Every individual SQL statement is treated as its own independent transaction.</li>
 *   <li><b>Transaction Demarcation:</b> Invoking {@code con.setAutoCommit(false)} starts a
 *       multi-statement transaction block. Locks (row-level or gap locks in InnoDB) are held
 *       until {@code con.commit()} or {@code con.rollback()} is executed.</li>
 *   <li><b>Connection Pooling Safety:</b> ALWAYS restore {@code con.setAutoCommit(true)} in a
 *       finally block before returning the connection to the pool. Otherwise, the next thread
 *       borrowing the pooled connection will unwittingly inherit an uncommitted transaction boundary!</li>
 *   <li><b>Resource Management Bug in Beginner Code:</b> Beginners frequently close the connection
 *       inside the try block ({@code con.close()}) right after commit, causing the finally block's
 *       cleanup or rollback to throw an "Operation not allowed on closed connection" exception!</li>
 * </ul>
 */
public class Problem07_TransactionHandling {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   PROBLEM 07: ACID TRANSACTION HANDLING DEMO     ");
        System.out.println("==================================================");

        printAccountBalances();

        // Scenario A: Successful Transfer
        System.out.println("\n>>> SCENARIO A: Valid Transfer of $5,000 from Acc 123 to Acc 567");
        boolean success1 = transferMoney(123, 567, 5000.00);
        System.out.println("Transfer Success? " + success1);
        printAccountBalances();

        // Scenario B: Failed Transfer (Insufficient Funds -> Rollback)
        System.out.println("\n>>> SCENARIO B: Invalid Transfer of $1,000,000 (Exceeds Balance)");
        boolean success2 = transferMoney(123, 567, 1_000_000.00);
        System.out.println("Transfer Success? " + success2 + " (Expected: false, rolled back cleanly)");
        printAccountBalances();

        // Scenario C: Savepoint Partial Rollback
        System.out.println("\n>>> SCENARIO C: Advanced Partial Rollback using Savepoints");
        demonstrateSavepoints();
        printAccountBalances();

        // Scenario D: Department Transfer (Relational Integrity)
        System.out.println("\n>>> SCENARIO D: Transferring Employee 1 to Department 2");
        boolean deptTransfer = transferEmployeeDepartment(1, 2);
        System.out.println("Department Transfer Success? " + deptTransfer);
    }

    /**
     * Executes an atomic funds transfer between two accounts.
     * Guarantees Atomicity & Consistency: either both accounts update or neither does.
     *
     * @param fromAcc debit account number
     * @param toAcc   credit account number
     * @param amount  amount to transfer
     * @return true if committed, false if rolled back
     */
    public static boolean transferMoney(int fromAcc, int toAcc, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly positive");
        }

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();

            // 1. Disable Auto-Commit to begin manual transaction boundary
            con.setAutoCommit(false);

            // Optional Senior Best Practice: Set appropriate isolation level
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            // 2. Check Sender's current balance (Row Lock: SELECT ... FOR UPDATE)
            String balanceCheckSql = "SELECT balance FROM accounts WHERE account_number = ? FOR UPDATE";
            try (PreparedStatement checkStmt = con.prepareStatement(balanceCheckSql)) {
                checkStmt.setInt(1, fromAcc);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        System.err.println("[TRANSACTION ERROR] Source account does not exist: " + fromAcc);
                        con.rollback();
                        return false;
                    }
                    double currentBalance = rs.getDouble("balance");
                    if (currentBalance < amount) {
                        System.err.printf("[TRANSACTION ERROR] Insufficient balance! Has: $%.2f, Requested: $%.2f%n",
                                currentBalance, amount);
                        // Explicit rollback
                        con.rollback();
                        return false;
                    }
                }
            }

            // 3. Debit Source Account
            String debitSql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
            try (PreparedStatement debitStmt = con.prepareStatement(debitSql)) {
                debitStmt.setDouble(1, amount);
                debitStmt.setInt(2, fromAcc);
                int debitRows = debitStmt.executeUpdate();
                if (debitRows != 1) {
                    System.err.println("[TRANSACTION ERROR] Debit step affected unexpected row count: " + debitRows);
                    con.rollback();
                    return false;
                }
            }

            // 4. Credit Target Account
            String creditSql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
            try (PreparedStatement creditStmt = con.prepareStatement(creditSql)) {
                creditStmt.setDouble(1, amount);
                creditStmt.setInt(2, toAcc);
                int creditRows = creditStmt.executeUpdate();
                if (creditRows != 1) {
                    System.err.println("[TRANSACTION ERROR] Destination account not found: " + toAcc);
                    con.rollback();
                    return false;
                }
            }

            // 5. Commit Transaction (Flushes to WAL / persistent storage, releases locks)
            con.commit();
            System.out.println("[TRANSACTION SUCCESS] Committed transaction for $" + amount);
            return true;

        } catch (SQLException e) {
            System.err.println("[TRANSACTION EXCEPTION] Error occurred, initiating rollback: " + e.getMessage());
            if (con != null) {
                try {
                    con.rollback();
                    System.out.println("[TRANSACTION ROLLBACK] Database state restored to pre-transaction snapshot.");
                } catch (SQLException rollbackEx) {
                    System.err.println("[CRITICAL] Rollback failed: " + rollbackEx.getMessage());
                }
            }
            return false;
        } finally {
            if (con != null) {
                try {
                    // Senior Rule: Always reset autoCommit to true before closing / returning to pool
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    System.err.println("[ERROR] Failed to restore autoCommit or close connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Demonstrates partial rollback via JDBC Savepoints.
     * Useful in multi-step workflows where non-critical steps can be aborted without cancelling the whole transaction.
     */
    public static void demonstrateSavepoints() {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // Step 1: Valid debit
            String debitSql = "UPDATE accounts SET balance = balance - 100 WHERE account_number = 123";
            try (PreparedStatement ps1 = con.prepareStatement(debitSql)) {
                ps1.executeUpdate();
            }

            // Create a milestone savepoint
            Savepoint savepoint1 = con.setSavepoint("AfterStep1");
            System.out.println("[SAVEPOINT] Created Savepoint 'AfterStep1'");

            // Step 2: Tentative charge that we might decide to discard
            String feeSql = "UPDATE accounts SET balance = balance - 500 WHERE account_number = 123";
            try (PreparedStatement ps2 = con.prepareStatement(feeSql)) {
                ps2.executeUpdate();
            }

            // Suppose business logic decides fee was invalid: roll back ONLY Step 2
            System.out.println("[SAVEPOINT] Rolling back fee to 'AfterStep1'...");
            con.rollback(savepoint1);

            // Step 3: Credit destination account
            String creditSql = "UPDATE accounts SET balance = balance + 100 WHERE account_number = 567";
            try (PreparedStatement ps3 = con.prepareStatement(creditSql)) {
                ps3.executeUpdate();
            }

            // Commit transaction: Step 1 and Step 3 are persisted; Step 2 was cleanly discarded!
            con.commit();
            System.out.println("[SAVEPOINT SUCCESS] Transaction committed with partial rollback preserved!");

        } catch (SQLException e) {
            System.err.println("[SAVEPOINT ERROR] " + e.getMessage());
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Atomically transfers an employee from one department to another.
     */
    public static boolean transferEmployeeDepartment(int empId, int toDeptId) {
        String updateEmpDept = "UPDATE employees SET department_id = ? WHERE id = ?";
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(updateEmpDept)) {
                ps.setInt(1, toDeptId);
                ps.setInt(2, empId);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    con.rollback();
                    return false;
                }
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private static void printAccountBalances() {
        String sql = "SELECT account_number, holder_name, balance FROM accounts ORDER BY account_number";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("--- Current Account Balances ---");
            while (rs.next()) {
                System.out.printf("  Acc #%d (%s): $%,.2f%n",
                        rs.getInt("account_number"),
                        rs.getString("holder_name"),
                        rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to print balances: " + e.getMessage());
        }
    }
}
