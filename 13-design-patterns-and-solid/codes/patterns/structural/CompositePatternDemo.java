package patterns.structural;

import java.util.ArrayList;
import java.util.List;

/**
 * STRUCTURAL PATTERN 5 — COMPOSITE PATTERN
 *
 * WHAT IT IS:
 *   Composes objects into tree structures to represent part-whole hierarchies, allowing callers
 *   to treat individual Leaf objects and Composite containers UNIFORMLY.
 *
 * WHY IT MATTERS:
 *   Without Composite, summing the size of a folder containing files and nested subfolders
 *   requires messy `if (item instanceof File) ... else if (item instanceof Folder)` checks!
 *   With Composite, both implement `sizeBytes()`.
 */
public class CompositePatternDemo {

    interface FileSystemNode {
        String name();
        long sizeBytes();
        void printTree(String indent);
    }

    /** Leaf node */
    static class FileLeaf implements FileSystemNode {
        private final String name;
        private final long bytes;

        FileLeaf(String name, long bytes) {
            this.name = name;
            this.bytes = bytes;
        }

        @Override public String name() { return name; }
        @Override public long sizeBytes() { return bytes; }
        @Override public void printTree(String indent) {
            System.out.println(indent + "- " + name + " (" + bytes + " B)");
        }
    }

    /** Composite container node holding leaves OR nested composites */
    static class DirectoryComposite implements FileSystemNode {
        private final String name;
        private final List<FileSystemNode> children = new ArrayList<>();

        DirectoryComposite(String name) {
            this.name = name;
        }

        public DirectoryComposite add(FileSystemNode node) {
            children.add(node);
            return this;
        }

        @Override public String name() { return name; }

        @Override
        public long sizeBytes() {
            // Uniform recursive delegation — zero instanceof checks!
            return children.stream().mapToLong(FileSystemNode::sizeBytes).sum();
        }

        @Override
        public void printTree(String indent) {
            System.out.println(indent + "+ " + name + "/ (" + sizeBytes() + " B total)");
            for (FileSystemNode child : children) {
                child.printTree(indent + "  ");
            }
        }
    }

    public static void main(String[] args) {
        DirectoryComposite src = new DirectoryComposite("src")
                .add(new FileLeaf("Main.java", 1420))
                .add(new FileLeaf("Config.java", 880));

        DirectoryComposite project = new DirectoryComposite("java-labs")
                .add(new FileLeaf("README.md", 5200))
                .add(src);

        project.printTree("");
    }
}
