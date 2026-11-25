package oops.c09_composition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Demonstrates Object Relationships:
 * 1. Composition (Strong HAS-A): Spaceship creates & owns its Thruster & LifeSupport.
 * 2. Aggregation (Weak HAS-A): SmartHomeHub holds independent SmartDevice references.
 * 3. Favoring Composition over Inheritance for runtime strategy swapping.
 */
public class CompositionVsAggregationDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. Strong Composition (Spaceship owns Thruster & LifeSupport) ===");
        Spaceship explorer = new Spaceship("USS Horizon", 12000);
        explorer.launch();

        System.out.println("\n=== 2. Weak Aggregation (SmartHomeHub groups independent devices) ===");
        SmartThermostat hallThermostat = new SmartThermostat("Hallway");
        SmartHomeHub hub = new SmartHomeHub();
        hub.registerDevice(hallThermostat);
        hub.activateAll();
        // Even if the hub is garbage-collected, hallThermostat still exists independently!
        hub = null;
        System.out.println("Thermostat still operates independently: " + hallThermostat.deviceLabel());

        System.out.println("\n=== 3. Composition Enables Runtime Behavior Swapping ===");
        Spaceship adaptable = new Spaceship("Starlight", 8000);
        adaptable.setNavigationEngine(() -> System.out.println("Navigating via Quantum Star Map."));
        adaptable.launch();
        adaptable.setNavigationEngine(() -> System.out.println("Swapped to Emergency Asteroid Avoidance Radar."));
        adaptable.launch();
    }

    @FunctionalInterface
    interface NavigationEngine {
        void plotCourse();
    }

    // Composition: Spaceship owns Thruster & LifeSupport lifecycles exclusively
    static class Spaceship {
        private final String name;
        private final Thruster thruster;
        private final LifeSupport lifeSupport;
        private NavigationEngine navigationEngine;

        Spaceship(String name, int thrustNewtons) {
            this.name = name;
            this.thruster = new Thruster(thrustNewtons);
            this.lifeSupport = new LifeSupport();
            this.navigationEngine = () -> System.out.println("Navigating via Standard Autopilot.");
        }

        void setNavigationEngine(NavigationEngine navigationEngine) {
            this.navigationEngine = navigationEngine;
        }

        void launch() {
            System.out.println("Launching " + name + ":");
            lifeSupport.stabilizeOxygen();
            navigationEngine.plotCourse();
            thruster.ignite();
        }
    }

    static class Thruster {
        private final int thrustNewtons;

        Thruster(int thrustNewtons) {
            this.thrustNewtons = thrustNewtons;
        }

        void ignite() {
            System.out.println("Thruster firing at " + thrustNewtons + " N.");
        }
    }

    static class LifeSupport {
        void stabilizeOxygen() {
            System.out.println("LifeSupport: Cabin oxygen stabilized at 21%.");
        }
    }

    static class SmartThermostat {
        private final String room;

        SmartThermostat(String room) {
            this.room = room;
        }

        void powerOn() {
            System.out.println(room + " thermostat set to Eco 22C.");
        }

        String deviceLabel() {
            return "Thermostat(" + room + ")";
        }
    }

    // Aggregation: Hub coordinates externally created devices
    static class SmartHomeHub {
        private final List<SmartThermostat> devices = new ArrayList<>();

        void registerDevice(SmartThermostat device) {
            devices.add(device);
        }

        List<SmartThermostat> getDevices() {
            return Collections.unmodifiableList(devices);
        }

        void activateAll() {
            devices.forEach(SmartThermostat::powerOn);
        }
    }
}
