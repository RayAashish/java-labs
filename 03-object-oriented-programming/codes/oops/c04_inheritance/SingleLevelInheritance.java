package oops.c04_inheritance;

public class SingleLevelInheritance {
    public static void main(String[] args) {
        B b = new B(123, "Something");
        b.someMethod();

        A a = new B(321, "some other string");
        a.someMethod(); 
    }
}

class A{
    int someField;

    A(int someField){
        this.someField = someField;
    }

    void someMethod(){
        System.out.println("Some method of A");
    }
}

class B extends A{
    String someOtherField;

    B(int someInteger, String someString){
        super(someInteger);
        this.someOtherField = someString;
    }

    @Override 
    void someMethod(){
        System.out.println("Some method of B");
    }
}