package command.solution;

import java.util.ArrayDeque;
import java.util.Deque;

// The invoker: knows only Command. Never touches Document, never checks command types.
public class Editor {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public void run(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();   // a new action forks history — the old future is unreachable
    }

    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        Command command = redoStack.pop();
        command.execute();   // re-captures its undo state against the current text
        undoStack.push(command);
        return true;
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
}
