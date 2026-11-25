package oops.c08_object_essentials;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Demonstrates java.lang.Object contracts:
 * 1. Clean developer-friendly toString().
 * 2. Proper equals() + hashCode() symmetry, reflexivity, transitivity, and HashMap safety.
 * 3. What breaks when equals() is overridden without hashCode().
 * 4. Copy constructor as the modern replacement for broken Cloneable.
 */
public class ObjectContractsDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. Proper equals() & hashCode() in HashMap ===");
        StarCoordinates c1 = new StarCoordinates(10, 20, 30);
        StarCoordinates c2 = new StarCoordinates(10, 20, 30);
        System.out.println("c1: " + c1);
        System.out.println("c1.equals(c2): " + c1.equals(c2));
        System.out.println("Same hashCode? " + (c1.hashCode() == c2.hashCode()));

        Map<StarCoordinates, String> starCatalog = new HashMap<>();
        starCatalog.put(c1, "Alpha Centauri");
        System.out.println("Lookup using new identical StarCoordinates: " + starCatalog.get(c2));

        System.out.println("\n=== 2. Broken Contract Trap (equals without hashCode) ===");
        BrokenSpell k1 = new BrokenSpell("FIREBALL");
        BrokenSpell k2 = new BrokenSpell("FIREBALL");
        Map<BrokenSpell, Integer> manaCosts = new HashMap<>();
        manaCosts.put(k1, 50);
        System.out.println("k1.equals(k2) is true, yet map.get(k2) returns: " + manaCosts.get(k2));

        System.out.println("\n=== 3. Safe Copy Constructor vs Cloneable ===");
        StarCoordinates copy = StarCoordinates.copyOf(c1);
        System.out.println("Copied coordinates: " + copy + " (distinct instance: " + (copy != c1) + ")");
    }

    static final class StarCoordinates {
        private final int x;
        private final int y;
        private final int z;

        StarCoordinates(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        // Copy constructor — clean, type-safe, zero Cloneable pitfalls
        StarCoordinates(StarCoordinates other) {
            this(other.x, other.y, other.z);
        }

        static StarCoordinates copyOf(StarCoordinates other) {
            return new StarCoordinates(Objects.requireNonNull(other));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StarCoordinates that = (StarCoordinates) o;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z); // Must hash the exact same fields checked in equals()
        }

        @Override
        public String toString() {
            return String.format("StarCoordinates(%d, %d, %d)", x, y, z);
        }
    }

    static class BrokenSpell {
        private final String spellId;

        BrokenSpell(String spellId) {
            this.spellId = spellId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BrokenSpell other)) return false;
            return Objects.equals(spellId, other.spellId);
        }
        // BUG: Forgot hashCode()! Two equal keys fall into different HashMap buckets.
    }
}
