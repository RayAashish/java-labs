package oops.c03_encapsulation;

public class ImmutableClass {
    public static void main(String[] args) {
        Phone phone = new Phone("Xiaomi", "Xiaomi 14", 12, 512);
        System.out.println(phone.toString());
    }
}


/**
 * Phone
 * Since there are no setters so the field once assigned can't be changed
 * Only one Constructor forces to pass the fields to create the Object
 * Once the object is created, the state can't be changed
 */
class Phone{
    private final String brand;
    private final String modelName;
    private final int ram;
    private final int internalStorage;
    public Phone(String brand, String modelName, int ram, int internalStorage){
        this.brand = brand;
        this.modelName = modelName;
        this.ram = ram;
        this.internalStorage = internalStorage;
    }
    public String getBrand() {
        return brand;
    }
    public String getModelName() {
        return modelName;
    }
    public int getRam() {
        return ram;
    }
    public int getInternalStorage() {
        return internalStorage;
    }
    @Override
    public String toString() {
        return "Phone [brand=" + brand + ", modelName=" + modelName + ", ram=" + ram + ", internalStorage="
                + internalStorage + "]";
    }
    
}