//6. Search with Condition
//
//        Problem
//
//List<Employee> findEmployeesOlderThan(int age)
//
//Concepts learned:
//        •	Filtering queries
//•	Dynamic parameters

import java.util.List;

public class FindAllEmployeeOlderThan {
    public static void main(String[] args) {
        List<Employee> employeeList = findEmployeesOlderThan(50);
        employeeList.forEach(System.out :: println);
    }

    static List<Employee> findEmployeesOlderThan(int age){
        List<Employee> employeeList = GetAllEmployees.getAllEmployees();
        List<Employee> olderEmployees = employeeList.stream()
                .filter(e -> e.age >= age)
                .toList();
        return olderEmployees;
    }
}
