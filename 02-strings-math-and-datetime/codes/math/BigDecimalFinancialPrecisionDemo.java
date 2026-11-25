package math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Demonstrates why double fails for financial math and how BigDecimal
 * & BigInteger guarantee exact precision.
 */
public class BigDecimalFinancialPrecisionDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. IEEE-754 double Precision Trap ===");
        double dSum = 0.1 + 0.2;
        System.out.println("double: 0.1 + 0.2 = " + dSum + " (equals 0.3? " + (dSum == 0.3) + ")");

        System.out.println("\n=== 2. BigDecimal Constructor Trap ===");
        BigDecimal badFromDouble = new BigDecimal(0.1);   // NEVER do this!
        BigDecimal goodFromString = new BigDecimal("0.1"); // ALWAYS pass String or valueOf()
        System.out.println("new BigDecimal(0.1):   " + badFromDouble);
        System.out.println("new BigDecimal(\"0.1\"): " + goodFromString);

        System.out.println("\n=== 3. equals() vs compareTo() Scale Trap ===");
        BigDecimal priceA = new BigDecimal("100.0");
        BigDecimal priceB = new BigDecimal("100.00");
        System.out.println("priceA.equals(priceB) [checks scale too!]:     " + priceA.equals(priceB));
        System.out.println("priceA.compareTo(priceB) == 0 [numeric value]: " + (priceA.compareTo(priceB) == 0));

        System.out.println("\n=== 4. Exact Division with RoundingMode.HALF_EVEN ===");
        BigDecimal total = new BigDecimal("100.00");
        BigDecimal installments = new BigDecimal("3");
        BigDecimal perInstallment = total.divide(installments, 2, RoundingMode.HALF_EVEN);
        System.out.println("$100.00 / 3 (Banker's Rounding): $" + perInstallment);

        System.out.println("\n=== 5. BigInteger Beyond 64-bit Long.MAX_VALUE ===");
        BigInteger factorial50 = BigInteger.ONE;
        for (int i = 2; i <= 50; i++) {
            factorial50 = factorial50.multiply(BigInteger.valueOf(i));
        }
        System.out.println("50! = " + factorial50);
    }
}
