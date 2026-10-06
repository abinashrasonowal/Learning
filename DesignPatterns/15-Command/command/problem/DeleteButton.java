package command.problem;

public class DeleteButton {
    private final Document doc;
    private final int count;

    public DeleteButton(Document doc, int count) {
        this.doc = doc;
        this.count = count;
    }

    public void click() {
        doc.delete(count);   // what was deleted? gone — nothing remembers it
    }
}
