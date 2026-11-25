package oops.c03_encapsulation;

import java.time.LocalDate;

public class GettersAndSetters {
    public static void main(String[] args) {
        Aircraft aircraft = new Aircraft("Warfare", 10000, LocalDate.of(2002, 1, 1));
        System.out.println(aircraft.getRange() + "KM");
        System.out.println(aircraft.getType());
        System.out.println(aircraft.getModel());    
    }
}

/**
 * Aircraft
 * Since the field are locked/encapsulated by private keywords
 * we still need to access & modify the data with boundries,
 * there the getters & setters comes into play 
 * They allows the access & modification of data & even put conditions if required
 */
class Aircraft{
    private String type;
    private double range;
    private LocalDate Model;
    
    public Aircraft(String type, double range, LocalDate model) {
        this.type = type;
        this.range = range;
        Model = model;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public double getRange() {
        return range;
    }
    public void setRange(double range) {
        this.range = range;
    }
    public LocalDate getModel() {
        return Model;
    }
    public void setModel(LocalDate model) {
        Model = model;
    }

}