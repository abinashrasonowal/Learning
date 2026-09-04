package lsp.solution;

public class ReadWriteFile extends File {

    public ReadWriteFile(String name, String content){
        super(name, content);
    }

    public void write(String content){
        this.content = content;
    }
}
