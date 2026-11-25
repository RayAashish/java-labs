package reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * Demonstrates Runtime Reflection and JDK Dynamic Proxies
 * (the core mechanism behind Spring AOP @Transactional & Mockito).
 */
public class ReflectionAndDynamicProxyDemo {

    interface PaymentService {
        String charge(String orderId, double amount);
    }

    static class StripePaymentService implements PaymentService {
        @SuppressWarnings("unused")
        private final String apiKey = "sk_live_secret_hidden";

        @Override
        public String charge(String orderId, double amount) {
            return "Charged $" + amount + " for " + orderId;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. Inspecting Class & Private Field via Reflection ===");
        StripePaymentService realService = new StripePaymentService();
        Class<?> clazz = realService.getClass();
        System.out.println("Class: " + clazz.getSimpleName());

        Field apiKeyField = clazz.getDeclaredField("apiKey");
        apiKeyField.setAccessible(true); // Works on user classes inside unnamed module
        System.out.println("Private field apiKey value: " + apiKeyField.get(realService));

        System.out.println("\n=== 2. JDK Dynamic Proxy (Mini AOP Timing & Audit Interceptor) ===");
        PaymentService proxy = createAuditProxy(realService, PaymentService.class);
        String receipt = proxy.charge("ORD-9001", 149.99);
        System.out.println("Caller received: " + receipt);
    }

    @SuppressWarnings("unchecked")
    private static <T> T createAuditProxy(T target, Class<T> interfaceType) {
        InvocationHandler handler = (Object proxyObj, Method method, Object[] args) -> {
            long startNs = System.nanoTime();
            System.out.println("[PROXY BEFORE] " + method.getName() + " args=" + Arrays.toString(args));
            Object result = method.invoke(target, args);
            long elapsedUs = (System.nanoTime() - startNs) / 1_000;
            System.out.println("[PROXY AFTER]  " + method.getName() + " took " + elapsedUs + " us");
            return result;
        };
        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                handler
        );
    }
}
