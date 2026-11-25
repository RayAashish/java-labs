package strings;

/**
 * Demonstrates String Immutability, String Constant Pool, intern(),
 * StringBuilder efficiency, and Java 15+ Text Blocks.
 */
public class StringPoolAndCompactStringsDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. String Pool vs Heap Object ===");
        String literal1 = "JavaLabs";
        String literal2 = "JavaLabs";
        String heapCopy = new String("JavaLabs");
        String interned = heapCopy.intern();

        System.out.println("literal1 == literal2 (same pool ref): " + (literal1 == literal2));
        System.out.println("literal1 == heapCopy (different refs): " + (literal1 == heapCopy));
        System.out.println("literal1 == heapCopy.intern() (pool):  " + (literal1 == interned));
        System.out.println("literal1.equals(heapCopy) (content):   " + literal1.equals(heapCopy));

        System.out.println("\n=== 2. Immutability Proof ===");
        String original = "Core";
        original.concat(" Java"); // Return value discarded!
        System.out.println("After ignored concat(): " + original);
        String updated = original.concat(" Java");
        System.out.println("Captured return value:  " + updated);

        System.out.println("\n=== 3. Loop Concatenation vs StringBuilder ===");
        int iterations = 15_000;
        long t1 = System.nanoTime();
        String bad = "";
        for (int i = 0; i < iterations; i++) {
            bad += "x";
        }
        long stringMs = (System.nanoTime() - t1) / 1_000_000;

        long t2 = System.nanoTime();
        StringBuilder sb = new StringBuilder(iterations);
        for (int i = 0; i < iterations; i++) {
            sb.append("x");
        }
        String good = sb.toString();
        long sbMs = (System.nanoTime() - t2) / 1_000_000;

        System.out.println("String += loop (" + bad.length() + " chars):     " + stringMs + " ms");
        System.out.println("StringBuilder  (" + good.length() + " chars):     " + sbMs + " ms");

        System.out.println("\n=== 4. Text Block (Java 15+) ===");
        String sql = """
                SELECT id, email
                FROM users
                WHERE status = '%s'
                """.formatted("ACTIVE");
        System.out.print(sql);
    }
}
