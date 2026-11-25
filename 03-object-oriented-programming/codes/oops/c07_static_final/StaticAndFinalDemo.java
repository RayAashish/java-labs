package oops.c07_static_final;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates static vs instance state, static initialization order,
 * static method hiding vs overriding, and the three semantics of 'final'.
 */
public class StaticAndFinalDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. Static Class-Level State & Constant ===");
        System.out.println("Speed of light constant: " + UniverseConstants.SPEED_OF_LIGHT_MPS + " m/s");

        QuestSystemRegistry q1 = new QuestSystemRegistry("Dragon Keep");
        QuestSystemRegistry q2 = new QuestSystemRegistry("Shadow Citadel");
        System.out.println("Active quests shared across all instances: " + QuestSystemRegistry.getTotalActiveQuests());

        System.out.println("\n=== 2. Static Method Hiding vs Dynamic Overriding ===");
        DeviceFirmware ref = new SmartSensorFirmware();
        // Static method call resolves by COMPILE-TIME reference type (Method Hiding)
        ref.identifyProtocol();
        // Instance method call resolves by RUNTIME heap type (Dynamic Dispatch)
        ref.reportStatus();

        System.out.println("\n=== 3. Final Reference Mutability Trap ===");
        final List<String> immutableRefList = new ArrayList<>();
        immutableRefList.add("Telemetry-OK"); // ALLOWED: object state is still mutable!
        immutableRefList.add("Battery-98%");
        // immutableRefList = new ArrayList<>(); // COMPILE ERROR: cannot reassign a final reference
        System.out.println("Final reference still allows list mutation: " + immutableRefList);
    }

    static final class UniverseConstants {
        public static final double SPEED_OF_LIGHT_MPS = 299_792_458.0;

        static {
            System.out.println("[Static Block] UniverseConstants loaded into Metaspace once.");
        }

        private UniverseConstants() {}
    }

    static class QuestSystemRegistry {
        private static int totalActiveQuests = 0; // Shared in Metaspace across all instances
        private final String questName;

        QuestSystemRegistry(String questName) {
            this.questName = questName;
            totalActiveQuests++;
        }

        public static int getTotalActiveQuests() {
            return totalActiveQuests;
        }

        public String getQuestName() {
            return questName;
        }
    }

    static class DeviceFirmware {
        static void identifyProtocol() {
            System.out.println("Base DeviceFirmware protocol v1");
        }

        void reportStatus() {
            System.out.println("Base DeviceFirmware status");
        }

        // Subclasses are forbidden from overriding final methods
        public final void bootDevice() {
            System.out.println("Secure hardware boot sequence");
        }
    }

    static class SmartSensorFirmware extends DeviceFirmware {
        // Method Hiding (NOT overriding — static methods belong to the class, not the object)
        static void identifyProtocol() {
            System.out.println("SmartSensorFirmware protocol v2");
        }

        @Override
        void reportStatus() {
            System.out.println("SmartSensorFirmware online (overridden)");
        }
    }
}
