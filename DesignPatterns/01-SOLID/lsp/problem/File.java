package lsp.problem;

public abstract class File {
    String name;
    String content;

    protected File(String name, String content){
        this.name=name;
        this.content=content;
    }

    public String read() {
        return content;
    }

    public void write(String content) {
        this.content=content;
    }
}
