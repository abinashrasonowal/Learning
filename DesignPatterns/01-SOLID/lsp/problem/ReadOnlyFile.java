package lsp.problem;

public class ReadOnlyFile extends File{

    ReadOnlyFile(String name, String content){
        super(name, content);
    }

    @Override
    public void write(String content){
        throw new UnsupportedOperationException("this is read only content");
    }
}
