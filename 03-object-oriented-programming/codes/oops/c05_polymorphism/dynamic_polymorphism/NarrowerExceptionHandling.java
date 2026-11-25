package oops.c05_polymorphism.dynamic_polymorphism;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Demonstrates Overriding Checked Exception Rules (Liskov Substitution Principle):
 * - Child may throw the SAME checked exception, a NARROWER subclass, or NONE at all.
 * - Child may NEVER throw a BROADER checked exception (fails compile time).
 */
public class NarrowerExceptionHandling {
    public static void main(String[] args) throws IOException {
        Books b1 = new ScifiBooks();
        Books b2 = new NovelBooks();
        b1.bookType();
        b2.bookType();
    }

    static class Books {
        void bookType() throws IOException {
            System.out.println("Generic Book");
        }
    }

    static class ScifiBooks extends Books {
        @Override
        void bookType() throws FileNotFoundException { // Narrower subclass of IOException -> VALID
            System.out.println("Sci-Fi Books (throws narrower FileNotFoundException)");
        }
    }

    static class NovelBooks extends Books {
        @Override
        void bookType() { // Throws NO checked exception -> VALID
            System.out.println("Novel Books (throws nothing)");
        }
    }
}
