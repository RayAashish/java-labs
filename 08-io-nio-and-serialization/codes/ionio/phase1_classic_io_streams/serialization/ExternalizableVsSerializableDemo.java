package ionio.phase1_classic_io_streams.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase1_classic_io_streams.serialization.codes
 *
 * Demonstrates:
 * 1. Serializable vs Externalizable architectural differences.
 * 2. Payload size comparison: Why Serializable is bloated (writes full class descriptor, field metadata, type signatures).
 * 3. Execution speed benchmark across 50,000 serialized/deserialized instances.
 * 4. The strict requirement of a public no-arg constructor for Externalizable.
 */
public class ExternalizableVsSerializableDemo {

    private static final int ITERATIONS = 50_000;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        System.out.println("==========================================================================");
        System.out.println(">>> BENCHMARK: java.io.Serializable vs java.io.Externalizable <<<");
        System.out.println(">>> Author: raysi | " + ITERATIONS + " Object Iterations");
        System.out.println("==========================================================================\n");

        OrderSerializable sampleSer = new OrderSerializable(99991L, "ORD-ALPHA-7721", 4500.50, 4);
        OrderExternalizable sampleExt = new OrderExternalizable(99991L, "ORD-ALPHA-7721", 4500.50, 4);

        // Step 1: Payload Byte Size Comparison
        byte[] serBytes = serialize(sampleSer);
        byte[] extBytes = serialize(sampleExt);

        System.out.println("[Step 1] Serialized Byte Size Comparison (Single Instance):");
        System.out.printf("   ├── Serializable Payload Size:   %d bytes%n", serBytes.length);
        System.out.printf("   └── Externalizable Payload Size: %d bytes (%.2f%% smaller)%n%n",
                extBytes.length, (1.0 - (double) extBytes.length / serBytes.length) * 100);

        // Step 2: Throughput Benchmark over 50,000 Iterations
        System.out.println("[Step 2] Benchmarking 50,000 Serialization / Deserialization cycles...");

        // Benchmark Serializable
        long startSer = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            byte[] b = serialize(sampleSer);
            OrderSerializable deserialized = (OrderSerializable) deserialize(b);
        }
        long timeSerMs = (System.nanoTime() - startSer) / 1_000_000;
        System.out.printf("   ├── Serializable Total Time:   %d ms%n", timeSerMs);

        // Benchmark Externalizable
        long startExt = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            byte[] b = serialize(sampleExt);
            OrderExternalizable deserialized = (OrderExternalizable) deserialize(b);
        }
        long timeExtMs = (System.nanoTime() - startExt) / 1_000_000;
        System.out.printf("   └── Externalizable Total Time: %d ms (%.2fx faster)%n%n",
                timeExtMs, (double) timeSerMs / Math.max(1, timeExtMs));

        // Step 3: Summary of Mechanics
        System.out.println("==========================================================================");
        System.out.println(">>> ARCHITECTURAL ANALYSIS <<<");
        System.out.println("==========================================================================");
        System.out.println("1. Serializable is a MARKER interface. It inspects object fields at runtime");
        System.out.println("   via reflection, writing class descriptors, field names, and type signatures.");
        System.out.println("2. Externalizable is a BEHAVIORAL interface. It bypasses reflection metadata.");
        System.out.println("   The developer manually writes only the raw primitives/fields needed.");
        System.out.println("3. Deserialization Contract:");
        System.out.println("   - Serializable: Allocates object directly via JVM magic (ReflectionFactory),");
        System.out.println("     calling only the constructor of the first non-serializable superclass.");
        System.out.println("   - Externalizable: MUST invoke the PUBLIC NO-ARG constructor first, then");
        System.out.println("     calls readExternal(ObjectInput in).");
        System.out.println("==========================================================================");
    }

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            oos.flush();
        }
        return baos.toByteArray();
    }

    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return ois.readObject();
        }
    }

    /**
     * Standard reflection-driven Serializable implementation.
     */
    public static class OrderSerializable implements Serializable {
        @Serial
        private static final long serialVersionUID = 100L;

        private Long orderId;
        private String orderCode;
        private Double totalAmount;
        private Integer itemCount;

        public OrderSerializable(Long orderId, String orderCode, Double totalAmount, Integer itemCount) {
            this.orderId = orderId;
            this.orderCode = orderCode;
            this.totalAmount = totalAmount;
            this.itemCount = itemCount;
        }

        public Long getOrderId() { return orderId; }
        public String getOrderCode() { return orderCode; }
        public Double getTotalAmount() { return totalAmount; }
        public Integer getItemCount() { return itemCount; }
    }

    /**
     * High-performance manual Externalizable implementation.
     */
    public static class OrderExternalizable implements Externalizable {
        @Serial
        private static final long serialVersionUID = 200L;

        private long orderId;
        private String orderCode;
        private double totalAmount;
        private int itemCount;

        // CRITICAL: Externalizable requires a public no-arg constructor!
        public OrderExternalizable() {}

        public OrderExternalizable(long orderId, String orderCode, double totalAmount, int itemCount) {
            this.orderId = orderId;
            this.orderCode = orderCode;
            this.totalAmount = totalAmount;
            this.itemCount = itemCount;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeLong(orderId);
            out.writeUTF(orderCode);
            out.writeDouble(totalAmount);
            out.writeInt(itemCount);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            this.orderId = in.readLong();
            this.orderCode = in.readUTF();
            this.totalAmount = in.readDouble();
            this.itemCount = in.readInt();
        }

        public long getOrderId() { return orderId; }
        public String getOrderCode() { return orderCode; }
        public double getTotalAmount() { return totalAmount; }
        public int getItemCount() { return itemCount; }
    }
}
