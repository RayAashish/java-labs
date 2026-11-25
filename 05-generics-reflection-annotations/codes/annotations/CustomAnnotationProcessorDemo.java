package annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * CUSTOM ANNOTATIONS & RUNTIME VALIDATION PROCESSOR
 *
 * WHAT IT IS:
 *   Declarative metadata (`@interface`) inspected via Reflection (similar to Jakarta Bean Validation
 *   `@NotBlank`, `@Range`, or Spring `@Scheduled`).
 *
 * WHY IT MATTERS:
 *   Separates validation/wiring rules from repetitive boilerplate `if` checks across DTOs.
 */
public class CustomAnnotationProcessorDemo {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface NotBlankField {
        String message() default "Field must not be blank";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface IntRange {
        int min();
        int max();
    }

    static class CreateUserRequest {
        @NotBlankField(message = "Username is required")
        private final String username;

        @IntRange(min = 18, max = 120)
        private final int age;

        CreateUserRequest(String username, int age) {
            this.username = username;
            this.age = age;
        }
    }

    public static List<String> validate(Object target) throws IllegalAccessException {
        List<String> errors = new ArrayList<>();
        for (Field field : target.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object val = field.get(target);

            if (field.isAnnotationPresent(NotBlankField.class)) {
                NotBlankField ann = field.getAnnotation(NotBlankField.class);
                if (val == null || val.toString().isBlank()) {
                    errors.add(field.getName() + ": " + ann.message());
                }
            }
            if (field.isAnnotationPresent(IntRange.class) && val instanceof Integer num) {
                IntRange range = field.getAnnotation(IntRange.class);
                if (num < range.min() || num > range.max()) {
                    errors.add(field.getName() + ": " + num + " outside [" + range.min() + ".." + range.max() + "]");
                }
            }
        }
        return errors;
    }

    public static void main(String[] args) throws IllegalAccessException {
        CreateUserRequest invalid = new CreateUserRequest("   ", 15);
        CreateUserRequest valid = new CreateUserRequest("aashish_ray", 25);

        System.out.println("Invalid DTO errors: " + validate(invalid));
        System.out.println("Valid DTO errors:   " + validate(valid));
    }
}
