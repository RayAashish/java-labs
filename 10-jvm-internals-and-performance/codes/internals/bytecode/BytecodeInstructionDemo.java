package internals.bytecode;

/**
 * Architectural Demonstration: JVM Stack Machine Bytecode Reference.
 * <p>
 * This class is designed as a direct reference for bytecode disassembly analysis using:
 * {@code javap -v -p -c BytecodeInstructionDemo.class}
 * <p>
 * Key Bytecode Patterns Illustrated:
 * 1. Arithmetic & Stack Manipulation (iload, istore, iadd, imul, ireturn).
 * 2. Object Instantiation (new, dup, invokespecial).
 * 3. Polymorphic Dispatch vs Static Invocations (invokevirtual, invokestatic, invokespecial).
 * 4. Branching & Comparisons (if_icmpge, goto).
 * 5. Exception Table Handlers.
 */
public class BytecodeInstructionDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   JVM BYTECODE INSTRUCTION SET DEMONSTRATION");
        System.out.println("================================================================================");

        BytecodeInstructionDemo demo = new BytecodeInstructionDemo();

        int sum = demo.addAndMultiply(10, 20);
        System.out.println("[1] addAndMultiply(10, 20) = " + sum);

        String type = demo.classifyNumber(15);
        System.out.println("[2] classifyNumber(15)     = " + type);

        int guarded = demo.guardedDivision(100, 5);
        System.out.println("[3] guardedDivision(100, 5)= " + guarded);

        System.out.println("\n[Tip] Disassemble this compiled classfile using:");
        System.out.println("      javap -v -p -c BytecodeInstructionDemo.class");
        System.out.println("================================================================================");
    }

    /**
     * BYTECODE BREAKDOWN:
     * 0: iload_1         // Push 'a' onto operand stack
     * 1: iload_2         // Push 'b' onto operand stack
     * 2: iadd            // Pop 'a' & 'b', add, push result
     * 3: istore_3        // Pop result, store into local slot 3 ('total')
     * 4: iload_3         // Push 'total' onto stack
     * 5: iconst_3        // Push constant 3 onto stack
     * 6: imul            // Multiply top two ints
     * 7: ireturn         // Return product
     */
    public int addAndMultiply(int a, int b) {
        int total = a + b;
        return total * 3;
    }

    /**
     * BYTECODE BREAKDOWN:
     * 0: iload_1         // Push 'val'
     * 1: bipush 10       // Push byte constant 10
     * 3: if_icmple 9     // Branch to instruction 9 if val <= 10
     * 6: ldc "LARGE"     // Push String reference from Constant Pool
     * 8: areturn         // Return reference
     * 9: ldc "SMALL"     // Push String reference from Constant Pool
     * 11: areturn        // Return reference
     */
    public String classifyNumber(int val) {
        if (val > 10) {
            return "LARGE";
        }
        return "SMALL";
    }

    /**
     * BYTECODE EXCEPTION TABLE:
     * from to target type
     * 0    4   7     Class java/lang/ArithmeticException
     */
    public int guardedDivision(int numerator, int denominator) {
        try {
            return numerator / denominator; // idiv
        } catch (ArithmeticException e) {
            return -1;
        }
    }
}
