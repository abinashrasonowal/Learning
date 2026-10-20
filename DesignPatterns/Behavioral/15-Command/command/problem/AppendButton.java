package command.problem;

public class AppendButton {
    private final Document doc;
    private final String text;

    public AppendButton(Document doc, String text) {
        this.doc = doc;
        this.text = text;
    }

    public void click() {
        doc.append(text);
    }
}
