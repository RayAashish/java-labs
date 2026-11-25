package patterns.behavioral;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * BEHAVIORAL PATTERN 5 — COMMAND PATTERN
 *
 * WHAT IT IS:
 *   Encapsulates a request/action as a standalone object containing all parameters needed to
 *   execute — enabling Undo/Redo stacks, audit logs, delayed execution, and retry queues.
 *
 * WHY IT MATTERS:
 *   If callers directly mutate `editor.append("abc")`, there is no history of prior state to undo
 *   or queue for async retry. Wrapping actions as Command objects solves both cleanly.
 */
public class CommandPatternDemo {

    /** Receiver performing actual document mutations */
    static class TextDocument {
        private final StringBuilder buffer = new StringBuilder();

        void append(String text) { buffer.append(text); }
        void deleteLast(int count) { buffer.delete(Math.max(0, buffer.length() - count), buffer.length()); }
        String content() { return buffer.toString(); }
    }

    interface EditorCommand {
        void execute();
        void undo();
    }

    static class AppendTextCommand implements EditorCommand {
        private final TextDocument doc;
        private final String text;

        AppendTextCommand(TextDocument doc, String text) {
            this.doc = doc;
            this.text = text;
        }

        @Override public void execute() { doc.append(text); }
        @Override public void undo()    { doc.deleteLast(text.length()); }
    }

    /** Invoker maintaining an undo stack */
    static class CommandHistory {
        private final Deque<EditorCommand> undoStack = new ArrayDeque<>();

        void run(EditorCommand cmd) {
            cmd.execute();
            undoStack.push(cmd);
        }

        void undoLast() {
            if (!undoStack.isEmpty()) {
                undoStack.pop().undo();
            }
        }
    }

    public static void main(String[] args) {
        TextDocument doc = new TextDocument();
        CommandHistory history = new CommandHistory();

        history.run(new AppendTextCommand(doc, "Java "));
        history.run(new AppendTextCommand(doc, "Patterns "));
        history.run(new AppendTextCommand(doc, "Mistake!"));
        System.out.println("After 3 commands: '" + doc.content() + "'");

        history.undoLast();
        System.out.println("After 1st undo:   '" + doc.content() + "'");

        history.undoLast();
        System.out.println("After 2nd undo:   '" + doc.content() + "'");
    }
}
