package command.problem;

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
        String upper = text.toString().toUpperCase();
        text.setLength(0);
        text.append(upper);
    }

    public String text() {
        return text.toString();
    }
}
