package lsp.problem;

public class LSPProblemMain {

    // Written against File. Knows nothing about which subtype it gets.
    static void backup(File file) {
        file.write(file.read() + " [backup]");
        System.out.println("backed up: " + file.read());
    }

    public static void main(String[] args) {
        backup(new ReadWriteFile("a.txt", "hello"));
        backup(new ReadOnlyFile("b.txt", "hello"));
    }
}
