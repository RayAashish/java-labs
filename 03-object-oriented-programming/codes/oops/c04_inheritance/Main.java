package oops.c04_inheritance;


public class Main {
    public static void main(String[] args) {
        Bike bike = new Bike();
        bike.speed = 120;
        bike.move();
    }
}

class Vehicle{
    int speed = 0;
    void move(){
        System.out.println("Vehicle is moving at the speed : " + this.speed);
    }
}

class Bike extends Vehicle{
    void sound(){
        System.out.println("peep peep");
    }
}