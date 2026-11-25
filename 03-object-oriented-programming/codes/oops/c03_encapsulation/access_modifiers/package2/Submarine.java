package oops.c03_encapsulation.access_modifiers.package2;

public class Submarine{
    protected String type;
    protected double maxDiveCapacity;
    protected  String getType() {
        return type;
    }
    protected  void setType(String type) {
        this.type = type;
    }
    protected  double getMaxDiveCapacity() {
        return maxDiveCapacity;
    }
    protected  void setMaxDiveCapacity(double maxDiveCapacity) {
        this.maxDiveCapacity = maxDiveCapacity;
    }
    
}