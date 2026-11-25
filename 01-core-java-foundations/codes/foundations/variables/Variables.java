package foundations.variables;

/**
 * Demonstrates Java variable declarations, naming rules, scope lifetimes
 * (local vs instance vs static), and Java 10+ local type inference (var).
 */
public class Variables {

    private static final String REPO_NAME = "java-labs"; // Static constant in Metaspace
    private String instanceTag = "Foundation-Module";     // Instance field on Heap

    public static void main(String[] args) {
        // Local variables live on the Thread Stack and MUST be initialized before use
        int age = 24;
        String name = "Aashish";
        double exactSalary = 27000.78d;
        String fullName = "Aashish Kumar Ray";

        // Java 10+ compile-time local type inference ('var')
        var languageVersion = 25;

        Variables demo = new Variables();
        System.out.println("=== Variables & Scope Demo ===");
        System.out.println("Repo (static):     " + REPO_NAME);
        System.out.println("Tag (instance):    " + demo.instanceTag);
        System.out.println("Name (local):      " + name + " (" + fullName + ")");
        System.out.println("Age & Salary:      " + age + ", $" + exactSalary);
        System.out.println("Inferred via var:  Java " + languageVersion);
    }
}
