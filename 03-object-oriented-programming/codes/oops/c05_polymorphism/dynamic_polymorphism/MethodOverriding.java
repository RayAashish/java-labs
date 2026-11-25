package oops.c05_polymorphism.dynamic_polymorphism;

/**
 * Demonstrates Runtime Polymorphism (Dynamic Method Dispatch via vtable).
 * The JVM resolves the overridden method based on the Heap object's runtime type.
 */
public class MethodOverriding {
    public static void main(String[] args) {
        Animal[] animals = {new Dog(), new Cat(), new Cow()};
        for (Animal a : animals) {
            a.makeSound(); // Dynamic dispatch calls Dog, Cat, Cow implementations
        }
    }

    static class Animal {
        void makeSound() {
            System.out.println("Generic sound");
        }
    }

    static class Dog extends Animal {
        @Override
        void makeSound() {
            System.out.println("Dog: bhow bhow");
        }
    }

    static class Cat extends Animal {
        @Override
        void makeSound() {
            System.out.println("Cat: meow meow");
        }
    }

    static class Cow extends Animal {
        @Override
        void makeSound() {
            System.out.println("Cow: mooo mooo");
        }
    }
}
