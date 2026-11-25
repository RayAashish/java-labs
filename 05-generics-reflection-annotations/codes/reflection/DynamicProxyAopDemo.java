package reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * JDK DYNAMIC PROXIES (How Spring AOP, @Transactional & Mockito Work!)
 *
 * WHAT IT IS:
 *   Generates a runtime implementation of one or more interfaces where every method call is
 *   intercepted by an `InvocationHandler` before/after delegating to the real object.
 *
 * WHY IT MATTERS:
 *   Cross-cutting infrastructure (logging, timing metrics, transactions, retry, auth checks)
 *   stays completely decoupled from business classes.
 *   LIMITATION: JDK Proxy ONLY works on interfaces! (Spring uses CGLIB/ByteBuddy for plain classes.)
 */
public class DynamicProxyAopDemo {

    interface OrderService {
        String placeOrder(String sku, int quantity);
    }

    static class RealOrderService implements OrderService {
        @Override
        public String placeOrder(String sku, int quantity) {
            return "CONFIRMED(" + quantity + "x " + sku + ")";
        }
    }

    public static void main(String[] args) {
        OrderService realService = new RealOrderService();
        OrderService proxy = wrapWithTransactionAndTimingProxy(realService, OrderService.class);

        System.out.println("Proxy runtime class name: " + proxy.getClass().getName());
        String result = proxy.placeOrder("SKU-KEYBOARD", 2);
        System.out.println("Caller received: " + result);
    }

    @SuppressWarnings("unchecked")
    private static <T> T wrapWithTransactionAndTimingProxy(T target, Class<T> iface) {
        InvocationHandler interceptor = (Object proxyObj, Method method, Object[] args) -> {
            long startNs = System.nanoTime();
            System.out.println("[AOP BEGIN TX]  " + method.getName() + " args=" + Arrays.toString(args));
            try {
                Object result = method.invoke(target, args);
                System.out.println("[AOP COMMIT TX] " + method.getName()
                        + " (" + (System.nanoTime() - startNs) / 1_000 + " us)");
                return result;
            } catch (Exception ex) {
                System.out.println("[AOP ROLLBACK]  " + ex.getCause());
                throw ex.getCause();
            }
        };
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, interceptor);
    }
}
