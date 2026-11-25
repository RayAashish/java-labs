package oops.c03_encapsulation.access_modifiers;

import oops.c03_encapsulation.access_modifiers.package2.Submarine;

public class Protected {
    /**
     * @param args
     * Can't access the submarine because it's in the
     * different package
     */
    public static void main(String[] args) {
        Submarine submarine = new Submarine();
        // submarine.setType("War");
        // String type = submarine.type();
        
    }
}
