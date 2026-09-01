// Interface Segregation Principle — no client should depend on methods it doesn't use.
// Run: java ISP.java

public class ISP {
    public static void main(String[] args) {
        Document doc = new Document("quarterly-report.pdf");

        System.out.println("--- BAD: fat interface forces useless stubs ---");
        Machine printer = new BadSimplePrinter();
        printer.print(doc);
        try {
            printer.scan(doc); // SimplePrinter never had a scanner. Compiles anyway.
        } catch (UnsupportedOperationException e) {
            System.out.println("Blew up: " + e.getMessage());
        }

        System.out.println("\n--- GOOD: role interfaces, implement only what you are ---");
        SimplePrinter simple = new SimplePrinter();
        MultiPurposeMachine mfp = new MultiPurposeMachine();

        printAnything(simple, doc);
        printAnything(mfp, doc);
        mfp.scan(doc);
        mfp.copy(doc);
        // simple.scan(doc) does not compile — the mistake is now impossible.
    }

    static void printAnything(Printer p, Document doc) { p.print(doc); }
}

class Document {
    private final String name;
    Document(String name) { this.name = name; }
    String getName() { return name; }
}

// ---------- BAD ----------
interface Machine {
    void print(Document doc);
    void scan(Document doc);
    void copy(Document doc);
}

class BadSimplePrinter implements Machine {
    @Override public void print(Document doc) { System.out.println("Printing " + doc.getName()); }
    @Override public void scan(Document doc)  { throw new UnsupportedOperationException("Scan not supported"); }
    @Override public void copy(Document doc)  { throw new UnsupportedOperationException("Copy not supported"); }
}

// ---------- GOOD ----------
interface Printer { void print(Document doc); }
interface Scanner { void scan(Document doc); }
interface Copier  { void copy(Document doc); }

class SimplePrinter implements Printer {
    @Override public void print(Document doc) { System.out.println("Printing " + doc.getName()); }
}

class MultiPurposeMachine implements Printer, Scanner, Copier {
    @Override public void print(Document doc) { System.out.println("Printing " + doc.getName()); }
    @Override public void scan(Document doc)  { System.out.println("Scanning " + doc.getName()); }
    @Override public void copy(Document doc)  { System.out.println("Copying " + doc.getName()); }
}
