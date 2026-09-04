package lsp.solution;


public class LSPSolutionMain {
    static void backup(ReadWriteFile file) {
        file.write(file.read() + " [backup]");
        System.out.println("backed up " + file.getName() + ": " + file.read());
    }

    public static void main(String[] args) {
        backup(new ReadWriteFile("a.txt", "hello"));
        ReadOnlyFile readOnlyFile = new ReadOnlyFile("b.txt","read only ");
        System.out.println(readOnlyFile.read());
        // backup(readOnlyFile);    // Required type:
                                    // ReadWriteFile
                                    // Provided:
                                    // ReadOnlyFile
    }
}
