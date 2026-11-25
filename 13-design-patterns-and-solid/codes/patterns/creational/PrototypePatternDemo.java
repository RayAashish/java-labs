package patterns.creational;

import java.util.ArrayList;
import java.util.List;

/**
 * CREATIONAL PATTERN 5 — PROTOTYPE PATTERN
 *
 * WHAT IT IS:
 *   Creates new objects by cloning/copying an existing pre-configured prototype instance
 *   rather than running an expensive creation pipeline from scratch.
 *
 * WHY IT MATTERS:
 *   When initializing a template object requires heavy parsing/DB queries (e.g., an enterprise
 *   report template or game enemy archetype), cloning a prototype in memory is orders of magnitude faster.
 *   CRITICAL GOTCHA: Always perform a DEEP COPY of mutable nested collections/objects!
 *
 * IN THIS FILE:
 *   1. BAD CODE:  Shallow copy shares the mutable `installedPackages` list -> corrupts prototype!
 *   2. GOOD CODE: Deep-copy Prototype interface (`copy()`) creates independent isolated clones.
 */
public class PrototypePatternDemo {

    interface Prototype<T> {
        T deepCopy();
    }

    static class ServerTemplate implements Prototype<ServerTemplate> {
        private final String baseOsImage;
        private final List<String> installedPackages;

        ServerTemplate(String baseOsImage, List<String> installedPackages) {
            this.baseOsImage = baseOsImage;
            this.installedPackages = new ArrayList<>(installedPackages);
        }

        // ❌ BAD: Shallow copy — both copies point to the SAME ArrayList on the Heap!
        public ServerTemplate shallowCopyBug() {
            ServerTemplate copy = new ServerTemplate(this.baseOsImage, List.of());
            copy.installedPackages.clear();
            // Simulating naive `this.list = other.list` aliasing:
            return new AliasedServerTemplate(this.baseOsImage, this.installedPackages);
        }

        // ✅ GOOD: True deep copy isolating mutable state cleanly
        @Override
        public ServerTemplate deepCopy() {
            return new ServerTemplate(this.baseOsImage, new ArrayList<>(this.installedPackages));
        }

        public void addPackage(String pkg) {
            installedPackages.add(pkg);
        }

        @Override
        public String toString() {
            return "ServerTemplate{os='" + baseOsImage + "', packages=" + installedPackages + "}";
        }
    }

    static class AliasedServerTemplate extends ServerTemplate {
        private final List<String> sharedListRef;
        AliasedServerTemplate(String os, List<String> sharedListRef) {
            super(os, List.of());
            this.sharedListRef = sharedListRef;
        }
        @Override public void addPackage(String pkg) { sharedListRef.add(pkg); }
        @Override public String toString() { return "Aliased{packages=" + sharedListRef + "}"; }
    }

    public static void main(String[] args) {
        ServerTemplate goldenBase = new ServerTemplate("Ubuntu-24.04-LTS", List.of("openjdk-25", "curl"));

        System.out.println("=== BAD (Shallow Copy Mutates the Golden Prototype!) ===");
        ServerTemplate badClone = goldenBase.shallowCopyBug();
        badClone.addPackage("DEBUG-BACKDOOR");
        System.out.println("Golden base corrupted! -> " + goldenBase);

        System.out.println("\n=== GOOD (Deep-Copy Prototype Registry) ===");
        ServerTemplate cleanBase = new ServerTemplate("Ubuntu-24.04-LTS", List.of("openjdk-25", "curl"));
        ServerTemplate workerNode = cleanBase.deepCopy();
        workerNode.addPackage("prometheus-exporter");

        System.out.println("Clean Golden Base: " + cleanBase);
        System.out.println("Worker Node Clone: " + workerNode);
    }
}
