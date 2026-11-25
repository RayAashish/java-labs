package functional.streams;

import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * COLLECTORS COMPLETE MASTERY
 *
 * Covers:
 * - toMap() duplicate key crash vs 3-arg merge + 4-arg mapFactory
 * - groupingBy() (1-arg, 2-arg downstream, 3-arg sorted TreeMap!)
 * - partitioningBy() (guaranteed true & false buckets)
 * - mapping(), filtering() (Java 9), collectingAndThen(), teeing() (Java 12)
 * - joining() & summarizingDouble()
 */
public class CollectorsCompleteMasteryDemo {

    record Employee(String id, String name, String dept, double salary) {}

    public static void main(String[] args) {
        List<Employee> staff = List.of(
                new Employee("E1", "Aashish", "Platform", 160_000),
                new Employee("E2", "Meera", "Platform", 145_000),
                new Employee("E3", "Rohan", "Payments", 130_000),
                new Employee("E4", "Sara", "Payments", 95_000),
                new Employee("E1", "Aashish-Updated", "Platform", 170_000) // Duplicate key E1!
        );

        System.out.println("=== 1. Collectors.toMap() Duplicate Key Trap & Fix ===");
        try {
            staff.stream().collect(Collectors.toMap(Employee::id, Employee::name));
        } catch (IllegalStateException ex) {
            System.out.println("BAD 2-arg toMap crashed on duplicate key: " + ex.getMessage());
        }
        Map<String, String> idToName = staff.stream().collect(Collectors.toMap(
                Employee::id,
                Employee::name,
                (oldVal, newVal) -> newVal, // Merge strategy: keep latest
                LinkedHashMap::new          // Preserve insertion order
        ));
        System.out.println("GOOD 4-arg toMap: " + idToName);

        List<Employee> cleanStaff = staff.subList(0, 4);

        System.out.println("\n=== 2. groupingBy() with Downstream & Sorted TreeMap ===");
        Map<String, List<String>> namesByDeptSorted = cleanStaff.stream().collect(Collectors.groupingBy(
                Employee::dept,
                TreeMap::new, // Sort departments alphabetically!
                Collectors.mapping(Employee::name, Collectors.toList())
        ));
        System.out.println("Names by dept (TreeMap): " + namesByDeptSorted);

        // Java 9 Collectors.filtering keeps empty group keys intact (unlike .filter() before grouping!)
        Map<String, Set<String>> highEarnersByDept = cleanStaff.stream().collect(Collectors.groupingBy(
                Employee::dept,
                Collectors.filtering(e -> e.salary() > 140_000, Collectors.mapping(Employee::name, Collectors.toSet()))
        ));
        System.out.println("High earners >140k per dept (Payments kept as []): " + highEarnersByDept);

        System.out.println("\n=== 3. partitioningBy() & joining() ===");
        Map<Boolean, List<Employee>> seniorSplit = cleanStaff.stream()
                .collect(Collectors.partitioningBy(e -> e.salary() >= 140_000));
        System.out.println("Partition >=140k count: true=" + seniorSplit.get(true).size()
                + ", false=" + seniorSplit.get(false).size());

        String csvNames = cleanStaff.stream()
                .map(Employee::name)
                .collect(Collectors.joining(", ", "[", "]"));
        System.out.println("Joined names: " + csvNames);

        System.out.println("\n=== 4. summarizingDouble(), collectingAndThen() & teeing() ===");
        DoubleSummaryStatistics stats = cleanStaff.stream()
                .collect(Collectors.summarizingDouble(Employee::salary));
        System.out.printf("Stats -> count=%d avg=$%.0f max=$%.0f%n",
                stats.getCount(), stats.getAverage(), stats.getMax());

        List<String> unmodifiableNames = cleanStaff.stream().collect(Collectors.collectingAndThen(
                Collectors.mapping(Employee::name, Collectors.toList()),
                Collections::unmodifiableList
        ));
        System.out.println("collectingAndThen unmodifiable: " + unmodifiableNames);

        // Single-pass min + max using Java 12 Collectors.teeing!
        String salaryRange = cleanStaff.stream().collect(Collectors.teeing(
                Collectors.minBy(Comparator.comparingDouble(Employee::salary)),
                Collectors.maxBy(Comparator.comparingDouble(Employee::salary)),
                (minOpt, maxOpt) -> formatNameSalary(minOpt) + " .. " + formatNameSalary(maxOpt)
        ));
        System.out.println("Single-pass teeing min..max: " + salaryRange);
    }

    private static String formatNameSalary(Optional<Employee> opt) {
        return opt.map(e -> e.name() + "($" + (int) e.salary() + ")").orElse("N/A");
    }
}
