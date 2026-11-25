package oops.c03_encapsulation.access_modifiers;

import oops.c03_encapsulation.access_modifiers.package2.SmartLock;

/**
 * Public
 * public classes are accessible anywhere & there are no restriction on it's acessing
 * (withing pacakge, outside package, anywhere)
 */
public class Public {
    /**
     * @param args
     * We can Access the InventoryItem class as well as SmartLock class which is outside the packge
     */
    public static void main(String[] args) {
        // InventoryItem bag = new InventoryItem();
        // bag.addItem(new Item("Ammo 10mm"));
        // bag.addItem(new Item("Grenade"));
        // bag.addItem(new Item("Smoke Bomb"));
        // bag.addItem(new Item("8X Scope"));
        // bag.addItem(new Item("Ammo 24mm"));
        // bag.addItem(new Item("Conclusion smoke"));
        // bag.removeItem("Grenade");
        // bag.removeItem("Ammo 24mm");
        // bag.addItem(new Item("Hover Board"));

        SmartLock smartLock = new SmartLock();
        System.out.println(smartLock.isLocked());
        // smartLock.unlock("anasdc");
        // smartLock.unlock("anasdc");
        // smartLock.unlock("anasdc");
        // smartLock.unlock("anasdc");
        smartLock.unlock("ABC");

    }
}
