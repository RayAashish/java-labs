package oops.c03_encapsulation.access_modifiers;

/**
 * Private
 * The methods, instances all remain within the class itself, outside body can't access it
 */
public class Private {
    Coffee coffee = new Coffee();
    // coffee.makingCoffee(); Can't access the method or field because they are private
}

class Coffee{
    private String recipeName;
    private int temp;

    private void makingCoffee(){
        System.out.println("Coffee is being made");
    }
}