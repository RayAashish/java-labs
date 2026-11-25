package lts.java17.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/**
 * Java 17 LTS — Helpful NPE (JEP 358), PRNG API (JEP 356) & ObjectInputFilter (JEP 415).
 */
public class HelpfulNpePrngAndFilterDemo {

    record Address(String city) {}
    record Customer(Address address) {}

    record SafePayload(String message) implements Serializable {}

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 17 Helpful NPE, PRNG & ObjectInputFilter Demo ===");

        // -------------------------------------------------------------
        // 1. JEP 358 Helpful NullPointerException (Exact null operand diagnostic!)
        // -------------------------------------------------------------
        Customer c = new Customer(null);
        try {
            c.address().city().toUpperCase();
        } catch (NullPointerException npe) {
            System.out.println("[1] Helpful NPE message: " + npe.getMessage());
        }

        // -------------------------------------------------------------
        // 2. JEP 356 Modern PRNG API (L64X128MixRandom)
        // -------------------------------------------------------------
        RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(42L);
        System.out.println("\n[2] L64X128MixRandom sample rolls: " +
                rng.ints(5, 1, 100).boxed().toList());

        // -------------------------------------------------------------
        // 3. JEP 415 Allowlist ObjectInputFilter Defeating Unsafe Classes
        // -------------------------------------------------------------
        byte[] safeBytes = serialize(new SafePayload("Approved"));
        byte[] blockedBytes = serialize(new Date()); // Pretend Date is an unwanted gadget type

        // Strictly allow ONLY SafePayload (and java.base.Record superclass), REJECT everything else `;!*`
        ObjectInputFilter allowOnlyPayload = ObjectInputFilter.Config.createFilter(
                SafePayload.class.getName() + ";java.lang.Record;!*"
        );

        System.out.println("\n[3] Deserialization filter checks:");
        System.out.println("    Allowed SafePayload -> " + deserializeWithFilter(safeBytes, allowOnlyPayload));
        try {
            deserializeWithFilter(blockedBytes, allowOnlyPayload);
        } catch (InvalidClassException rejected) {
            System.out.println("    Blocked unauthorized class -> " + rejected.getMessage());
        }
    }

    private static byte[] serialize(Object obj) throws Exception {
        try (var baos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            return baos.toByteArray();
        }
    }

    private static Object deserializeWithFilter(byte[] bytes, ObjectInputFilter filter) throws Exception {
        try (var ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            ois.setObjectInputFilter(filter);
            return ois.readObject();
        }
    }
}
