package oops.c10_nested_classes;

import java.util.List;

/**
 * Demonstrates all 4 Java nested class varieties:
 * 1. Static Nested Class (Builder / DTO without outer reference leak)
 * 2. Member Inner Class (Accesses enclosing Outer.this private fields)
 * 3. Local Class (Scoped inside a method; captures effectively final variables)
 * 4. Anonymous Class (One-off interface implementation vs Lambda)
 */
public class NestedAndInnerClassesDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. Static Nested Class (Builder Pattern) ===");
        QuestLog.Config config = new QuestLog.Config.Builder()
                .maxObjectives(5)
                .autoTrack(true)
                .build();
        System.out.println(config);

        System.out.println("\n=== 2. Member Inner Class (Outer.this access) ===");
        QuestLog questLog = new QuestLog("Lost Temple");
        QuestLog.ObjectiveTracker tracker = questLog.new ObjectiveTracker();
        tracker.markProgress(2, 5);

        System.out.println("\n=== 3. Local Class + Effectively Final Closure Capture ===");
        questLog.evaluateBonuses(100);

        System.out.println("\n=== 4. Anonymous Class Callback ===");
        Runnable onComplete = new Runnable() {
            @Override
            public void run() {
                System.out.println("Anonymous callback: Quest completed!");
            }
        };
        onComplete.run();
    }

    static class QuestLog {
        private final String questTitle;
        private int completedCount = 0;

        QuestLog(String questTitle) {
            this.questTitle = questTitle;
        }

        // 1. Static Nested Class: No implicit reference to QuestLog instance (prevents memory leaks!)
        static class Config {
            private final int maxObjectives;
            private final boolean autoTrack;

            private Config(int maxObjectives, boolean autoTrack) {
                this.maxObjectives = maxObjectives;
                this.autoTrack = autoTrack;
            }

            @Override
            public String toString() {
                return "Config{maxObjectives=" + maxObjectives + ", autoTrack=" + autoTrack + "}";
            }

            static class Builder {
                private int maxObjectives = 3;
                private boolean autoTrack = false;

                Builder maxObjectives(int max) {
                    this.maxObjectives = max;
                    return this;
                }

                Builder autoTrack(boolean autoTrack) {
                    this.autoTrack = autoTrack;
                    return this;
                }

                Config build() {
                    return new Config(maxObjectives, autoTrack);
                }
            }
        }

        // 2. Member Inner Class: Holds implicit pointer QuestLog.this to outer instance
        class ObjectiveTracker {
            private final String questTitle = "Local Shadowed Title";

            void markProgress(int done, int total) {
                QuestLog.this.completedCount = done; // Explicitly disambiguates shadowed field
                System.out.println("Quest [" + QuestLog.this.questTitle + "] (tracker label: "
                        + this.questTitle + ") -> " + completedCount + "/" + total);
            }
        }

        // 3. Local Class inside method
        void evaluateBonuses(int baseXp) {
            int multiplier = 2; // Effectively final: never reassigned after initialization
            class BonusCalculator {
                int computeTotal() {
                    // Captures local variable 'multiplier' + enclosing field 'questTitle'
                    return baseXp * multiplier;
                }
            }
            BonusCalculator calc = new BonusCalculator();
            System.out.println("Bonus XP for " + questTitle + ": " + calc.computeTotal());
            // Reassigning 'multiplier = 3;' anywhere in this method would cause a compile error!
            List.of(1).forEach(step -> System.out.println("Step " + step + " verified with multiplier " + multiplier));
        }
    }
}
