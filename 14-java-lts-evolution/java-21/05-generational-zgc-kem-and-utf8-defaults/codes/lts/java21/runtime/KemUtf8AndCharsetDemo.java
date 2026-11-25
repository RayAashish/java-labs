package lts.java21.runtime;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import javax.crypto.KEM;
import javax.crypto.SecretKey;

/**
 * Java 21 LTS (covering Java 18-21) — UTF-8 Default (JEP 400) & KEM API (JEP 452).
 */
public class KemUtf8AndCharsetDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 21 UTF-8 Default (JEP 400) & KEM API (JEP 452) ===");

        // -------------------------------------------------------------
        // 1. JEP 400 UTF-8 by Default across every OS / Container
        // -------------------------------------------------------------
        System.out.println("[1] Charset.defaultCharset() = " + Charset.defaultCharset()
                + " (Guaranteed UTF-8 on Windows/macOS/Linux since JDK 18!)");
        System.out.println("    native.encoding = " + System.getProperty("native.encoding"));
        assert StandardCharsets.UTF_8.equals(Charset.defaultCharset());

        // -------------------------------------------------------------
        // 2. JEP 452 Key Encapsulation Mechanism (KEM) End-to-End Handshake
        // -------------------------------------------------------------
        // Receiver generates asymmetric X25519 key pair (works identically when switching to ML-KEM!)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
        KeyPair aliceKeys = kpg.generateKeyPair();

        KEM kem = KEM.getInstance("DHKEM");

        // Sender (Bob) encapsulates a shared symmetric secret using Alice's public key
        KEM.Encapsulator bobEncapsulator = kem.newEncapsulator(aliceKeys.getPublic());
        KEM.Encapsulated encapsulated = bobEncapsulator.encapsulate();
        SecretKey bobSessionKey = encapsulated.key();
        byte[] networkCapsule = encapsulated.encapsulation();

        // Receiver (Alice) decapsulates using her private key -> derives identical shared key
        KEM.Decapsulator aliceDecapsulator = kem.newDecapsulator(aliceKeys.getPrivate());
        SecretKey aliceSessionKey = aliceDecapsulator.decapsulate(networkCapsule);

        boolean keysMatch = Arrays.equals(bobSessionKey.getEncoded(), aliceSessionKey.getEncoded());
        System.out.printf("%n[2] DHKEM Handshake complete -> capsuleBytes=%d | sharedKeyAlgo=%s | identical=%s%n",
                networkCapsule.length, aliceSessionKey.getAlgorithm(), keysMatch);
    }
}
