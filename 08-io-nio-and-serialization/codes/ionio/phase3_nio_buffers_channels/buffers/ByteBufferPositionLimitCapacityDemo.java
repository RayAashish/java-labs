package ionio.phase3_nio_buffers_channels.buffers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase3_nio_buffers_channels.buffers.codes
 *
 * Demonstrates the internal 4-pointer State Machine of java.nio.ByteBuffer:
 * 1. capacity: Total fixed size of the underlying memory block.
 * 2. limit:    The boundary index up to which you are allowed to read or write.
 * 3. position: Index of the next byte to be written (Write Mode) or read (Read Mode).
 * 4. mark:     A bookmarked position that can be returned to via reset().
 *
 * Invariant: 0 <= mark <= position <= limit <= capacity
 */
public class ByteBufferPositionLimitCapacityDemo {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println(">>> DEMO: java.nio.ByteBuffer State Machine (pos, lim, cap, mark) <<<");
        System.out.println(">>> Author: raysi | Buffer Pointer Transitions Deep Dive");
        System.out.println("==========================================================================\n");

        // Step 1: Allocate a 10-byte buffer on the JVM Heap (Starts in WRITE mode)
        ByteBuffer buffer = ByteBuffer.allocate(10);
        printState("1. After allocate(10) [WRITE MODE]", buffer);

        // Step 2: Write 5 bytes ("RAYSI") into the buffer -> position advances to 5
        buffer.put("RAYSI".getBytes(StandardCharsets.UTF_8));
        printState("2. After putting 'RAYSI' (5 bytes written)", buffer);

        // Step 3: CRITICAL! To read what we wrote, we MUST call flip()
        // flip() sets: limit = current position (5), and resets position = 0
        buffer.flip();
        printState("3. After flip() [SWITCHED TO READ MODE]", buffer);

        // Step 4: Read 2 bytes ('R', 'A') -> position advances to 2
        System.out.print("   -> Read 2 bytes: ");
        System.out.print((char) buffer.get());
        System.out.print((char) buffer.get());
        System.out.println();
        printState("4. After reading 2 bytes ('R','A')", buffer);

        // Step 5: Bookmark current position with mark()
        buffer.mark();
        printState("5. After mark() [BOOKMARKED pos=2]", buffer);

        // Step 6: Read 1 more byte ('Y') -> position advances to 3
        System.out.println("   -> Read 1 more byte: " + (char) buffer.get());
        printState("6. After reading 'Y' (pos advanced to 3)", buffer);

        // Step 7: Restore bookmarked position with reset() -> position returns to 2
        buffer.reset();
        printState("7. After reset() [RETURNED pos TO BOOKMARKED 2]", buffer);

        // Step 8: What if we want to write more without losing unread bytes ('Y', 'S', 'I')?
        // compact() copies unread bytes (indices 2..4) to indices 0..2 and sets position = 3, limit = capacity (10)!
        buffer.compact();
        printState("8. After compact() [PRESERVED 'Y,S,I' & BACK TO WRITE MODE]", buffer);

        // Step 9: Write 4 more bytes ("_DEV") into the buffer
        buffer.put("_DEV".getBytes(StandardCharsets.UTF_8));
        printState("9. After putting '_DEV' (4 bytes appended)", buffer);

        // Step 10: Switch to Read Mode again to read everything
        buffer.flip();
        printState("10. After flip() [READY TO READ CONCATENATED PAYLOAD]", buffer);
        System.out.print("   -> Fully read remaining content: ");
        while (buffer.hasRemaining()) {
            System.out.print((char) buffer.get());
        }
        System.out.println();
        printState("11. After draining all bytes", buffer);

        // Step 11: clear() resets position = 0 and limit = capacity (10)
        // Note: It does NOT wipe the bytes in memory; it just resets the pointers!
        buffer.clear();
        printState("12. After clear() [POINTERS RESET TO WRITE MODE]", buffer);

        System.out.println("\n==========================================================================");
        System.out.println(">>> KEY TAKEAWAYS <<<");
        System.out.println("1. allocate(): Starts in WRITE mode (pos=0, lim=cap).");
        System.out.println("2. flip(): Transitions WRITE -> READ (lim=pos, pos=0).");
        System.out.println("3. rewind(): Re-reads from the beginning (pos=0, lim unchanged).");
        System.out.println("4. compact(): Copies unread bytes to head, sets pos=remaining, lim=cap (switches to WRITE).");
        System.out.println("5. clear(): Does not zero memory! Just resets pos=0, lim=cap.");
        System.out.println("==========================================================================");
    }

    private static void printState(String step, ByteBuffer buf) {
        System.out.printf("%-62s -> [pos=%d, lim=%d, cap=%d]%n",
                step, buf.position(), buf.limit(), buf.capacity());
    }
}
