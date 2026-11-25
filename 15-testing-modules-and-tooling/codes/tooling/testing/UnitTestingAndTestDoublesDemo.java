package tooling.testing;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Pure-JDK demonstration of Clean Unit Testing principles (AAA pattern)
 * and Test Doubles: Fake, Stub, and Spy without external frameworks.
 */
public class UnitTestingAndTestDoublesDemo {

    interface UserRepository {
        Optional<String> findEmailById(long userId);
        void save(long userId, String email);
    }

    interface EmailSender {
        void send(String to, String subject);
    }

    static class OnboardingService {
        private final UserRepository repo;
        private final EmailSender emailSender;

        OnboardingService(UserRepository repo, EmailSender emailSender) {
            this.repo = repo;
            this.emailSender = emailSender;
        }

        public boolean registerUser(long id, String email) {
            if (repo.findEmailById(id).isPresent()) {
                return false;
            }
            repo.save(id, email);
            emailSender.send(email, "Welcome to Java-Labs!");
            return true;
        }
    }

    // FAKE: Working in-memory repository (far cleaner than heavy mocking!)
    static class InMemoryUserRepo implements UserRepository {
        private final Map<Long, String> store = new HashMap<>();
        public Optional<String> findEmailById(long userId) { return Optional.ofNullable(store.get(userId)); }
        public void save(long userId, String email) { store.put(userId, email); }
    }

    // SPY: Records side-effect invocations for verification
    static class SpyEmailSender implements EmailSender {
        int sentCount = 0;
        String lastRecipient;

        public void send(String to, String subject) {
            sentCount++;
            lastRecipient = to;
        }
    }

    public static void main(String[] args) {
        testRegisterNewUserSendsWelcomeEmail();
        testDuplicateUserSkipsEmail();
        System.out.println("ALL UNIT TESTS PASSED!");
    }

    private static void testRegisterNewUserSendsWelcomeEmail() {
        // Arrange
        InMemoryUserRepo fakeRepo = new InMemoryUserRepo();
        SpyEmailSender spyEmail = new SpyEmailSender();
        OnboardingService service = new OnboardingService(fakeRepo, spyEmail);

        // Act
        boolean created = service.registerUser(101L, "aashish@example.com");

        // Assert
        assertTrue(created, "New user should register successfully");
        assertEquals(1, spyEmail.sentCount, "Exactly 1 welcome email should be sent");
        assertEquals("aashish@example.com", spyEmail.lastRecipient, "Email recipient mismatch");
    }

    private static void testDuplicateUserSkipsEmail() {
        // Arrange
        InMemoryUserRepo fakeRepo = new InMemoryUserRepo();
        fakeRepo.save(101L, "existing@example.com");
        SpyEmailSender spyEmail = new SpyEmailSender();
        OnboardingService service = new OnboardingService(fakeRepo, spyEmail);

        // Act
        boolean created = service.registerUser(101L, "existing@example.com");

        // Assert
        assertTrue(!created, "Duplicate user must not re-register");
        assertEquals(0, spyEmail.sentCount, "No email should be sent for duplicate user");
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError("Assertion failed: " + msg);
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError(msg + " | expected=" + expected + ", actual=" + actual);
        }
    }
}
