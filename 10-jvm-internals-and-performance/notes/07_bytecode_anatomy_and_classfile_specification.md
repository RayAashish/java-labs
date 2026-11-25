# 07. Bytecode Anatomy & Classfile Specification

---

## 1. The Classfile Binary Structure: Byte-by-Byte

Every `.class` file executed by the JVM is a strictly packed binary stream adhering to the **Java Virtual Machine Specification (JVMS §4)**. There is zero alignment padding between fields:

```
+---------------------------------------------------------------------------------------------+
|                                 CLASSFILE BINARY STRUCTURE                                  |
|                                                                                             |
|  Type      Name                    Size            Description                              |
|  -----------------------------------------------------------------------------------------  |
|  u4        magic                   4 bytes         0xCAFEBABE (Magic Number identifier)     |
|  u2        minor_version           2 bytes         Minor JDK version (e.g., 0)              |
|  u2        major_version           2 bytes         Major JDK version (65 for Java 21)       |
|  u2        constant_pool_count     2 bytes         Entries in Constant Pool + 1             |
|  cp_info   constant_pool[]         variable        The Symbol & Literal Table of the class  |
|  u2        access_flags            2 bytes         public, final, abstract, interface, etc. |
|  u2        this_class              2 bytes         Index into Constant Pool for this class  |
|  u2        super_class             2 bytes         Index into Constant Pool for super class |
|  u2        interfaces_count        2 bytes         Number of directly implemented interfaces|
|  u2        interfaces[]            variable        Array of Constant Pool indexes           |
|  u2        fields_count            2 bytes         Number of declared fields                |
|  field_info fields[]               variable        Field metadata & attributes              |
|  u2        methods_count           2 bytes         Number of declared methods               |
|  method_info methods[]             variable        Method metadata & executable Code        |
|  u2        attributes_count        2 bytes         Class-level attributes count             |
|  attribute_info attributes[]       variable        SourceFile, InnerClasses, BootstrapMethods|
+---------------------------------------------------------------------------------------------+
```

---

## 2. The Constant Pool: The Central Symbol Directory

The **Constant Pool** is the relational symbol dictionary of the class. Every string literal, class name, interface name, method descriptor, and field name referenced anywhere in the bytecode is stored once in this table as a tagged entry.

```
       +-------------------------------------------------------------+
       |               Common Constant Pool Tags                     |
       +-------------------------------+-----------------------------+
       | Tag Constant                  | Description                 |
       +-------------------------------+-----------------------------+
       | CONSTANT_Utf8 (1)             | Raw UTF-8 string bytes      |
       | CONSTANT_Integer (3)          | 32-bit integer literal      |
       | CONSTANT_Float (4)            | 32-bit float literal        |
       | CONSTANT_Long (5)             | 64-bit long literal (2 slot)|
       | CONSTANT_Double (6)           | 64-bit double literal(2 slot|
       | CONSTANT_Class (7)            | Class or interface pointer  |
       | CONSTANT_String (8)           | String object literal       |
       | CONSTANT_Fieldref (9)         | Field reference pointer     |
       | CONSTANT_Methodref (10)       | Method reference pointer    |
       | CONSTANT_InterfaceMethodref(11)| Interface method pointer   |
       | CONSTANT_NameAndType (12)     | Name and Type descriptor    |
       | CONSTANT_MethodHandle (15)    | Method handle descriptor    |
       | CONSTANT_InvokeDynamic (18)   | Bootstrap specifier for indy|
       +-------------------------------+-----------------------------+
```

> [!NOTE]
> **Constant Pool 1-Based Indexing:** The Constant Pool is 1-indexed (`1` to `constant_pool_count - 1`). Index `0` is reserved for internal JVM use and represents "no reference." Furthermore, 64-bit constants (`Long` and `Double`) consume **two consecutive index slots**—a legacy architectural artifact from 1995.

---

## 3. The `Code` Attribute: Inside a Compiled Method

In the `methods[]` table, executable Java bytecode is stored inside a nested attribute named **`Code`**:

```
Code_attribute {
    u2 attribute_name_index;   // Pointer to Utf8 "Code"
    u4 attribute_length;       // Total length of Code attribute
    u2 max_stack;              // Maximum depth of Operand Stack required
    u2 max_locals;             // Maximum slots required in Local Variable Array
    u4 code_length;            // Number of bytes of bytecode instructions
    u1 code[code_length];      // The raw bytecode opcodes and parameters
    u2 exception_table_length; // Number of catch handlers
    exception_info exception_table[];
    u2 attributes_count;       // Sub-attributes (LineNumberTable, StackMapTable)
    attribute_info attributes[];
}
```

---

## 4. The Stack-Machine Instruction Set

The JVM is an **Abstract Stack Machine** (in contrast to hardware CPUs like x86 or ARM, which are register-based machines).
- Calculations occur by pushing values onto the **Operand Stack**, executing an instruction that pops arguments and pushes the result.
- *Advantage:* Highly compact bytecode format and trivial portability across diverse CPU architectures.

### Categorized Instruction Set Overview:

