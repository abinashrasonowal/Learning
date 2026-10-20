package command.solution;

public class UpperCaseCommand implements Command {
    private final Document doc;
    private String before = "";   // not naturally reversible, so remember the old text

    public UpperCaseCommand(Document doc) {
        this.doc = doc;
    }

    @Override
    public void execute() {
        before = doc.text();
        doc.toUpperCase();
    }

    @Override
    public void undo() {
        doc.setText(before);
    }
}
