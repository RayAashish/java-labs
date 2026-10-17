package jdbc.advanced;

import jdbc.config.DatabaseConfig;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Educational Connection Pool Implementation.
 * <p>
 * <b>Bonus Problem: Connection Pooling Under the Hood</b>
 * <p>
 * <b>Why Connection Pools are Mandatory in Senior Backend Systems:</b>
 * - Creating a physical JDBC connection requires:
 *   1. TCP 3-way handshake (~1 RTT)
 *   2. TLS negotiation (~2 RTTs)
 *   3. MySQL authentication handshake & privilege verification (~1 RTT)
 *   4. MySQL server worker thread allocation and memory buffer setup (~5MB heap on DB server per thread).
 *   Total time per connection: <b>50ms to 250ms</b>!
 * - A connection pool pre-allocates a fixed number of physical sockets and holds them open.
 * - When application code calls {@code dataSource.getConnection()}, an idle socket is borrowed in <b>< 1ms</b>.
 * - When code calls {@code connection.close()}, a Dynamic Proxy intercepts the call and
 *   returns the connection back to the idle queue instead of closing the physical TCP socket!
 * <p>
 * <b>HikariCP in Production:</b>
 * HikariCP is the gold standard connection pool in Spring Boot and Java enterprise applications.
 * It uses bytecode generation, thread-local lockless bags ({@code ConcurrentBag}), and elimination
 * of statement wrappers to achieve zero-overhead pooling.
 */
public class SimpleConnectionPool {

    private final BlockingQueue<Connection> pool;
    private final int poolSize;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public SimpleConnectionPool(int poolSize) throws SQLException {
        this.poolSize = poolSize;
        this.pool = new ArrayBlockingQueue<>(poolSize);

        // Pre-warm the pool with physical database connections
        for (int i = 0; i < poolSize; i++) {
            Connection physicalConnection = DriverManager.getConnection(
                    DatabaseConfig.getUrl(),
                    DatabaseConfig.getUser(),
                    DatabaseConfig.getPassword()
            );
            // Wrap the physical connection in a dynamic proxy that intercepts close()
            Connection proxiedConnection = createConnectionProxy(physicalConnection);
            pool.offer(proxiedConnection);
        }
        System.out.printf("[POOL INITIALIZED] Pre-warmed pool with %d physical connections.%n", poolSize);
    }

    /**
     * Borrows an active connection from the pool.
     * Blocks up to 3 seconds before timing out.
     */
    public Connection getConnection() throws SQLException {
        if (isShutdown.get()) {
            throw new SQLException("Connection pool has been shut down!");
        }
        try {
            Connection conn = pool.poll(3, TimeUnit.SECONDS);
            if (conn == null) {
                throw new SQLException("Connection pool exhausted! All " + poolSize + " connections are busy.");
            }
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Thread interrupted while awaiting connection from pool", e);
        }
    }

    /**
     * Intercepts calls to {@link Connection#close()} so that instead of severing the socket,
     * the connection proxy is returned back to the queue for the next caller.
     */
    private Connection createConnectionProxy(Connection physicalConn) {
        return (Connection) Proxy.newProxyInstance(
                SimpleConnectionPool.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("close".equals(method.getName())) {
                            if (!physicalConn.isClosed()) {
                                // Reset autoCommit to true before putting back into pool
                                physicalConn.setAutoCommit(true);
                                pool.offer((Connection) proxy);
                                // System.out.println("  -> [PROXY] Connection returned to pool (Physical socket kept alive).");
                            }
                            return null;
                        }
                        return method.invoke(physicalConn, args);
                    }
                }
        );
    }

    /**
     * Gracefully shuts down the pool and terminates the underlying physical sockets.
     */
    public synchronized void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            System.out.println("[POOL SHUTDOWN] Closing all underlying physical connections...");
            for (Connection conn : pool) {
                try {
                    // Extract physical connection or close directly
                    conn.unwrap(Connection.class).close();
                } catch (Exception ignored) {}
            }
            pool.clear();
        }
    }

    public static void main(String[] args) throws SQLException {
        System.out.println("=== Demonstrating Custom Connection Pool Architecture ===");

        SimpleConnectionPool pool = new SimpleConnectionPool(3);

        // Borrow 2 connections
        Connection conn1 = pool.getConnection();
        Connection conn2 = pool.getConnection();
        System.out.println("Successfully borrowed 2 connections from pool. Available idle: " + pool.pool.size());

        // Close conn1: notice it returns to pool without severing physical connection!
        conn1.close();
        System.out.println("Closed conn1 via proxy. Available idle in pool: " + pool.pool.size());

        // Close conn2
        conn2.close();
        System.out.println("Closed conn2 via proxy. Available idle in pool: " + pool.pool.size());

        pool.shutdown();
        System.out.println("Demo completed successfully!");
    }
}
