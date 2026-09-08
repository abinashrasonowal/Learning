package isp.problem;

public class ISPProblemMain {
    // the client asks for a Machine, so it believes every one of them can copy
    static void archive(Machine machine, String document) {
        machine.copy(document);
    }

    public static void main(String[] args) {
        Machine office = new MultiPurposeMachine("office-mfp", 5);
        archive(office, "contract.pdf");

        Machine desk = new SimplePrinter("desk-printer", 2);
        desk.print("invoice.pdf");
        desk.print("invoice.pdf");
        desk.print("invoice.pdf");        // out of paper — state is real

        archive(desk, "contract.pdf");    // compiles fine, dies here at runtime
    }
}
