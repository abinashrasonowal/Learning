package command.solution;

public class AppendCommand implements Command {
    private final Document doc;
    private final String text;

    public AppendCommand(Document doc, String text) {
        this.doc = doc;
        this.text = text;
    }

    @Override
    public void execute() {
        doc.append(text);
    }

    @Override
    public void undo() {
        doc.delete(text.length());
    }
}
