package command.solution;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CommandSolutionMain {

    public static void main(String[] args) throws Exception {
        Document doc = new Document();
        Editor editor = new Editor();

        // Same sequence as problem/ — same results.
        editor.run(new AppendCommand(doc, "hello"));
        editor.run(new AppendCommand(doc, " world"));
        expect(doc, "hello world");
        editor.run(new DeleteCommand(doc, 6));
        expect(doc, "hello");
        editor.run(new UpperCaseCommand(doc));
        expect(doc, "HELLO");

        // Undo, every step checked.
        editor.undo();
        expect(doc, "hello");
        editor.undo();
        expect(doc, "hello world");
        editor.undo();
        expect(doc, "hello");
        editor.undo();
        expect(doc, "");
        assert !editor.undo() : "undo on empty history is a no-op";

        // Redo, then a new command wipes the redo stack.
        editor.redo();
        expect(doc, "hello");
        editor.redo();
        expect(doc, "hello world");
        editor.run(new AppendCommand(doc, "!"));
        expect(doc, "hello world!");
        assert !editor.canRedo() : "new command must clear redo";
        assert !editor.redo();

        // Macro: one undo reverts every step, in reverse.
        Command shout = new MacroCommand("shout", List.of(
                new AppendCommand(doc, "!!"),
                new UpperCaseCommand(doc)));
        editor.run(shout);
        expect(doc, "HELLO WORLD!!!");
        editor.undo();
        expect(doc, "hello world!");

        // Runnable is the JDK's command: a request object handed to a queue to run later.
        Document background = new Document();
        Command job = new AppendCommand(background, "queued");
        ExecutorService queue = Executors.newSingleThreadExecutor();
        try {
            Future<?> done = queue.submit(job::execute);
            done.get();
        } finally {
            queue.shutdown();
        }
        expect(background, "queued");

        System.out.println("solution: all asserts passed");
    }

    private static void expect(Document doc, String expected) {
        assert doc.text().equals(expected) : "expected '" + expected + "' but was '" + doc.text() + "'";
        System.out.println("'" + doc.text() + "'");
    }
}
