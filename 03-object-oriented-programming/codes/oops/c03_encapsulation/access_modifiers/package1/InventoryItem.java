package oops.c03_encapsulation.access_modifiers.package1;

import java.util.ArrayList;
import java.util.List;

/**
 * InventoryItem
 * It's based on : Bags in gaming. We have limited no. of solt in the bag
 * we can't fill more than that
 */
public class InventoryItem {
    private final int bagSize = 6;
    private List<Item> items = new ArrayList<>(); //Breaking our SOLID principals but I am just exploring things


    public void addItem(Item item){
        if (this.items.size() >= bagSize)
            throw new RuntimeException("Bag is full! Remove item to add new item in the bag");
        this.items.add(item);
    }
    public void removeItem(String itemName){
        if (items.size() == 0)
            throw new RuntimeException("The bag is empty");
        for (int i = 0; i < items.size(); i++){
            if (itemName.equals(items.get(i).getItemName()))
                items.remove(i);
        }
    }
    public void showItemsInInventory(){
        for (Item item : this.items){
            System.out.println(item.toString());
        }
    }

    
}

class Item{
    private String itemName;

    public Item(String itemName) {
        this.itemName = itemName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    @Override
    public String toString() {
        return "Item [itemName=" + itemName + "]";
    }
    
}
