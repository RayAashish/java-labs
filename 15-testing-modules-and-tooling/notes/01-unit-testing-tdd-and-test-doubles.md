# 01. Unit Testing, Assertions & Test Doubles (Stub, Fake, Mock, Spy)

---

## 🟢 Layer 1 — Noob Track: Why We Write Automated Tests

Manual testing ("run `main()`, check console") breaks the moment your project has 50 classes. **Unit tests** are tiny automated programs that test one unit (class/method) in isolation in milliseconds!

Every test follows **AAA**:
1. **Arrange:** Create test data and dependencies.
2. **Act:** Call the method you are testing.
3. **Assert:** Verify the return value or exception.

*(Note: Java's built-in `assert condition : msg;` keyword is disabled by default unless `-ea` is passed; in real projects use **JUnit 5** `assertEquals`, `assertThrows`, `assertAll`!)*

---

## 🟡 Layer 2 — Practitioner Track: Test Doubles (No More Mock Confusion!)

| Test Double | Purpose | Example |
|---|---|---|
| **Dummy** | Passed just to fill a parameter; never called | `new Logger()` that does nothing |
| **Fake** | Working lightweight implementation (fastest & cleanest!) | `InMemoryUserRepository` backed by a `HashMap` instead of MySQL |
| **Stub** | Returns hardcoded canned answers for a test | `Clock.fixed(...)`, or `when(fxClient.rate()).thenReturn(84.0)` |
| **Spy** | Wraps a real/stubbed object and records how many times methods were called | Checking `emailSpy.sentEmails.size() == 1` |
| **Mock** | Dynamic test double verifying specific method interactions | `verify(paymentGateway, times(1)).charge(...)` |

### JUnit 5 & Mockito Quick Reference
- `@Test`, `@BeforeEach` / `@AfterEach`, `@ParameterizedTest` (`@ValueSource`, `@CsvSource`)
- `@Mock` (empty mock) vs `@Spy` (real object with selective stubbing — use `doReturn(...).when(spy).method()`) vs `@InjectMocks`.

---

## 🔴 Layer 3 — Senior Track: Testing Strategy & Pitfalls

1. **Test Pyramid:** Many fast pure-Java **Unit Tests** (ms) -> targeted **Integration Tests** (Testcontainers with real PostgreSQL/Kafka!) -> few **E2E tests**.
2. **Prefer Fakes over Over-Mocking:** Heavy mocking binds tests tightly to *how* a method calls internal dependencies, breaking tests on every safe refactor. Testing against an `InMemoryRepository` Fake tests real behavior!
3. **`@Transactional` Test Rollback Gotcha in Spring:** Annotated `@SpringBootTest` tests roll back DB inserts after each test, hiding lazy-loading (`LazyInitializationException`) and post-commit listener bugs!
