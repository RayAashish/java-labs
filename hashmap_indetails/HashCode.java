package hashmap_indetails;

public class HashCode {
    public static void main(String[] args) {
        String[] names = {"Aashish", "Bunu", "Bhanu", "Aarav", "Gudiya", "Kenish"};
        for (String name : names){
            System.err.println(name + " : " + name.hashCode());
        }
    }
}
