package strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demonstrates pre-compiled Pattern, named capturing groups,
 * find() vs matches(), and greedy vs reluctant quantifiers.
 */
public class RegexAndTextBlockDemo {

    // Pre-compile once as static final — avoids recompiling the automaton per request
    private static final Pattern LOG_LINE_PATTERN =
            Pattern.compile("\\[(?<level>INFO|WARN|ERROR)\\]\\s+user=(?<user>[a-zA-Z0-9_]+)\\s+latency=(?<ms>\\d+)ms");

    public static void main(String[] args) {
        System.out.println("=== 1. Named Capturing Groups in Log Parsing ===");
        String logs = """
                [INFO] user=aashish_ray latency=14ms
                [WARN] user=guest_99 latency=420ms
                [ERROR] user=billing_svc latency=1290ms
                """;

        Matcher matcher = LOG_LINE_PATTERN.matcher(logs);
        while (matcher.find()) {
            System.out.printf("Level=%-5s | User=%-12s | Latency=%sms%n",
                    matcher.group("level"),
                    matcher.group("user"),
                    matcher.group("ms"));
        }

        System.out.println("\n=== 2. Greedy vs Reluctant Quantifiers ===");
        String html = "<b>Java</b> and <b>JVM</b>";
        Matcher greedy = Pattern.compile("<b>.*</b>").matcher(html);
        Matcher reluctant = Pattern.compile("<b>.*?</b>").matcher(html);

        if (greedy.find()) {
            System.out.println("Greedy (.*) match:     " + greedy.group());
        }
        System.out.print("Reluctant (.*?) matches: ");
        while (reluctant.find()) {
            System.out.print(reluctant.group() + "  ");
        }
        System.out.println();
    }
}
