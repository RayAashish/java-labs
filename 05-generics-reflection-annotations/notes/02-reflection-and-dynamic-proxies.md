# 02. Reflection API & Dynamic Proxies

---

## 🟢 Layer 1 — Noob Track: Inspecting a Class While Running

Normally, Java code knows every class and method name before compiling (`user.getName()`). **Reflection (`java.lang.reflect`)** lets a program inspect and invoke classes, constructors, fields, and methods **dynamically at runtime** using only strings or `Class<?>` objects!

```java
Class<?> clazz = Class.forName("java.lang.String");
System.out.println("Methods count: " + clazz.getDeclaredMethods().length);
```

---

## 🟡 Layer 2 — Practitioner Track: Core Reflection & Dynamic Proxies

### 1. Three Ways to Obtain `Class<?>`
1. `String.class` (compile-time class literal — safest & fastest)
2. `obj.getClass()` (from an existing instance)
3. `Class.forName("com.example.Driver")` (dynamic string loading — triggers static initialization)

### 2. `getMethods()` vs `getDeclaredMethods()`
- `getMethods()` -> Only `public` methods, **including inherited** ones (`toString()`, `equals()`).
- `getDeclaredMethods()` -> **All visibility levels** (`private`, `protected`, package, `public`), **only declared directly in this class** (no inherited methods).

### 3. Dynamic Proxies (`java.lang.reflect.Proxy`)
JDK Dynamic Proxies generate a runtime implementation of one or more **interfaces**, funneling every method call into a single `InvocationHandler.invoke(proxy, method, args)`:
- **How Spring `@Transactional`, `@Cacheable`, and `@Async` work:** Spring wraps your service in a proxy that starts a DB transaction *before* calling `method.invoke(target, args)` and commits/rolls back *after*!

---

## 🔴 Layer 3 — Senior / JVM Track: Performance, JPMS Encapsulation & `MethodHandles`

1. **Performance Cost:** Reflective calls bypass early JIT static binding and box primitive arguments/varargs arrays. In Java 18+ (`JEP 416`), Core Reflection was reimplemented on top of **`MethodHandles`**, cutting cold-call overhead and unifying JIT inlining once warmed up.
2. **JPMS Strong Encapsulation (`JEP 403`, Java 17+):** Calling `field.setAccessible(true)` on JDK internal classes (like `java.util.HashMap.table`) throws `InaccessibleObjectException` unless the JVM is launched with `--add-opens java.base/java.util=ALL-UNNAMED`.
3. **JDK Proxy vs CGLIB:** JDK `Proxy` requires the target to implement an **interface**. When a Spring Bean has no interface, Spring uses **CGLIB / ByteBuddy** to generate a runtime subclass of the target (which is why `final` classes and `final` methods cannot be proxied by Spring AOP!).
