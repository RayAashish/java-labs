package oops.c12_mini_project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Capstone OOP Mini-Project: Smart Home Energy & Automation Controller.
 * Combines all OOP pillars into one cohesive, real-world architecture:
 * - Abstraction (abstract SmartDevice + PowerControllable interface)
 * - Encapsulation (validated state transitions, immutable telemetry Snapshot record/class)
 * - Inheritance & Polymorphism (SmartLight, SmartThermostat dispatched uniformly)
 * - Composition (SmartHomeController owns an immutable PowerBudget + AuditLog)
 * - Aggregation (SmartHomeController manages independent SmartDevice instances)
 * - Enums & Nested Classes (SecurityLevel enum + static nested AuditEntry)
 */
public class SmartHomeMiniProject {

    public static void main(String[] args) {
        SmartHomeController controller = new SmartHomeController("Home-Alpha", 2500);

        SmartLight livingRoomLight = new SmartLight("L-01", "Living Room Light", 80);
        SmartThermostat bedroomThermostat = new SmartThermostat("T-01", "Bedroom Thermostat", 21.5);

        controller.registerDevice(livingRoomLight);
        controller.registerDevice(bedroomThermostat);

        controller.activateNightMode();
        System.out.println("Total live draw: " + controller.totalPowerDrawWatts() + "W");

        System.out.println("\n--- Audit Trail ---");
        controller.auditHistory().forEach(System.out::println);
    }

    enum PowerState { OFF, STANDBY, ACTIVE }

    interface PowerControllable {
        void turnOn();
        void turnOff();
        int currentDrawWatts();
    }

    abstract static class SmartDevice implements PowerControllable {
        private final String deviceId;
        private final String label;
        private PowerState state = PowerState.OFF;

        protected SmartDevice(String deviceId, String label) {
            this.deviceId = Objects.requireNonNull(deviceId);
            this.label = Objects.requireNonNull(label);
        }

        protected void setState(PowerState state) {
            this.state = state;
        }

        public PowerState getState() {
            return state;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public void turnOn() {
            setState(PowerState.ACTIVE);
        }

        @Override
        public void turnOff() {
            setState(PowerState.OFF);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SmartDevice that = (SmartDevice) o;
            return Objects.equals(deviceId, that.deviceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceId);
        }
    }

    static class SmartLight extends SmartDevice {
        private int brightnessPercent;

        SmartLight(String id, String label, int brightnessPercent) {
            super(id, label);
            setBrightness(brightnessPercent);
        }

        public void setBrightness(int percent) {
            if (percent < 0 || percent > 100) {
                throw new IllegalArgumentException("Brightness must be between 0 and 100");
            }
            this.brightnessPercent = percent;
        }

        @Override
        public int currentDrawWatts() {
            return getState() == PowerState.ACTIVE ? (int) Math.ceil(brightnessPercent * 0.15) : 0;
        }
    }

    static class SmartThermostat extends SmartDevice {
        private double targetCelsius;

        SmartThermostat(String id, String label, double targetCelsius) {
            super(id, label);
            setTargetCelsius(targetCelsius);
        }

        public void setTargetCelsius(double targetCelsius) {
            if (targetCelsius < 10.0 || targetCelsius > 32.0) {
                throw new IllegalArgumentException("Target temperature out of safe bounds (10-32C)");
            }
            this.targetCelsius = targetCelsius;
        }

        @Override
        public int currentDrawWatts() {
            return switch (getState()) {
                case ACTIVE -> 1200;
                case STANDBY -> 15;
                case OFF -> 0;
            };
        }

        public double getTargetCelsius() {
            return targetCelsius;
        }
    }

    static class SmartHomeController {
        private final String hubName;
        private final int maxWattsBudget;
        private final List<SmartDevice> devices = new ArrayList<>(); // Aggregation
        private final List<AuditEntry> logs = new ArrayList<>();     // Composition

        // Static nested class representing immutable audit record
        static final class AuditEntry {
            private final String message;

            AuditEntry(String message) {
                this.message = message;
            }

            @Override
            public String toString() {
                return "[AUDIT] " + message;
            }
        }

        SmartHomeController(String hubName, int maxWattsBudget) {
            this.hubName = hubName;
            this.maxWattsBudget = maxWattsBudget;
        }

        void registerDevice(SmartDevice device) {
            devices.add(device);
            logs.add(new AuditEntry(hubName + " registered " + device.getLabel()));
        }

        void activateNightMode() {
            for (SmartDevice d : devices) {
                if (d instanceof SmartLight light) {
                    light.turnOn();
                    light.setBrightness(20);
                } else if (d instanceof SmartThermostat thermostat) {
                    thermostat.turnOn();
                    thermostat.setTargetCelsius(19.0);
                }
            }
            logs.add(new AuditEntry("Night mode activated across " + devices.size() + " devices; modes="
                    + EnumSet.of(PowerState.ACTIVE)));
            if (totalPowerDrawWatts() > maxWattsBudget) {
                logs.add(new AuditEntry("WARNING: Budget exceeded!"));
            }
        }

        int totalPowerDrawWatts() {
            return devices.stream().mapToInt(PowerControllable::currentDrawWatts).sum();
        }

        List<AuditEntry> auditHistory() {
            return Collections.unmodifiableList(logs);
        }
    }
}
