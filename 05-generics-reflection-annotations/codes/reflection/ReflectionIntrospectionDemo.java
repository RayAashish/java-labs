package reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * RUNTIME REFLECTION & MODERN MethodHandles
 *
 * Covers:
 * - getMethods() (public + inherited) vs getDeclaredMethods() (all visibility, this class only)
 * - Accessing & mutating private fields and invoking private constructors/methods
 * - High-performance modern MethodHandles (used internally by Java 18+ Reflection!)
 */
public class ReflectionIntrospectionDemo {

    static class SecretVault {
        private String secretCode = "ALPHA-007";

        private SecretVault() {}

        private String decrypt(String salt) {
            return secretCode + ":" + salt;
        }
    }

    public static void main(String[] args) throws Throwable {
        Class<SecretVault> clazz = SecretVault.class;

        System.out.println("=== 1. getMethods() vs getDeclaredMethods() ===");
        System.out.println("Public + inherited count (getMethods):    " + clazz.getMethods().length);
        System.out.println("Declared in SecretVault (getDeclared):    " + clazz.getDeclaredMethods().length);

        System.out.println("\n=== 2. Invoking Private Constructor, Field & Method ===");
        Constructor<SecretVault> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        SecretVault vault = ctor.newInstance();

        Field codeField = clazz.getDeclaredField("secretCode");
        codeField.setAccessible(true);
        System.out.println("Original private secretCode: " + codeField.get(vault));
        codeField.set(vault, "OMEGA-999");

        Method decryptMethod = clazz.getDeclaredMethod("decrypt", String.class);
        decryptMethod.setAccessible(true);
        System.out.println("Private method output:       " + decryptMethod.invoke(vault, "SALT_42"));

        System.out.println("\n=== 3. MethodHandles (Modern Low-Overhead Dynamic Invocation) ===");
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(SecretVault.class, MethodHandles.lookup());
        MethodHandle mh = lookup.findVirtual(
                SecretVault.class, "decrypt", MethodType.methodType(String.class, String.class));
        System.out.println("MethodHandle result:         " + (String) mh.invokeExact(vault, "FAST_PATH"));
    }
}
