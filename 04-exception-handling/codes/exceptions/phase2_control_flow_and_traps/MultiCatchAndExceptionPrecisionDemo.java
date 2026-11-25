package exceptions.phase2_control_flow_and_traps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase2_control_flow_and_traps.codes
 *
 * Demonstrates:
 * 1. Java 7+ Multi-Catch block syntax and rules (disjoint exception types).
 * 2. Immutability: The multi-catch variable is implicitly 'final'.
 * 3. Precise Re-throw: Why javac allows throws clauses to remain precise even when catching Exception.
 */
public class MultiCatchAndExceptionPrecisionDemo {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("MULTI-CATCH AND PRECISE RE-THROW DEMO");
        System.out.println("===============================================================");

        // Test Multi-Catch
        testMultiCatch(1);
        testMultiCatch(2);

        // Test Precise Re-throw
        try {
            preciseRethrowDemo(true);
        } catch (SQLException e) {
            System.out.println("\n[main] Successfully caught precise SQLException: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("\n[main] Successfully caught precise IOException: " + e.getMessage());
        }
    }

    /**
     * Rule 1: The alternatives in a multi-catch cannot have an inheritance relationship!
     * Example: 'catch (FileNotFoundException | IOException e)' causes a COMPILE ERROR
     * because FileNotFoundException is a subclass of IOException.
     *
     * Rule 2: Multi-catch parameter 'e' is implicitly final.
     * Reassigning 'e = new IOException();' causes compile error:
     * "Multi-catch parameter e may not be assigned"
     */
    public static void testMultiCatch(int condition) {
        try {
            if (condition == 1) {
                throw new SQLException("Database connection timeout at shard-01");
            } else {
                throw new IOException("Failed to read file from disk path /tmp/data.csv");
            }
        } catch (SQLException | IOException e) {
            // e is implicitly final!
            System.out.println("\n[Multi-Catch Intercepted]");
            System.out.println("  Class: " + e.getClass().getName());
            System.out.println("  Message: " + e.getMessage());
            System.out.println("  Common ancestor: java.lang.Exception");
        }
    }

    /**
     * Java 7+ Precise Re-throw (Rethrowing More Specific Exceptions):
     * Even though the catch block catches 'Exception', javac analyzes the try block
     * and knows that ONLY SQLException or IOException can actually be thrown.
     * Therefore, the method signature only needs to declare 'throws SQLException, IOException'!
     */
    public static void preciseRethrowDemo(boolean throwSql) throws SQLException, IOException {
        try {
            if (throwSql) {
                throw new SQLException("Critical DB transaction rollback occurred!");
            } else {
                throw new IOException("Network socket disconnected!");
            }
        } catch (final Exception e) {
            System.out.println("\n[preciseRethrowDemo] Intercepted Exception: " + e.getMessage());
            System.out.println("  Logging context before precise re-throw...");
            throw e; // Compiles because javac knows 'e' can only be SQLException or IOException!
        }
    }
}
