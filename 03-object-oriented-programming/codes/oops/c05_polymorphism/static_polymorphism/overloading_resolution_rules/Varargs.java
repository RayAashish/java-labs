package oops.c05_polymorphism.static_polymorphism.overloading_resolution_rules;


public class Varargs {
    public static void main(String[] args) {
        new Box2().show(2, 1, 9);
        new Box2().show(2, 3, 4, 8);
        new Box2().show(1);
    }
}

class Box2{
    void show(int a, int b, int c){
        System.out.println("int");
    }

    void show (int... nums){
        System.out.println("Variable arguments");
    }
}