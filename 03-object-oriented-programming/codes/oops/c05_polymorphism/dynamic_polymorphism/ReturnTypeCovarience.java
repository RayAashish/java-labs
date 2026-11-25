package oops.c05_polymorphism.dynamic_polymorphism;

/**
 * Demonstrates Covariant Return Types in method overriding:
 * A subclass override can return a narrower subtype of the parent's return type.
 */
public class ReturnTypeCovarience {
    public static void main(String[] args) {
        ShelterAnimal parentRef = new RescueDog();
        ShelterAnimal offspring = parentRef.reproduce();
        System.out.println("Runtime offspring type: " + offspring.getClass().getSimpleName());

        RescueDog dog = new RescueDog();
        RescueDog puppy = dog.reproduce(); // No cast needed due to covariant return type!
        System.out.println("Typed directly as RescueDog: " + puppy.getClass().getSimpleName());
    }

    static class ShelterAnimal {
        ShelterAnimal reproduce() {
            return new ShelterAnimal();
        }
    }

    static class RescueDog extends ShelterAnimal {
        @Override
        RescueDog reproduce() {
            return new RescueDog();
        }
    }
}
