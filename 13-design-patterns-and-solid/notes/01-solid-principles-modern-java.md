# 01. SOLID Principles in Modern Java

---

## 🟢 Layer 1 — Noob Track: Five Rules for Clean Code

| Letter | Principle | Plain-English Meaning |
|---|---|---|
| **S** | Single Responsibility (SRP) | One class = one cohesive job (don't mix SQL queries, PDF rendering, and email sending in one `OrderManager`!). |
| **O** | Open/Closed (OCP) | Open for **extension**, closed for **modification** (add a new discount rule by adding a class/lambda, not editing a giant `if-else` block). |
| **L** | Liskov Substitution (LSP) | Any child class must safely work anywhere the parent class is expected without surprising crashes (`UnsupportedOperationException`). |
| **I** | Interface Segregation (ISP) | Don't force a class to implement 10 methods it doesn't need; split fat interfaces into small role interfaces. |
| **D** | Dependency Inversion (DIP) | High-level business logic should depend on an `interface` (`PaymentGateway`), not `new StripeSdkClient()` directly. |

---

## 🟡 Layer 2 — Practitioner Track: Real-World Violation & Fix

### Classic LSP Trap: `ReadOnlyDocument` extending `EditableDocument`
If `Document` defines `save()`, and `ReadOnlyDocument extends Document` throws `UnsupportedOperationException` in `save()`, callers holding `Document` crash unexpectedly!
**Fix (ISP + LSP):** Separate `ReadableDocument` (`read()`) from `WritableDocument extends ReadableDocument` (`save()`).

### OCP + DIP Together
Instead of:
```java
// Violates OCP & DIP: adding UPI requires modifying CheckoutService!
if (type.equals("CARD")) { new StripeClient().charge(); }
else if (type.equals("PAYPAL")) { new PayPalClient().pay(); }
```
Inject `PaymentProcessor` interface (or `Map<PaymentType, PaymentProcessor>` in Spring!) so adding `UpiProcessor` requires **zero edits** to `CheckoutService`.

---

## 🔴 Layer 3 — Senior Track: SOLID Trade-offs & Over-Engineering

- **SRP != "One method per class":** SRP is about **actors / reasons to change** (Robert C. Martin). Splitting every 10-line helper into 12 files creates "lasagna code" with worse readability than a cohesive 80-line class.
- **LSP & Java Overriding Rules:** Notice how Java enforces LSP at compile time for checked exceptions and access modifiers (child cannot throw broader checked exceptions or narrow visibility!).
- **DIP enables Testability:** Constructor injection (`OrderService(PaymentGateway gateway)`) lets unit tests pass an in-memory Fake with zero network I/O or Mockito reflection hacks.