```
1. CONSTANT LOADING & PUSHING:
   iconst_0 ... iconst_5  : Push small integer constants directly (1 byte opcode).
   bipush <byte>          : Push single byte (-128 to 127) as integer.
   sipush <short>         : Push 16-bit short as integer.
   ldc <index>            : Push constant from Constant Pool (String, int, Class).

2. LOCAL VARIABLE OPERATIONS:
   iload <index>          : Load integer from Local Variable Array slot onto stack.
   istore <index>         : Pop integer from stack and store into Local Variable slot.
   aload <index>          : Load reference pointer from Local Variable slot.
   astore <index>         : Pop reference pointer and store into Local Variable slot.

3. ARITHMETIC & LOGIC:
   iadd, isub, imul, idiv : Pop two ints, perform math, push result.
   iand, ior, ixor        : Bitwise operations.
   iinc <slot> <const>    : Increment local variable in-place without touching stack!

4. CONTROL FLOW & BRANCHING:
   ifeq <offset>          : Branch if top of stack is 0.
   if_icmpne <offset>     : Pop two ints; branch if not equal.
   goto <offset>          : Unconditional branch.
   tableswitch            : O(1) indexed jump table (contiguous switch cases).
   lookupswitch           : O(log N) binary search jump table (sparse switch cases).

5. OBJECT LIFECYCLE & ALLOCATION:
   new <class_index>      : Allocate uninitialized object on heap; push reference.
   dup                    : Duplicate top of stack (needed to preserve 'this' before <init>).
   invokespecial <init>   : Call constructor on newly allocated instance.

6. METHOD INVOCATION OPCODES:
   invokestatic           : Call static method (no 'this' pointer required).
   invokespecial          : Call private method, superclass method, or constructor (non-virtual).
   invokevirtual          : Call standard instance method via V-Table dynamic dispatch.
   invokeinterface        : Call interface method via I-Table lookup.
   invokedynamic (indy)   : Dynamically resolved call site via Bootstrap Method (Lambdas).
```

---

## 5. Bytecode Disassembly Walkthrough: `javap -v -c`

Let us examine what `javac` produces for a classic Java method:

```java
public class Calculator {
    public int addAndMultiply(int a, int b) {
        int sum = a + b;
        return sum * 2;
    }
}
```

Running `javap -p -v -c Calculator.class`:

```text
public int addAndMultiply(int, int);
  descriptor: (II)I
  flags: (0x0001) ACC_PUBLIC
  Code:
    stack=2, locals=4, args_size=3
       0: iload_1         // Push argument 'a' (local slot 1) onto operand stack
       1: iload_2         // Push argument 'b' (local slot 2) onto operand stack
       2: iadd            // Pop 'a' and 'b', add them, push result onto stack
       3: istore_3        // Pop sum and store into local variable slot 3 ('sum')
       4: iload_3         // Load 'sum' from slot 3 onto stack
       5: iconst_2        // Push constant 2 onto stack
       6: imul            // Pop 'sum' and 2, multiply them, push product
       7: ireturn         // Return the integer result to caller
```

### Stack Frame Evolution Visualized:

```
  Instruction      Operand Stack State             Local Variable Array Slots
  -----------------------------------------------------------------------------
  (Initial)        [ Empty ]                       [0: this, 1: a=5, 2: b=10, 3: ?]
  0: iload_1       [ 5 ]                           [0: this, 1: a=5, 2: b=10, 3: ?]
  1: iload_2       [ 5, 10 ]                       [0: this, 1: a=5, 2: b=10, 3: ?]
  2: iadd          [ 15 ]                          [0: this, 1: a=5, 2: b=10, 3: ?]
  3: istore_3      [ Empty ]                       [0: this, 1: a=5, 2: b=10, 3: 15]
  4: iload_3       [ 15 ]                          [0: this, 1: a=5, 2: b=10, 3: 15]
  5: iconst_2      [ 15, 2 ]                       [0: this, 1: a=5, 2: b=10, 3: 15]
  6: imul          [ 30 ]                          [0: this, 1: a=5, 2: b=10, 3: 15]
  7: ireturn       -> Returns 30 to caller frame!
```

---

## 6. `invokedynamic` (Indy) & Modern Java Lambdas

Prior to Java 7, all method calls were strictly typed by four opcodes (`invokestatic`, `invokevirtual`, `invokespecial`, `invokeinterface`).
Java 7 introduced **`invokedynamic` (JSR-292)**, enabling language runtimes to defer method binding to runtime:
- **How Lambdas Are Implemented:** When you write `Runnable r = () -> doWork();`, `javac` does *not* generate an anonymous inner class file (`Main$1.class`)!
- Instead, `javac` emits an `invokedynamic` instruction pointing to a **Bootstrap Method (BSM)** (`LambdaMetafactory.metafactory`).
- On the first invocation, HotSpot calls the BSM, which dynamically spins an optimized, single-purpose class in memory via `MethodHandle` and caches a `CallSite` pointer.
- **Benefit:** Massive reduction in JAR file size, zero disk I/O for inner classes, and faster cold-start times.
