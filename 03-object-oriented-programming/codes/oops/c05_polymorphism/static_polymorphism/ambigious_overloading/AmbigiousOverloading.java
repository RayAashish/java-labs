package oops.c05_polymorphism.static_polymorphism.ambigious_overloading;

public class AmbigiousOverloading {
    /**
     * @param args
     * 
     * Won't compile because the compiler isn't able to
     * distinguish between int & long because it's not specified
     */
    public static void main(String[] args) {
        
        // new Box().show(2, 3); // Won't compile because of ambiguity
        new Box().show(2l, 9); // Will compike & run
    }
}

class Box{
    void show (int a, long b){
        System.out.println("int, long");
    }

    void show(long a, int b){
        System.out.println("long, int");
    }
}