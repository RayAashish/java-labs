**Loops in Java are used to repeatedly execute a block of code until a specified condition is met.** They help reduce code duplication, save time, and make code more efficient and readable. 

Java offers four main types of loops:

### 1. The `for` Loop
Use the `for` loop when you **know exactly how many times** you want to run the code. It groups the initialization, condition, and update statement together in one line.

```java
for (initialization; condition; update) {
    // Code to be executed
}
```

* **Example:**
  ```java
  for (int i = 0; i < 5; i++) {
      System.out.println("Iteration: " + i);
  }
  ```
  *This will print the text along with numbers from 0 to 4.*

### 2. The `while` Loop
Use the `while` loop when you **do not know the exact number of iterations** beforehand, but you know the condition that must stop it (e.g., waiting for specific user input). It checks the condition **before** executing the loop body (entry-controlled). If the initial condition is false, the loop body never runs.

```java
while (condition) {
    // Code to be executed
    // Must include an update to eventually break the condition
}
```

* **Example:**
  ```java
  int i = 0;
  while (i < 5) {
      System.out.println(i);
      i++; // Increments i so the loop eventually stops
  }
  ```

### 3. The `do-while` Loop
The `do-while` loop is an exit-controlled loop. It executes the code block **at least once** before checking the condition. Use this when the action must happen at least once regardless of the condition (like showing a menu to a user).

```java
do {
    // Code to be executed
} while (condition);
```

* **Example:**
  ```java
  int i = 5;
  do {
      System.out.println("This prints even though i is not less than 5");
      i++;
  } while (i < 5);
  ```

### 4. The Enhanced `for` Loop (for-each)
Specifically used to **iterate exclusively through elements in an array or a collection**. It simplifies code readability because it eliminates the need for a counter variable or index tracking.

```java
for (type variable : arrayOrCollection) {
    // Code to be executed
}
```

* **Example:**
  ```java
  String[] fruits = {"Apple", "Banana", "Cherry"};
  for (String fruit : fruits) {
      System.out.println(fruit);
  }
  ```

---

### Comparison Overview

| Loop Type | Best Used When... | Condition Checked | Minimum Iterations |
| :--- | :--- | :--- | :--- |
| **`for`** | You know the exact number of cycles. | Before the loop body (Entry-controlled). | 0 |
| **`while`** | The execution count depends on a dynamic variable. | Before the loop body (Entry-controlled). | 0 |
| **`do-while`** | The code block must execute at least once. | After the loop body (Exit-controlled). | 1 |
| **`for-each`** | You need to read every single item in a collection. | Automatically handled per element. | 0 (if empty) |

### Key Control Statements
You can alter the behavior of loops using these keywords:
* **`break`:** Instantly terminates the loop altogether.
* **`continue`:** Skips the rest of the current iteration and jumps directly to the next cycle calculation.
