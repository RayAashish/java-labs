package ionio.phase1_classic_io_streams.charstreams;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase1_classic_io_streams.charstreams.codes
 *
 * Demonstrates:
 * 1. Why java.util.Scanner is notoriously slow (heavy regex parsing, unbuffered allocations, synchronization).
 * 2. Standard Fast I/O: BufferedReader + StringTokenizer (standard for Competitive Programming).
 * 3. Ultra-Fast I/O: Byte-level FastScanner reading directly from InputStream with manual ASCII parsing.
 * 4. Empirical Benchmark on 500,000 parsed integers.
 */
public class BufferedReaderFastCPInputTemplate {

    private static final int TEST_ELEMENTS = 500_000;

    public static void main(String[] args) throws IOException {
        System.out.println("==========================================================================");
        System.out.println(">>> BENCHMARK: Scanner vs BufferedReader vs Byte-Level FastScanner <<<");
        System.out.println(">>> Author: raysi | Parsing " + TEST_ELEMENTS + " integers");
        System.out.println("==========================================================================\n");

        // Step 1: Generate synthetic numeric dataset in memory
        byte[] dataset = generateSyntheticNumbers(TEST_ELEMENTS);
        System.out.println("[Step 1] Dataset generated: " + (dataset.length / 1024) + " KB in RAM.\n");

        // Step 2: Benchmark java.util.Scanner (Standard beginner approach)
        System.out.println("[Step 2] Benchmarking java.util.Scanner...");
        long startScanner = System.nanoTime();
        long scannerChecksum = runScanner(new ByteArrayInputStream(dataset));
        long scannerTimeMs = (System.nanoTime() - startScanner) / 1_000_000;
        System.out.printf("   -> Scanner finished: %d ms | Checksum: %d%n%n", scannerTimeMs, scannerChecksum);

        // Step 3: Benchmark BufferedReader + StringTokenizer (Idiomatic CP approach)
        System.out.println("[Step 3] Benchmarking BufferedReader + StringTokenizer...");
        long startBr = System.nanoTime();
        long brChecksum = runBufferedReaderTokenizer(new ByteArrayInputStream(dataset));
        long brTimeMs = (System.nanoTime() - startBr) / 1_000_000;
        System.out.printf("   -> BufferedReader+Tokenizer finished: %d ms | Checksum: %d%n%n", brTimeMs, brChecksum);

        // Step 4: Benchmark Custom Byte-Level FastScanner (Ultra-High Throughput)
        System.out.println("[Step 4] Benchmarking Byte-Level FastScanner (Zero Object Allocation)...");
        long startFast = System.nanoTime();
        long fastChecksum = runByteLevelFastScanner(new ByteArrayInputStream(dataset));
        long fastTimeMs = (System.nanoTime() - startFast) / 1_000_000;
        System.out.printf("   -> Byte-Level FastScanner finished: %d ms | Checksum: %d%n%n", fastTimeMs, fastChecksum);

        // Summary
        System.out.println("==========================================================================");
        System.out.println(">>> PERFORMANCE SUMMARY <<<");
        System.out.println("==========================================================================");
        System.out.printf("1. Scanner:                 %6d ms  (Baseline - Slowest)%n", scannerTimeMs);
        System.out.printf("2. BufferedReader+Tokenizer: %6d ms  (Speedup: %.2fx)%n", brTimeMs, (double) scannerTimeMs / Math.max(1, brTimeMs));
        System.out.printf("3. Byte-Level FastScanner:  %6d ms  (Speedup: %.2fx over Scanner, %.2fx over BR)%n",
                fastTimeMs, (double) scannerTimeMs / Math.max(1, fastTimeMs), (double) brTimeMs / Math.max(1, fastTimeMs));
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("Why Scanner is slow: It evaluates regular expressions for every token delimiter,");
        System.out.println("instantiates String objects per token, and synchronizes internally.");
        System.out.println("FastScanner reads a 32KB byte[] buffer and parses integer values via bit shifts!");
        System.out.println("==========================================================================");
    }

    private static long runScanner(InputStream in) {
        Scanner sc = new Scanner(in);
        long sum = 0;
        while (sc.hasNextInt()) {
            sum += sc.nextInt();
        }
        return sum;
    }

    private static long runBufferedReaderTokenizer(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in), 32768);
        StringTokenizer st = null;
        long sum = 0;
        String line;
        while ((line = br.readLine()) != null) {
            st = new StringTokenizer(line);
            while (st.hasMoreTokens()) {
                sum += Integer.parseInt(st.nextToken());
            }
        }
        return sum;
    }

    private static long runByteLevelFastScanner(InputStream in) throws IOException {
        FastByteScanner sc = new FastByteScanner(in);
        long sum = 0;
        for (int i = 0; i < TEST_ELEMENTS; i++) {
            sum += sc.nextInt();
        }
        return sum;
    }

    private static byte[] generateSyntheticNumbers(int count) {
        StringBuilder sb = new StringBuilder(count * 8);
        for (int i = 1; i <= count; i++) {
            sb.append(i % 1000).append(i % 10 == 0 ? '\n' : ' ');
        }
        return sb.toString().getBytes();
    }

    /**
     * Ultra-High-Performance FastScanner.
     * Uses a 32 KB internal byte buffer and converts ASCII digit bytes directly
     * into integers without intermediate String or Token allocations.
     */
    public static class FastByteScanner {
        private final InputStream in;
        private final byte[] buffer = new byte[32768];
        private int head = 0;
        private int tail = 0;

        public FastByteScanner(InputStream in) {
            this.in = in;
        }

        private byte readByte() throws IOException {
            if (head >= tail) {
                head = 0;
                tail = in.read(buffer, 0, buffer.length);
                if (tail <= 0) return -1;
            }
            return buffer[head++];
        }

        public int nextInt() throws IOException {
            byte c = readByte();
            while (c <= ' ') {
                if (c == -1) return -1;
                c = readByte();
            }
            boolean neg = (c == '-');
            if (neg) {
                c = readByte();
            }
            int res = 0;
            while (c >= '0' && c <= '9') {
                res = res * 10 + (c - '0');
                c = readByte();
            }
            return neg ? -res : res;
        }

        public String nextToken() throws IOException {
            byte c = readByte();
            while (c <= ' ') {
                if (c == -1) return null;
                c = readByte();
            }
            StringBuilder sb = new StringBuilder();
            while (c > ' ') {
                sb.append((char) c);
                c = readByte();
            }
            return sb.toString();
        }
    }
}
