package command.solution;

public class DeleteCommand implements Command {
    private final Document doc;
    private final int count;
    private String deleted = "";   // captured in execute() — it doesn't exist before then

    public DeleteCommand(Document doc, int count) {
        this.doc = doc;
        this.count = count;
    }

    @Override
    public void execute() {
        String current = doc.text();
        deleted = current.substring(current.length() - Math.min(count, current.length()));
        doc.delete(count);
    }

    @Override
    public void undo() {
        doc.append(deleted);
    }
}
