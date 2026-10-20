package command.solution;

// The receiver: does the real work, knows nothing about commands or undo.
public class Document {
    private final StringBuilder text = new StringBuilder();

    public void append(String s) {
        text.append(s);
    }

    public void delete(int count) {
        int n = Math.min(count, text.length());
        text.setLength(text.length() - n);
    }

    public void toUpperCase() {
        setText(text.toString().toUpperCase());
    }

    public void setText(String s) {
        text.setLength(0);
        text.append(s);
    }

    public String text() {
        return text.toString();
    }
}
