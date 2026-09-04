package lsp.solution;

public abstract class File {
    private final String name;
    protected String content;

    protected File(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public String read() {
        return content;
    }
}
