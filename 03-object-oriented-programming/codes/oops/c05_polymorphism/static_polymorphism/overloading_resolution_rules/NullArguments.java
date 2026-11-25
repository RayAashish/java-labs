package oops.c05_polymorphism.static_polymorphism.overloading_resolution_rules;

public class NullArguments {
    public static void main(String[] args) {
        new Box1().show(null);;
    }
}


class Box1{
    void show(String s){
        System.out.println("String");
    }

    void show(int a){ //Primitive type doens't support null values
        System.out.println("int");
    }

    void show(Object o){
        System.out.println("Object");
    }
}