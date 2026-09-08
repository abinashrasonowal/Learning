package isp.solution;

public class ISPSolutionMain {

    // each client asks for the narrowest role it actually calls —
    // splitting the interface but keeping a fat parameter type fixes nothing
    static void printReport(Printer printer, String document) {
        printer.print(document);
    }

    static void archive(Copier copier, String document) {
        copier.copy(document);
    }

    // needs two roles, still refuses to name a concrete class
    static <T extends Printer & Scanner> void scanThenPrint(T machine, String document) {
        machine.scan(document);
        machine.print(document);
    }

    public static void main(String[] args) {
        MultiPurposeMachine office = new MultiPurposeMachine("office-mfp", 5);
        archive(office, "contract.pdf");
        scanThenPrint(office, "id-card.jpg");

        SimplePrinter desk = new SimplePrinter("desk-printer", 2);
        printReport(desk, "invoice.pdf");
        printReport(desk, "invoice.pdf");
        printReport(desk, "invoice.pdf");   // out of paper — state is real

        // archive(desk, "contract.pdf");   // Required type:
                                            // Copier
                                            // Provided:
                                            // SimplePrinter

        // scanThenPrint(desk, "invoice.pdf");  // inference fails: SimplePrinter is not a Scanner
    }
}
