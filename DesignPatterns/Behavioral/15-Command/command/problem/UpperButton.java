package command.problem;

public class UpperButton {
    private final Document doc;

    public UpperButton(Document doc) {
        this.doc = doc;
    }

    public void click() {
        doc.toUpperCase();   // the original casing is lost the moment this runs
    }
}
