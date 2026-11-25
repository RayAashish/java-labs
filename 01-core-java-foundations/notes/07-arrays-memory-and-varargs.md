# Java Arrays: Detailed Notes

An **array** is a fixed-size, ordered collection of elements of the **same type**, stored in **contiguous** memory and accessed by a zero-based **index**.

```java
int[] marks = {90, 85, 72};
System.out.println(marks[0]);      // 90
System.out.println(marks.length);  // 3
```

---

## Table of Contents

1. [Key Facts](#1-key-facts)
2. [Declaration, Creation and Initialization](#2-declaration-creation-and-initialization)
3. [Default Values](#3-default-values)
4. [Accessing Elements and Exceptions](#4-accessing-elements-and-exceptions)
5. [Arrays in Memory](#5-arrays-in-memory)
6. [Traversing Arrays](#6-traversing-arrays)
7. [Multi-Dimensional and Jagged Arrays](#7-multi-dimensional-and-jagged-arrays)
8. [Passing and Returning Arrays](#8-passing-and-returning-arrays)
9. [Copying Arrays](#9-copying-arrays)
10. [The Arrays Utility Class](#10-the-arrays-utility-class)
11. [Sorting Arrays](#11-sorting-arrays)
12. [Arrays of Objects, Covariance and ArrayStoreException](#12-arrays-of-objects-covariance-and-arraystoreexception)
13. [Varargs](#13-varargs)
14. [Array vs ArrayList and Conversions](#14-array-vs-arraylist-and-conversions)
15. [Time Complexity](#15-time-complexity)
16. [Common Array Algorithms](#16-common-array-algorithms)
17. [Common Pitfalls](#17-common-pitfalls)
18. [Interview-Style Output Questions](#18-interview-style-output-questions)
19. [Quick Cheat Sheet](#19-quick-cheat-sheet)

---

## 1. Key Facts

- Arrays are **objects** in Java. They live on the **heap**, and the variable holds a **reference**.
- Every array directly extends `Object` and implements `Cloneable` and `Serializable`.
- **Fixed size**: the length is set at creation and can never change.
- All elements have the **same type** (primitives or references).
- **Zero-indexed**: valid indices are `0` to `length - 1`.
- The index must be `int`-compatible (`byte`, `short`, `char`, `int`). A `long` index is a compile error.
- `length` is a **final field**, not a method (`arr.length`, not `arr.length()`).
- Elements are automatically initialized to **default values**.
- Arrays support **O(1) random access**.

```java
int[] a = new int[3];
System.out.println(a.getClass().getName());   // [I
System.out.println(a instanceof Object);      // true
```

Quick way to remember the three "length" spellings:

| Type        | Way to get the size |
|-------------|---------------------|
| Array       | `arr.length`        |
| `String`    | `str.length()`      |
| Collections | `list.size()`       |

---

## 2. Declaration, Creation and Initialization

### 2.1 Declaration

```java
int[] arr;        // preferred style
int arr2[];       // legal (C-style), discouraged
String[] names;
double[][] matrix;
```

Watch out when declaring multiple variables:

```java
int[] a, b;       // both a and b are int[]
int a[], b;       // a is int[], b is a plain int
int[] a, b[];     // a is int[], b is int[][]
```

### 2.2 Creation (allocation with `new`)

```java
int[] arr = new int[5];         // 5 elements, all 0
String[] s = new String[3];     // 3 elements, all null
```

The size can be any `int` expression evaluated at runtime, including `0`:

```java
int n = sc.nextInt();
int[] dynamic = new int[n];     // OK
int[] empty = new int[0];       // valid, length is 0
```

### 2.3 Initialization with values

```java
int[] a = {10, 20, 30};                 // array initializer (only at declaration)
int[] b = new int[]{10, 20, 30};        // anonymous array style
int[] c = new int[3];                   // then assign individually
c[0] = 10; c[1] = 20; c[2] = 30;
```

Rules:

```java
int[] x;
x = {1, 2, 3};                 // COMPILE ERROR: initializer allowed only at declaration
x = new int[]{1, 2, 3};        // OK

int[] y = new int[3]{1, 2, 3}; // COMPILE ERROR: cannot give both size and values
```

### 2.4 With `var` (Java 10+)

```java
var a = new int[]{1, 2, 3};    // OK
var b = {1, 2, 3};             // ERROR: cannot infer type from an initializer alone
```

### 2.5 `final` arrays

`final` makes the **reference** constant, not the elements.

```java
final int[] a = {1, 2, 3};
a[0] = 99;                     // OK, elements are mutable
a = new int[5];                // ERROR: cannot reassign a final variable
```

### 2.6 `main(String[] args)`

`args` is just a `String[]` holding command-line arguments; `args.length` is `0` when none are given.

---

## 3. Default Values

Every element of a newly created array is set to the type's default.

| Element type                 | Default          |
|------------------------------|------------------|
| `byte`, `short`, `int`       | `0`              |
| `long`                       | `0L`             |
| `float`                      | `0.0f`           |
| `double`                     | `0.0`            |
| `char`                       | `'\u0000'` (null character) |
| `boolean`                    | `false`          |
| Any reference (`String`, ...)| `null`           |

```java
boolean[] flags = new boolean[2];
System.out.println(flags[0]);        // false

String[] names = new String[2];
System.out.println(names[0]);        // null
System.out.println(names[0].length());   // NullPointerException
```

> Local **variables** must be initialized before use, but **array elements** never need to be.

---

## 4. Accessing Elements and Exceptions

```java
int[] a = {10, 20, 30};
System.out.println(a[0]);            // 10   (first)
System.out.println(a[a.length - 1]); // 30   (last)
a[1] = 99;                           // update
```

### 4.1 Runtime exceptions

| Exception | Cause |
|-----------|-------|
| `ArrayIndexOutOfBoundsException` | Index `< 0` or `>= length` |
| `NegativeArraySizeException`     | `new int[n]` where `n < 0` |
| `NullPointerException`           | Using `arr.length` / `arr[i]` when `arr` is `null` |
| `ArrayStoreException`            | Storing an incompatible object type (see section 12) |
| `OutOfMemoryError`               | Array too large for the heap |

```java
int[] a = {1, 2, 3};
System.out.println(a[3]);
// ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 3

int n = -1;
int[] b = new int[n];                // NegativeArraySizeException

int[] c = null;
System.out.println(c.length);        // NullPointerException
```

### 4.2 `null` array vs empty array

```java
int[] nothing = null;        // no array object at all
int[] empty = new int[0];    // valid array with length 0
```

Prefer returning an **empty array** instead of `null` from methods to save callers from null checks.

### 4.3 `char` as an index

`char` is promoted to `int`, which is handy for frequency counting:

```java
int[] freq = new int[26];
for (char ch : "banana".toCharArray()) {
    freq[ch - 'a']++;
}
// freq[0] ('a') = 3, freq[1] ('b') = 1, freq[13] ('n') = 2
```

---

## 5. Arrays in Memory

```java
int[] a = new int[3];
a[0] = 5;
```

```text
STACK                     HEAP
+-----+                   +---+---+---+
|  a  | ----------------> | 5 | 0 | 0 |    int[] object (length = 3)
+-----+                   +---+---+---+
```

- The variable `a` holds a **reference**; the array object is on the heap.
- For **primitive arrays**, the elements themselves sit contiguously in the array object.
- For **object arrays**, the elements are references (each may be `null` or point to a separate object).

```java
int[] a = {1, 2, 3};
int[] b = a;          // copies the REFERENCE, not the array (aliasing)
b[0] = 100;
System.out.println(a[0]);   // 100
```

Array **equality** with `==` or `.equals()` compares references only:

```java
int[] p = {1, 2, 3};
int[] q = {1, 2, 3};
System.out.println(p == q);                 // false
System.out.println(p.equals(q));            // false (Object.equals = identity)
System.out.println(java.util.Arrays.equals(p, q));   // true (content)
```

---

## 6. Traversing Arrays

### 6.1 Classic `for` loop

Use when you need the **index**, want to **modify** elements, or need a custom range/direction.

```java
for (int i = 0; i < a.length; i++) {
    System.out.println(a[i]);
}

for (int i = a.length - 1; i >= 0; i--) {   // reverse
    System.out.println(a[i]);
}
```

### 6.2 Enhanced `for` (for-each)

```java
for (int x : a) {
    System.out.println(x);
}
```

Limitations:

- No access to the index.
- Cannot traverse in reverse or skip elements.
- The loop variable is a **copy** of the element (for primitives), so assigning to it does **not** change the array:

```java
int[] a = {1, 2, 3};
for (int x : a) {
    x *= 2;
}
System.out.println(Arrays.toString(a));   // [1, 2, 3]  (unchanged)
```

For object arrays, you can change the **state** of the object through the loop variable, but reassigning the variable does not replace the array element.

### 6.3 Streams

```java
Arrays.stream(a).forEach(System.out::println);
int sum = Arrays.stream(a).sum();
```

### 6.4 Printing arrays

```java
int[] a = {1, 2, 3};
System.out.println(a);                      // [I@6d06d69c  (type + hash, not the content)
System.out.println(Arrays.toString(a));     // [1, 2, 3]

int[][] m = {{1, 2}, {3, 4}};
System.out.println(Arrays.toString(m));     // [[I@..., [I@...]  (rows are references)
System.out.println(Arrays.deepToString(m)); // [[1, 2], [3, 4]]
```

Special case: `char[]` has its own `println` overload that prints the characters.

```java
char[] c = {'h', 'i'};
System.out.println(c);          // hi
System.out.println("" + c);     // [C@...  (string concatenation uses toString of the reference)
System.out.println(String.valueOf(c));   // hi
```

---

## 7. Multi-Dimensional and Jagged Arrays

Java has no true multi-dimensional arrays; a 2D array is an **array of arrays**.

### 7.1 Rectangular 2D arrays

```java
int[][] m = new int[3][4];              // 3 rows, 4 columns, all 0
int[][] g = {
    {1, 2, 3},
    {4, 5, 6}
};

System.out.println(g.length);           // 2  (rows)
System.out.println(g[0].length);        // 3  (columns in row 0)
System.out.println(g[1][2]);            // 6
```

Traversal:

```java
for (int i = 0; i < g.length; i++) {
    for (int j = 0; j < g[i].length; j++) {
        System.out.print(g[i][j] + " ");
    }
    System.out.println();
}

for (int[] row : g) {
    for (int val : row) {
        System.out.print(val + " ");
    }
}
```

### 7.2 Jagged arrays (rows of different lengths)

```java
int[][] jag = new int[3][];       // only the first dimension is mandatory
jag[0] = new int[2];
jag[1] = new int[4];
jag[2] = new int[1];

int[][] tri = {{1}, {2, 3}, {4, 5, 6}};
```

```java
int[][] m = new int[2][];
System.out.println(m[0]);          // null   (row not created yet)
System.out.println(m[0][0]);       // NullPointerException
```

### 7.3 3D arrays

```java
int[][][] cube = new int[2][3][4];   // 2 x 3 x 4
```

### 7.4 Memory layout

```text
g --> [ ref0 ] --> [1, 2, 3]
      [ ref1 ] --> [4, 5, 6]
```

### 7.5 Shallow copy trap

```java
int[][] a = {{1, 2}, {3, 4}};
int[][] b = a.clone();      // copies only the outer array; rows are SHARED

b[0][0] = 99;
System.out.println(a[0][0]);   // 99  (row is shared)

b[0] = new int[]{7, 8};        // replaces the row reference in b only
System.out.println(a[0][0]);   // 99  (a still points to the old row)
```

Deep copy for 2D:

```java
int[][] deep = new int[a.length][];
for (int i = 0; i < a.length; i++) {
    deep[i] = a[i].clone();
}
```

### 7.6 Matrix transpose

```java
int rows = m.length, cols = m[0].length;
int[][] t = new int[cols][rows];
for (int i = 0; i < rows; i++)
    for (int j = 0; j < cols; j++)
        t[j][i] = m[i][j];
```

---

## 8. Passing and Returning Arrays

Java is **pass-by-value**. For arrays, the value passed is a **copy of the reference**.

- The method **can modify the elements** of the caller's array.
- The method **cannot reassign** the caller's variable.

```java
static void modify(int[] a) {
    a[0] = 100;                 // visible to caller
    a = new int[]{9, 9, 9};     // only changes the local copy of the reference
}

int[] arr = {1, 2, 3};
modify(arr);
System.out.println(Arrays.toString(arr));   // [100, 2, 3]
```

### 8.1 Swap needs the array and indices

```java
static void swap(int[] a, int i, int j) {
    int tmp = a[i];
    a[i] = a[j];
    a[j] = tmp;
}
```

Swapping two plain `int` parameters does nothing for the caller, but swapping elements of a passed array works.

### 8.2 Returning an array

```java
static int[] minMax(int[] a) {
    int min = a[0], max = a[0];
    for (int x : a) {
        if (x < min) min = x;
        if (x > max) max = x;
    }
    return new int[]{min, max};
}
```

### 8.3 Passing an array literal

```java
printAll(new int[]{1, 2, 3});     // must use "new int[]{...}"
printAll({1, 2, 3});              // COMPILE ERROR
```

---

## 9. Copying Arrays

| Method | Type of copy | Notes |
|--------|--------------|-------|
| `b = a` | **None** (alias) | Both variables refer to the same array |
| `a.clone()` | Shallow | Simple, returns `int[]` without a cast |
| `Arrays.copyOf(a, newLen)` | Shallow | Can truncate or pad with defaults |
| `Arrays.copyOfRange(a, from, to)` | Shallow | `from` inclusive, `to` **exclusive** |
| `System.arraycopy(src, sp, dst, dp, len)` | Shallow | Fastest, works in place, handles overlap |
| Manual loop | Your choice | Full control |
| `Arrays.stream(a).toArray()` | Shallow | Stream-based |

```java
int[] a = {1, 2, 3, 4, 5};

int[] c1 = a.clone();                          // [1, 2, 3, 4, 5]
int[] c2 = Arrays.copyOf(a, 3);                // [1, 2, 3]        (truncated)
int[] c3 = Arrays.copyOf(a, 7);                // [1, 2, 3, 4, 5, 0, 0]  (padded)
int[] c4 = Arrays.copyOfRange(a, 1, 4);        // [2, 3, 4]

int[] dest = new int[5];
System.arraycopy(a, 0, dest, 1, 4);            // dest = [0, 1, 2, 3, 4]
```

### 9.1 `System.arraycopy` for shifting in place

```java
int[] a = {1, 2, 3, 4, 5};
// Insert 99 at index 2 by shifting right (array must have spare room, here we use a bigger one)
int[] b = new int[6];
System.arraycopy(a, 0, b, 0, 2);       // copy [1, 2]
b[2] = 99;
System.arraycopy(a, 2, b, 3, 3);       // copy [3, 4, 5] after the gap
// b = [1, 2, 99, 3, 4, 5]
```

### 9.2 Shallow copy of object arrays

Copying an object array copies the **references**, so both arrays point to the same objects.

```java
StringBuilder[] x = { new StringBuilder("a") };
StringBuilder[] y = x.clone();
y[0].append("!");
System.out.println(x[0]);      // a!   (same object)
```

---

## 10. The Arrays Utility Class

`import java.util.Arrays;`

| Method | Purpose |
|--------|---------|
| `Arrays.toString(a)` | String form of a 1D array |
| `Arrays.deepToString(a)` | String form of nested arrays |
| `Arrays.sort(a)` | Sort ascending |
| `Arrays.sort(a, from, to)` | Sort a range (`to` exclusive) |
| `Arrays.sort(a, comparator)` | Sort object arrays with a custom order |
| `Arrays.parallelSort(a)` | Multi-threaded sort for large arrays |
| `Arrays.binarySearch(a, key)` | Search a **sorted** array |
| `Arrays.fill(a, v)` / `fill(a, from, to, v)` | Fill with a value |
| `Arrays.equals(a, b)` | Element-wise equality |
| `Arrays.deepEquals(a, b)` | Equality for nested arrays |
| `Arrays.hashCode(a)` / `deepHashCode(a)` | Content-based hash |
| `Arrays.copyOf` / `copyOfRange` | Copy (see section 9) |
| `Arrays.asList(T...)` | Fixed-size `List` view of an object array |
| `Arrays.stream(a)` | Stream over the array |
| `Arrays.setAll(a, i -> ...)` | Fill using a function of the index |
| `Arrays.compare(a, b)` / `mismatch(a, b)` | Lexicographic compare / first differing index (Java 9+) |

### 10.1 Examples

```java
int[] a = {5, 2, 9, 1};

Arrays.sort(a);                                  // [1, 2, 5, 9]
System.out.println(Arrays.binarySearch(a, 5));   // 2
System.out.println(Arrays.binarySearch(a, 3));   // -3
```

**`binarySearch` return value:** if found, the index. If not found, `-(insertionPoint) - 1`. For key `3` in `[1, 2, 5, 9]`, the insertion point is `2`, so the result is `-3`. The array **must be sorted**, otherwise the result is undefined.

```java
int[] f = new int[5];
Arrays.fill(f, 7);                 // [7, 7, 7, 7, 7]
Arrays.fill(f, 1, 3, 0);           // [7, 0, 0, 7, 7]   (from 1 inclusive, to 3 exclusive)

int[] sq = new int[5];
Arrays.setAll(sq, i -> i * i);     // [0, 1, 4, 9, 16]

int[] x = {1, 2, 3}, y = {1, 2, 4};
System.out.println(Arrays.equals(x, y));     // false
System.out.println(Arrays.mismatch(x, y));   // 2

int[][] p = {{1, 2}, {3}}, q = {{1, 2}, {3}};
System.out.println(Arrays.equals(p, q));     // false (rows compared by reference)
System.out.println(Arrays.deepEquals(p, q)); // true
```

### 10.2 Stream helpers on primitive arrays

```java
int[] a = {4, 8, 15, 16, 23, 42};

int sum = Arrays.stream(a).sum();                    // 108
int max = Arrays.stream(a).max().getAsInt();        // 42
double avg = Arrays.stream(a).average().orElse(0);  // 18.0
int[] evens = Arrays.stream(a).filter(v -> v % 2 == 0).toArray();
```

### 10.3 `Arrays.asList` gotchas

```java
Integer[] boxed = {1, 2, 3};
List<Integer> list = Arrays.asList(boxed);   // fixed-size view backed by the array

list.set(0, 99);
System.out.println(boxed[0]);                // 99   (write-through)
list.add(4);                                 // UnsupportedOperationException (fixed size)

int[] prim = {1, 2, 3};
List<int[]> bad = Arrays.asList(prim);       // List with ONE element (the whole array)
System.out.println(bad.size());              // 1
```

`Arrays.asList` needs an **object** array. A primitive `int[]` is treated as a single object.

---

## 11. Sorting Arrays

### 11.1 Primitives

```java
int[] a = {5, 2, 9, 1};
Arrays.sort(a);                    // [1, 2, 5, 9]
Arrays.sort(a, 1, 3);              // sort only indices 1 and 2
```

Primitive arrays use **Dual-Pivot Quicksort**, O(n log n) on average.

### 11.2 Objects

Object arrays use **TimSort** (stable, O(n log n) worst case).

```java
String[] s = {"pear", "Apple", "fig"};
Arrays.sort(s);                                     // [Apple, fig, pear]  (uppercase before lowercase)
Arrays.sort(s, String.CASE_INSENSITIVE_ORDER);      // [Apple, fig, pear]
Arrays.sort(s, Comparator.comparing(String::length)); // by length
```

### 11.3 Descending order

`Collections.reverseOrder()` works only on **object** arrays.

```java
Integer[] b = {5, 2, 9, 1};
Arrays.sort(b, Collections.reverseOrder());   // [9, 5, 2, 1]

int[] a = {5, 2, 9, 1};
Arrays.sort(a);                               // then reverse manually
```

### 11.4 Sorting 2D arrays by a column

```java
int[][] intervals = {{5, 6}, {1, 3}, {2, 4}};
Arrays.sort(intervals, (x, y) -> Integer.compare(x[0], y[0]));
// [[1, 3], [2, 4], [5, 6]]
```

Prefer `Integer.compare(x, y)` over `x - y`, because subtraction can **overflow**.

---

## 12. Arrays of Objects, Covariance and ArrayStoreException

### 12.1 Object arrays hold references

```java
String[] names = new String[3];      // [null, null, null]
names[0] = "Aashish";

Student[] students = new Student[2]; // no Student objects yet
students[0] = new Student("A");      // must create each object
```

### 12.2 Array covariance

If `Dog` extends `Animal`, then `Dog[]` is a subtype of `Animal[]`.

```java
Object[] objs = new String[2];       // allowed (covariance)
objs[0] = "ok";
objs[1] = 42;                        // compiles, but throws ArrayStoreException at runtime
```

The runtime remembers the actual element type (`String`), so storing an `Integer` fails. Generics (`List<Object> = new ArrayList<String>()`) reject this at **compile time** instead.

### 12.3 Generic arrays

```java
T[] arr = new T[10];                        // ERROR: generic array creation
T[] arr = (T[]) new Object[10];             // works with an unchecked warning
List<String>[] lists = new List[3];         // unchecked warning
```

Prefer `List<T>` over arrays when generics are involved.

---

## 13. Varargs

A **variable-arity** parameter (`type... name`) is treated as an **array** inside the method.

```java
static int sum(int... nums) {          // nums is an int[]
    int total = 0;
    for (int n : nums) total += n;
    return total;
}

sum();               // 0     (empty array)
sum(1, 2, 3);        // 6
sum(new int[]{4, 5}); // 9    (an array can be passed directly)
```

Rules:

- Only **one** varargs parameter is allowed and it must be the **last** parameter.
- `main(String... args)` is a valid signature.

---

## 14. Array vs ArrayList and Conversions

| Feature | Array | `ArrayList` |
|---------|-------|-------------|
| Size | Fixed | Grows and shrinks |
| Element types | Primitives and objects | Objects only (wrappers like `Integer`) |
| Size access | `arr.length` | `list.size()` |
| Element access | `arr[i]` | `list.get(i)` |
| Generics | Not supported | Supported |
| Speed / memory | Faster, less overhead for primitives | Extra overhead (boxing, resizing) |
| Built-in methods | Few (`Arrays` helper class) | Many (`add`, `remove`, `contains`, ...) |

### 14.1 Conversions

```java
// Array -> List
Integer[] boxed = {1, 2, 3};
List<Integer> l1 = new ArrayList<>(Arrays.asList(boxed));   // modifiable copy

int[] prim = {1, 2, 3};
List<Integer> l2 = Arrays.stream(prim).boxed().collect(Collectors.toList());

// List -> Array
List<String> names = List.of("a", "b");
String[] arr1 = names.toArray(new String[0]);

List<Integer> nums = List.of(1, 2, 3);
int[] arr2 = nums.stream().mapToInt(Integer::intValue).toArray();
```

---

## 15. Time Complexity

| Operation | Complexity |
|-----------|------------|
| Access / update by index | O(1) |
| Search in unsorted array | O(n) |
| Binary search in sorted array | O(log n) |
| Insert / delete in the middle | O(n) (elements must shift) |
| Append (needs a new array when full) | O(n) for the copy |
| Sort | O(n log n) |
| Space for an array of size n | O(n) |

---

## 16. Common Array Algorithms

### 16.1 Find max / min

```java
int max = a[0];
for (int i = 1; i < a.length; i++) {
    if (a[i] > max) max = a[i];
}
```

### 16.2 Reverse (two pointers)

```java
static void reverse(int[] a, int l, int r) {
    while (l < r) {
        int t = a[l]; a[l] = a[r]; a[r] = t;
        l++; r--;
    }
}
```

### 16.3 Rotate right by k (reversal trick)

```java
static void rotate(int[] a, int k) {
    int n = a.length;
    if (n == 0) return;
    k %= n;
    reverse(a, 0, n - 1);
    reverse(a, 0, k - 1);
    reverse(a, k, n - 1);
}
// [1,2,3,4,5,6,7], k = 3  ->  [5,6,7,1,2,3,4]
```

### 16.4 Prefix sum (range sum queries in O(1))

```java
int[] pre = new int[a.length + 1];
for (int i = 0; i < a.length; i++) {
    pre[i + 1] = pre[i] + a[i];
}
// sum of a[l..r] (inclusive) = pre[r + 1] - pre[l]
```

### 16.5 Maximum subarray sum (Kadane's algorithm)

```java
int best = a[0], cur = a[0];
for (int i = 1; i < a.length; i++) {
    cur = Math.max(a[i], cur + a[i]);
    best = Math.max(best, cur);
}
```

### 16.6 Binary search (iterative)

```java
static int binarySearch(int[] a, int target) {
    int lo = 0, hi = a.length - 1;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;      // overflow-safe
        if (a[mid] == target) return mid;
        else if (a[mid] < target) lo = mid + 1;
        else hi = mid - 1;
    }
    return -1;
}
```

### 16.7 Sliding window: max sum of k consecutive elements

```java
int sum = 0;
for (int i = 0; i < k; i++) sum += a[i];
int best = sum;
for (int i = k; i < a.length; i++) {
    sum += a[i] - a[i - k];
    best = Math.max(best, sum);
}
```

### 16.8 Remove duplicates from a sorted array (in place)

```java
int w = 1;                                  // write index
for (int r = 1; r < a.length; r++) {
    if (a[r] != a[r - 1]) a[w++] = a[r];
}
// first w elements are unique
```

### 16.9 Frequency array for lowercase letters

```java
int[] freq = new int[26];
for (char c : s.toCharArray()) freq[c - 'a']++;
```

### 16.10 Two-pointer pair sum (sorted array)

```java
int l = 0, r = a.length - 1;
while (l < r) {
    int s = a[l] + a[r];
    if (s == target) return new int[]{l, r};
    if (s < target) l++; else r--;
}
```

---

## 17. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | Off-by-one: `i <= a.length` | Use `i < a.length` |
| 2 | Printing an array directly gives `[I@hash` | `Arrays.toString` / `deepToString` |
| 3 | Comparing arrays with `==` or `.equals()` | `Arrays.equals` / `deepEquals` |
| 4 | `b = a` thinks it copies | Use `clone`, `copyOf`, or `arraycopy` |
| 5 | Shallow copy of 2D or object arrays | Copy each row or element yourself |
| 6 | Using `a.length()` or `s.length` | Array: `length`, String: `length()` |
| 7 | Forgetting to create objects in an object array | Elements start as `null` |
| 8 | Modifying elements through for-each on primitives | Use an indexed loop |
| 9 | `Arrays.asList(int[])` returns a one-element list | Use `Integer[]` or streams with `boxed()` |
| 10 | `Arrays.asList(...).add()` fails | Wrap in `new ArrayList<>(...)` |
| 11 | `binarySearch` on an unsorted array | Sort first |
| 12 | `Collections.reverseOrder()` on `int[]` | Use `Integer[]` |
| 13 | `x - y` in a comparator can overflow | `Integer.compare(x, y)` |
| 14 | `arr[-1]` (Java has no negative indexing) | Use `arr[arr.length - 1]` |
| 15 | Jagged array row still `null` | Allocate each row before use |
| 16 | Assuming a `final` array is immutable | Only the reference is final |
| 17 | Passing an array and expecting the caller's variable to change on reassignment | Modify elements, or return the new array |

---

## 18. Interview-Style Output Questions

**Q1**
```java
int[] a = new int[3];
System.out.println(a[0]);
```
**Answer:** `0`. Default value for `int`.

---

**Q2**
```java
int[] a = {1, 2, 3};
int[] b = a;
b[0] = 10;
System.out.println(a[0]);
```
**Answer:** `10`. Both variables refer to the same array.

---

**Q3**
```java
int[] a = {1, 2, 3};
int[] b = a.clone();
b[0] = 9;
System.out.println(a[0]);
```
**Answer:** `1`. `clone()` created a separate array.

---

**Q4**
```java
int[] a = {1, 2, 3};
int[] b = {1, 2, 3};
System.out.println(a == b);
System.out.println(a.equals(b));
System.out.println(Arrays.equals(a, b));
```
**Answer:** `false`, `false`, `true`.

---

**Q5**
```java
static void modify(int[] a) { a[0] = 100; a = new int[]{9, 9}; }
int[] arr = {1, 2, 3};
modify(arr);
System.out.println(Arrays.toString(arr));
```
**Answer:** `[100, 2, 3]`. The element change is visible; the reassignment is local.

---

**Q6**
```java
int[] a = {1, 2, 3};
for (int x : a) x *= 2;
System.out.println(Arrays.toString(a));
```
**Answer:** `[1, 2, 3]`. `x` is a copy of each element.

---

**Q7**
```java
Object[] o = new Integer[1];
o[0] = "hi";
```
**Answer:** Compiles, but throws `ArrayStoreException: java.lang.String` at runtime.

---

**Q8**
```java
List<int[]> l = Arrays.asList(new int[]{1, 2, 3});
System.out.println(l.size());
```
**Answer:** `1`. The whole `int[]` is a single element.

---

**Q9**
```java
char[] c = {'a', 'b'};
System.out.println(c);
System.out.println("" + c);
```
**Answer:** `ab`, then something like `[C@1b6d3586`. The first uses the `println(char[])` overload; the second uses string concatenation on the reference.

---

**Q10**
```java
int i = 0;
int[] a = new int[3];
a[i] = i = 2;
System.out.println(Arrays.toString(a));
```
**Answer:** `[2, 0, 0]`. The array index `a[i]` is evaluated **before** the right-hand side, so the index is `0`.

---

**Q11**
```java
int[][] m = new int[2][];
System.out.println(m[0]);
System.out.println(m[0][0]);
```
**Answer:** `null`, then `NullPointerException`. The row was never allocated.

---

**Q12**
```java
int[] a = new int[5];
Arrays.fill(a, 1, 3, 7);
System.out.println(Arrays.toString(a));
```
**Answer:** `[0, 7, 7, 0, 0]`. The `to` index is exclusive.

---

**Q13**
```java
int[] a = {1, 2, 5, 9};
System.out.println(Arrays.binarySearch(a, 3));
```
**Answer:** `-3`. The insertion point is `2`, so `-(2) - 1 = -3`.

---

**Q14**
```java
int[] a = Arrays.copyOf(new int[]{1, 2, 3}, 5);
System.out.println(Arrays.toString(a));
```
**Answer:** `[1, 2, 3, 0, 0]`. Extra slots are padded with the default value.

---

**Q15**
```java
int[] a = {1, 2, 3};
System.out.println(a[3]);
```
**Answer:** `ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 3`.

---

## 19. Quick Cheat Sheet

```text
DECLARE        int[] a;                    int[][] m;
CREATE         new int[5]                  new int[3][4]        new int[3][]   (jagged)
INIT           {1, 2, 3}  (only at declaration)      new int[]{1, 2, 3}
LENGTH         a.length  (field)           m[0].length  (columns of row 0)
DEFAULTS       0 / 0.0 / false / '\u0000' / null
INDEX RANGE    0 .. length-1               else ArrayIndexOutOfBoundsException
PRINT          Arrays.toString(a)          Arrays.deepToString(m)
COMPARE        Arrays.equals(a, b)         Arrays.deepEquals(m1, m2)
COPY           clone()   copyOf()   copyOfRange()   System.arraycopy()
SORT           Arrays.sort(a)              Arrays.sort(objs, comparator)
SEARCH         Arrays.binarySearch(a, key)   (sorted only; not found -> -(ins) - 1)
FILL           Arrays.fill(a, v)           Arrays.setAll(a, i -> ...)
```

**Remember:**

1. Arrays are objects: the variable is a **reference**, and assignment copies the reference.
2. Size is **fixed** at creation; use `ArrayList` when you need to grow.
3. `length` (array) vs `length()` (String) vs `size()` (collections).
4. `clone`, `copyOf`, and `arraycopy` are all **shallow** copies.
5. Use `Arrays.toString` / `deepToString` / `equals` / `deepEquals` instead of printing or comparing arrays directly.
6. For-each cannot modify primitive elements or give you the index.
7. `Arrays.asList` needs `Integer[]`, not `int[]`, and returns a fixed-size list.
8. Guard against off-by-one errors and remember `binarySearch` needs sorted input.
