package foundations.control.loops;

import java.util.List;

public class EnhancedForLoop {

    /**
     * @param planets
     * enhanced for loop is specially used with the Object datatype
     */
    private static void enhancedForLoop(List<Planet> planets){
        for (Planet planet : planets){
            System.out.println(planet.getName());
        }
    }
    public static void main(String[] args) {
        Planet p1 = new Planet("Earth");
        Planet p2 = new Planet("Mars");
        Planet p3 = new Planet("Keplar");
        List<Planet> planets = List.of(p1, p2, p3);
        enhancedForLoop(planets);
    }
}

class Planet{
    private String name;

    public Planet(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
}